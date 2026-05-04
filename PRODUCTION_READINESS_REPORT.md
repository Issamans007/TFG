# TFG APK Production Readiness Report

Date: 2026-05-04

Target release channel: direct APK distribution, not Google Play Store publishing.

## Verdict

The project is not ready for production APK release yet.

The app has a solid modular Android/Kotlin structure and several good production-oriented choices: Hilt DI, Room, Retrofit/OkHttp, Firebase Crashlytics/Analytics integration, encrypted API credential storage, HTTPS-only network config, release minification/resource shrinking, Binance request signing, WebSocket support, and risk/trading engine separation.

However, the app still has APK production blockers in release configuration, trading-state correctness, runtime validation, and tests. Several earlier source-level blockers have been fixed since the first audit, including blank-order-ID normalization at the repository boundary, destructive Room migration fallback removal, offline cancel JSON parsing, widget refresh exposure, WebSocket reconnect variable visibility, and data extraction rules.

The remaining highest-risk issue is trading-state correctness: TP/SL/trailing/close paths still update local order status optimistically after placing close orders, without proving the close order filled. For an app that can handle real funds, that must be resolved or very tightly tested before distributing the APK to users.

## What Was Reviewed

- Root Gradle configuration, version catalog, app Gradle config, module list, manifest, network security, ProGuard, Firebase config, Gradle properties.
- 120 Kotlin source files outside generated build directories.
- App startup, Hilt repository bindings, navigation entry point, widget, worker, foreground trading service.
- Domain models/use cases, Room entities/DAOs/database migrations, local mappers.
- Binance trading repository, futures/spot order placement, WebSocket manager, offline queue, risk engine, trade executor.
- Feature-level ViewModels and screens at a source-review level.
- VS Code Problems scan.

Not executed: Gradle build/test tasks. The proposed `gradlew.bat testDebugUnitTest` run was skipped, so this report does not claim build or test pass status.

## Validated Fixes Since Initial Audit

1. Blank order IDs are mitigated at the repository boundary.
   - `TradingRepositoryImpl.placeOrder()` now generates a valid `"tfg" + UUID` client ID whenever `order.id.isBlank()` before spot/futures API calls or Room inserts.
   - `TradeViewModel` and `TradeExecutor` still pass blank IDs in some paths, but the repository boundary now normalizes them before exchange/database use.
   - Remaining validation needed: add tests proving manual orders, TP, SL, trailing stop, and close-all orders all arrive at Binance/Room with nonblank unique IDs.

2. Destructive Room migration fallback has been removed.
   - `DatabaseModule.kt` no longer calls `.fallbackToDestructiveMigration()`.
   - Remaining validation needed: run migration tests from every supported schema version to the current DB version.

3. Offline queue cancel parsing now uses JSON.
   - `TradeExecutor` now deserializes queued `orderJson` with Gson into `Order`, then calls `cancelOrder(order.id, order.symbol)`.
   - Remaining validation needed: add queue drain tests for cancel retries, missing fields, malformed JSON, and dead-letter behavior.

4. Manual market orders now use ticker fallback pricing.
   - `TradeViewModel` now falls back to `state.ticker?.price` for market orders when the price field is blank.
   - Remaining caveat: if ticker data is not available, price still falls back to `0.0`, so the risk engine can still block the order. That is safer than placing an unpriced order, but should be surfaced clearly in UI.

5. Widget custom refresh exposure has been removed.
   - The custom `com.tfg.widget.REFRESH` action was removed from the manifest intent-filter.
   - `TradingWidgetProvider` no longer contains the spoofable `callerPackage` refresh handler.
   - `TradingForegroundService` now refreshes widgets through `AppWidgetManager.ACTION_APPWIDGET_UPDATE` with explicit widget IDs and package scoping.

6. WebSocket reconnect variables now have visibility protection.
   - `lastTickerSymbols`, `lastListenKey`, `lastKlineSymbol`, and `lastKlineInterval` are now annotated `@Volatile`.
   - Remaining caveat: `@Volatile` improves visibility, but it does not make multi-field reconnect state atomic.

7. Android 12+ data extraction rules are now explicit.
   - `data_extraction_rules.xml` now defines cloud-backup and device-transfer exclusions for Room DB files, secure API-key prefs, widget state, executor state, and cache.

## Critical Blockers Remaining

1. Firebase config is placeholder.
   - `app/google-services.json` contains `tradeforgood-placeholder` and `PLACEHOLDER_API_KEY_REPLACE_WITH_REAL_ONE`.
   - Production impact: Firebase Analytics/Crashlytics and Google services are not actually configured for the app.
   - Required fix: replace with the real Firebase Console config for `com.tfg.tradeforgood`.

2. APK release signing configuration is missing.
   - `app/build.gradle.kts` configures release minify/shrink but has no `signingConfigs` or release `signingConfig`.
   - Production impact: the APK is not ready for safe direct distribution. Even outside the Play Store, users need a consistently signed release APK for install, update, and authenticity checks.
   - Required fix: add a secure APK signing configuration using environment variables or Gradle properties, not hardcoded secrets.

3. Automated tests are effectively absent.
   - Only default example tests exist.
   - The instrumented example expects package `com.example.tfg`, while the real app ID is `com.tfg.tradeforgood`, so it is stale.
   - Production impact: critical trading/risk/database behavior has no regression protection, including the fixes listed above.
   - Required fix: add focused unit/instrumentation tests before release.

## High Severity Issues

1. TP/SL/trailing/close status is updated optimistically.
   - Several paths place a market close order and then immediately mark the original order filled/cancelled/partially filled without confirming the close order filled.
   - Production impact: UI and risk accounting can say a position is closed while the exchange still has exposure.
   - Required fix: confirm the close fill via WebSocket/REST before status changes, and reconcile failures visibly.

