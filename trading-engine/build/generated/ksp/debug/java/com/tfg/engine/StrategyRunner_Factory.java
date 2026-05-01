package com.tfg.engine;

import android.content.Context;
import com.tfg.data.local.dao.CandleDao;
import com.tfg.data.local.dao.SignalMarkerDao;
import com.tfg.data.remote.websocket.WebSocketManager;
import com.tfg.domain.model.ScriptExecutor;
import com.tfg.domain.repository.AuditRepository;
import com.tfg.domain.repository.FeeRepository;
import com.tfg.domain.repository.MarketRepository;
import com.tfg.domain.repository.PortfolioRepository;
import com.tfg.domain.repository.ScriptRepository;
import com.tfg.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class StrategyRunner_Factory implements Factory<StrategyRunner> {
  private final Provider<Context> contextProvider;

  private final Provider<ScriptRepository> scriptRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<CandleDao> candleDaoProvider;

  private final Provider<SignalMarkerDao> signalMarkerDaoProvider;

  private final Provider<TradeExecutor> tradeExecutorProvider;

  private final Provider<RiskEngine> riskEngineProvider;

  private final Provider<AuditRepository> auditRepositoryProvider;

  private final Provider<PortfolioRepository> portfolioRepositoryProvider;

  private final Provider<MarketRepository> marketRepositoryProvider;

  private final Provider<WebSocketManager> webSocketManagerProvider;

  private final Provider<ScriptExecutor> scriptExecutorProvider;

  private final Provider<FeeRepository> feeRepositoryProvider;

  public StrategyRunner_Factory(Provider<Context> contextProvider,
      Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CandleDao> candleDaoProvider, Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<TradeExecutor> tradeExecutorProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<AuditRepository> auditRepositoryProvider,
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<ScriptExecutor> scriptExecutorProvider,
      Provider<FeeRepository> feeRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.scriptRepositoryProvider = scriptRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.candleDaoProvider = candleDaoProvider;
    this.signalMarkerDaoProvider = signalMarkerDaoProvider;
    this.tradeExecutorProvider = tradeExecutorProvider;
    this.riskEngineProvider = riskEngineProvider;
    this.auditRepositoryProvider = auditRepositoryProvider;
    this.portfolioRepositoryProvider = portfolioRepositoryProvider;
    this.marketRepositoryProvider = marketRepositoryProvider;
    this.webSocketManagerProvider = webSocketManagerProvider;
    this.scriptExecutorProvider = scriptExecutorProvider;
    this.feeRepositoryProvider = feeRepositoryProvider;
  }

  @Override
  public StrategyRunner get() {
    return newInstance(contextProvider.get(), scriptRepositoryProvider.get(), settingsRepositoryProvider.get(), candleDaoProvider.get(), signalMarkerDaoProvider.get(), tradeExecutorProvider.get(), riskEngineProvider.get(), auditRepositoryProvider.get(), portfolioRepositoryProvider.get(), marketRepositoryProvider.get(), webSocketManagerProvider.get(), scriptExecutorProvider.get(), feeRepositoryProvider.get());
  }

  public static StrategyRunner_Factory create(Provider<Context> contextProvider,
      Provider<ScriptRepository> scriptRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CandleDao> candleDaoProvider, Provider<SignalMarkerDao> signalMarkerDaoProvider,
      Provider<TradeExecutor> tradeExecutorProvider, Provider<RiskEngine> riskEngineProvider,
      Provider<AuditRepository> auditRepositoryProvider,
      Provider<PortfolioRepository> portfolioRepositoryProvider,
      Provider<MarketRepository> marketRepositoryProvider,
      Provider<WebSocketManager> webSocketManagerProvider,
      Provider<ScriptExecutor> scriptExecutorProvider,
      Provider<FeeRepository> feeRepositoryProvider) {
    return new StrategyRunner_Factory(contextProvider, scriptRepositoryProvider, settingsRepositoryProvider, candleDaoProvider, signalMarkerDaoProvider, tradeExecutorProvider, riskEngineProvider, auditRepositoryProvider, portfolioRepositoryProvider, marketRepositoryProvider, webSocketManagerProvider, scriptExecutorProvider, feeRepositoryProvider);
  }

  public static StrategyRunner newInstance(Context context, ScriptRepository scriptRepository,
      SettingsRepository settingsRepository, CandleDao candleDao, SignalMarkerDao signalMarkerDao,
      TradeExecutor tradeExecutor, RiskEngine riskEngine, AuditRepository auditRepository,
      PortfolioRepository portfolioRepository, MarketRepository marketRepository,
      WebSocketManager webSocketManager, ScriptExecutor scriptExecutor,
      FeeRepository feeRepository) {
    return new StrategyRunner(context, scriptRepository, settingsRepository, candleDao, signalMarkerDao, tradeExecutor, riskEngine, auditRepository, portfolioRepository, marketRepository, webSocketManager, scriptExecutor, feeRepository);
  }
}