2. Singleton engine scopes are manually owned.
   - `EngineManager` still owns a singleton `CoroutineScope` with no cancel path.
   - `StrategyRunner` owns its own scope, but it does cancel `runnerJob` and the scope in `stop()`.
   - `TradingForegroundService` uses a service scope tied to service shutdown.
   - Production impact: duplicate collectors, stale WebSocket reconnects, orphan jobs, or inconsistent service state after restarts.
   - Required fix: inject an application scope where appropriate and keep service-owned jobs strictly tied to service lifecycle.

3. Screenshot prevention is disabled.
   - `ScreenshotPrevention.apply()` is a no-op and explicitly says screenshots/screen recording are enabled.
   - Production impact: API keys, balances, PnL, and trading screens can be captured.
   - Required fix: apply `FLAG_SECURE` on sensitive auth/trading/settings screens, or document that screenshots are intentionally allowed.

## Medium Severity Issues

1. Offline queue retry is not transactionally claimed.
   - `getRetryable()` returns all retryable rows, and `markFailed()` increments count separately.
   - Production impact: if more than one drainer runs, the same queue item can be processed/retried concurrently.
   - Recommended fix: add a processing state or transactional claim step.

2. Unused generated starter app code remains.
   - `app/src/main/java/com/example/tfg/MainActivity.kt` and related theme files are still present but not the real launcher activity.
   - Recommended fix: delete stale starter code to reduce confusion and accidental test/package drift.

3. Many exceptions are swallowed silently.
   - This appears in dashboard, trade, portfolio, script, service, and repository paths.
   - Production impact: users may see stale data or failed background work with no actionable error.
   - Recommended fix: log or surface failures in user-impacting flows.

## Positive Findings

- Modern Android stack: AGP 8.13.2, Kotlin 2.0.21, compile/target SDK 35, Java 17.
- Modular architecture with app, core, domain, data-local, data-remote, security, trading-engine, and feature modules.
- Release build enables minification and resource shrinking.
- Network security blocks cleartext traffic.
- Binance API key/secret storage uses Android Keystore with AES/GCM.
- OkHttp logging is disabled in release and sensitive headers are redacted.
- ProGuard includes keep rules for Hilt, Room/Gson DTOs/models, Retrofit, OkHttp, coroutines, security, and JS interfaces.
- Binance request signing and time resync handling are present.
- Blank order IDs are now normalized in `TradingRepositoryImpl.placeOrder()` before exchange/database use.
- Room destructive migration fallback has been removed.
- Offline queue cancel handling now parses queued JSON instead of splitting strings.
- Widget refresh exposure was reduced by removing the custom exported refresh action.
- Android 12+ data extraction rules now explicitly exclude sensitive/ephemeral data.
- Risk engine includes kill switch, stale portfolio fail-closed behavior, daily loss, max open trades, drawdown, weekend/time-window, volatility, and balance checks.
- Foreground service uses wake lock handling, user-data stream startup, startup portfolio seeding, reconciliation attempt, monitoring loop, offline queue drain, and heartbeat.

## Required Before Production

1. Replace Firebase placeholder config.
2. Add secure release APK signing configuration.
3. Fix optimistic TP/SL/trailing/close status updates by confirming close fills before mutating local order status.
4. Add regression tests for the validated fixes: blank ID normalization, migration safety, offline queue cancel parsing, market-order ticker fallback, widget refresh, and data extraction rules.
5. Add trading/risk/data tests for order placement, close all, TP/SL/trailing execution, risk blocking, migrations, and queue retries.
6. Run and pass at minimum: debug unit tests, Android lint, debug assemble, release APK assemble, and an instrumented smoke test.
7. Perform paper-trading and small-value live-exchange validation with detailed audit/reconciliation checks before enabling real user funds.

## Suggested Test Plan

Unit tests:
- Blank manual order IDs are normalized to nonblank unique IDs before exchange/database use.
- TP, SL, trailing stop, and close-all close orders are normalized to fresh IDs before exchange/database use.
- TP, SL, trailing stop, and close-all status changes occur only after close-fill confirmation or explicit reconciliation failure handling.
- RiskEngine blocks stale portfolio and allows valid fresh portfolio orders.
- Market order position sizing uses ticker fallback and surfaces a clear error when ticker/reference price is unavailable.
- Offline queue cancel item parses from JSON and retries correctly.
- Room migrations from every supported version to current version preserve critical tables.

Integration/instrumented tests:
- App launches with Hilt and real package ID.
- Auth/settings save API credentials to Keystore path.
- Trade screen places a paper order and shows it in open/history state.
- Foreground service start/stop cancels jobs and releases wake lock.
- Widget refresh works through explicit app-widget update broadcasts and has no custom exported refresh action.

Manual APK release checks:
- Real Firebase Crashlytics receives a test non-fatal event.
- Release APK is signed with the production keystore.
- Release APK installs cleanly on a fresh device and updates correctly over the previous APK with the same package name/signing key.
- API logging is disabled in release.
- Certificate pins are current and have a rotation plan.
- Backup/data extraction rules do not expose credentials, order history, or PII.

## Final Assessment

APK production readiness: approximately 70-75%.

The architecture is promising, and several of the original hard blockers have now been fixed. The app still should not be distributed as a production APK until Firebase config, APK signing, automated tests, and close-fill/status confirmation are handled. Because this app can touch real trading and money, the next gate should be a full build/lint/test/release APK verification cycle plus paper-trading and small-value live validation before broader user distribution.