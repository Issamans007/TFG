package com.tfg.data.local.mapper

import com.tfg.data.local.entity.*
import com.tfg.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EntityMapper {
    private val gson = Gson()
    private val stringMapType = object : TypeToken<Map<String, String>>() {}.type

    fun OrderEntity.toDomain(): Order = Order(
        id = id, signalId = signalId, symbol = symbol,
        side = OrderSide.valueOf(side), type = OrderType.valueOf(type),
        status = OrderStatus.valueOf(status), executionMode = ExecutionMode.valueOf(executionMode),
        quantity = quantity, price = price, stopPrice = stopPrice,
        takeProfits = gson.fromJson(takeProfitsJson, object : TypeToken<List<TakeProfit>>() {}.type) ?: emptyList(),
        stopLosses = gson.fromJson(stopLossesJson, object : TypeToken<List<StopLoss>>() {}.type) ?: emptyList(),
        trailingStopPercent = trailingStopPercent,
        trailingStopActivationPrice = trailingStopActivationPrice,
        ocoLinkedOrderId = ocoLinkedOrderId, bracketParentId = bracketParentId,
        timeInForce = TimeInForce.valueOf(timeInForce), scheduledAt = scheduledAt,
        filledQuantity = filledQuantity, filledPrice = filledPrice,
        fee = fee, feeAsset = feeAsset, donationAmount = donationAmount,
        realizedPnl = realizedPnl, slippage = slippage, isPaperTrade = isPaperTrade,
        createdAt = createdAt, updatedAt = updatedAt, executedAt = executedAt,
        closedAt = closedAt, binanceOrderId = binanceOrderId, errorMessage = errorMessage,
        marketType = runCatching { MarketType.valueOf(marketType) }.getOrDefault(MarketType.SPOT),
        leverage = leverage,
        marginType = runCatching { MarginType.valueOf(marginType) }.getOrDefault(MarginType.ISOLATED),
        positionSide = runCatching { PositionSide.valueOf(positionSide) }.getOrDefault(PositionSide.BOTH),
        reduceOnly = reduceOnly,
        closePosition = closePosition
    )

    fun Order.toEntity(): OrderEntity = OrderEntity(
        id = id, signalId = signalId, symbol = symbol,
        side = side.name, type = type.name, status = status.name,
        executionMode = executionMode.name, quantity = quantity,
        price = price, stopPrice = stopPrice,
        takeProfitsJson = gson.toJson(takeProfits), stopLossesJson = gson.toJson(stopLosses),
        trailingStopPercent = trailingStopPercent,
        trailingStopActivationPrice = trailingStopActivationPrice,
        ocoLinkedOrderId = ocoLinkedOrderId, bracketParentId = bracketParentId,
        timeInForce = timeInForce.name, scheduledAt = scheduledAt,
        filledQuantity = filledQuantity, filledPrice = filledPrice,
        fee = fee, feeAsset = feeAsset, donationAmount = donationAmount,
        realizedPnl = realizedPnl, slippage = slippage, isPaperTrade = isPaperTrade,
        createdAt = createdAt, updatedAt = updatedAt, executedAt = executedAt,
        closedAt = closedAt, binanceOrderId = binanceOrderId, errorMessage = errorMessage,
        marketType = marketType.name,
        leverage = leverage,
        marginType = marginType.name,
        positionSide = positionSide.name,
        reduceOnly = reduceOnly,
        closePosition = closePosition
    )

    fun SignalEntity.toDomain(): Signal = Signal(
        id = id, symbol = symbol, side = OrderSide.valueOf(side),
        entryPrice = entryPrice,
        takeProfits = gson.fromJson(takeProfitsJson, object : TypeToken<List<TakeProfit>>() {}.type) ?: emptyList(),
        stopLosses = gson.fromJson(stopLossesJson, object : TypeToken<List<StopLoss>>() {}.type) ?: emptyList(),
        confidence = confidence, riskRewardRatio = riskRewardRatio,
        expiresAt = expiresAt, receivedAt = receivedAt,
        status = SignalStatus.valueOf(status), hmacSignature = hmacSignature,
        isExpired = isExpired, wasExecuted = wasExecuted, missedWhileOffline = missedWhileOffline
    )

    fun Signal.toEntity(): SignalEntity = SignalEntity(
        id = id, symbol = symbol, side = side.name,
        entryPrice = entryPrice,
        takeProfitsJson = gson.toJson(takeProfits), stopLossesJson = gson.toJson(stopLosses),
        confidence = confidence, riskRewardRatio = riskRewardRatio,
        expiresAt = expiresAt, receivedAt = receivedAt,
        status = status.name, hmacSignature = hmacSignature,
        isExpired = isExpired, wasExecuted = wasExecuted, missedWhileOffline = missedWhileOffline
    )

    fun TradingPairEntity.toDomain(): TradingPair = TradingPair(
        symbol = symbol, baseAsset = baseAsset, quoteAsset = quoteAsset,
        lastPrice = lastPrice, priceChangePercent24h = priceChangePercent24h,
        volume24h = volume24h, high24h = high24h, low24h = low24h,
        isWatchlisted = isWatchlisted, isActiveForTrading = isActiveForTrading,
        minQty = minQty, stepSize = stepSize, tickSize = tickSize, minNotional = minNotional
    )

    fun CandleEntity.toDomain(): Candle = Candle(
        symbol = symbol, interval = interval, openTime = openTime,
        open = open, high = high, low = low, close = close,
        volume = volume, closeTime = closeTime, quoteVolume = quoteVolume,
        numberOfTrades = numberOfTrades
    )

    fun AssetBalanceEntity.toDomain(): AssetBalance = AssetBalance(
        asset = asset, free = free, locked = locked,
        usdValue = usdValue, allocationPercent = allocationPercent,
        walletType = walletType
    )

    fun AuditLogEntity.toDomain(): AuditLog = AuditLog(
        id = id, action = AuditAction.valueOf(action), category = AuditCategory.valueOf(category),
        details = details, oldValue = oldValue, newValue = newValue,
        orderId = orderId, symbol = symbol, userId = userId,
        ipAddress = ipAddress, timestamp = timestamp
    )

    fun AuditLog.toEntity(): AuditLogEntity = AuditLogEntity(
        id = id, action = action.name, category = category.name,
        details = details, oldValue = oldValue, newValue = newValue,
        orderId = orderId, symbol = symbol, userId = userId,
        ipAddress = ipAddress, timestamp = timestamp
    )

    fun FeeRecordEntity.toDomain(): FeeRecord = FeeRecord(
        id = id, orderId = orderId, symbol = symbol,
        feeAmount = feeAmount, feeAsset = feeAsset,
        feeType = FeeType.valueOf(feeType), timestamp = timestamp
    )

    fun FeeRecord.toEntity(): FeeRecordEntity = FeeRecordEntity(
        id = id, orderId = orderId, symbol = symbol,
        feeAmount = feeAmount, feeAsset = feeAsset,
        feeType = feeType.name, timestamp = timestamp
    )

    fun DonationEntity.toDomain(): Donation = Donation(
        id = id, orderId = orderId, amount = amount, currency = currency,
        ngoName = ngoName, ngoId = ngoId,
        status = DonationStatus.valueOf(status), timestamp = timestamp
    )

    fun Donation.toEntity(): DonationEntity = DonationEntity(
        id = id, orderId = orderId, amount = amount, currency = currency,
        ngoName = ngoName, ngoId = ngoId, status = status.name, timestamp = timestamp
    )

    fun ScriptEntity.toDomain(): Script = Script(
        id = id, name = name, code = code, isActive = isActive,
        activeSymbol = activeSymbol,
        strategyTemplateId = strategyTemplateId,
        params = paramsJson?.let {
            try { gson.fromJson<Map<String, String>>(it, object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type) }
            catch (_: Exception) { emptyMap() }
        } ?: emptyMap(),
        lastRun = lastRun,
        backtestResult = backtestResultJson?.let { gson.fromJson(it, BacktestResult::class.java) },
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun Script.toEntity(): ScriptEntity = ScriptEntity(
        id = id, name = name, code = code, isActive = isActive,
        activeSymbol = activeSymbol,
        strategyTemplateId = strategyTemplateId,
        paramsJson = if (params.isNotEmpty()) gson.toJson(params) else null,
        lastRun = lastRun,
        backtestResultJson = backtestResult?.let { gson.toJson(it) },
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun SignalMarkerEntity.toDomain(): SignalMarker = SignalMarker(
        id = id, scriptId = scriptId, symbol = symbol, interval = interval,
        openTime = openTime, signalType = SignalType.valueOf(signalType),
        price = price, label = label, orderType = orderType, timestamp = timestamp
    )

    fun SignalMarker.toEntity(): SignalMarkerEntity = SignalMarkerEntity(
        id = id, scriptId = scriptId, symbol = symbol, interval = interval,
        openTime = openTime, signalType = signalType.name,
        price = price, label = label, orderType = orderType, timestamp = timestamp
    )

    fun CustomTemplateEntity.toDomain(): CustomTemplate = CustomTemplate(
        id = id, name = name, description = description,
        baseTemplateId = baseTemplateId, code = code,
        defaultParams = defaultParamsJson?.let {
            try { gson.fromJson<Map<String, String>>(it, stringMapType) }
            catch (_: Exception) { emptyMap() }
        } ?: emptyMap(),
        createdAt = createdAt
    )

    fun CustomTemplate.toEntity(): CustomTemplateEntity = CustomTemplateEntity(
        id = id, name = name, description = description,
        baseTemplateId = baseTemplateId, code = code,
        defaultParamsJson = if (defaultParams.isNotEmpty()) gson.toJson(defaultParams) else null,
        createdAt = createdAt
    )

    fun AlertEntity.toDomain(): Alert? = try {
        Alert(
            id = id, symbol = symbol, name = name,
            type = AlertType.valueOf(type),
            condition = AlertCondition.valueOf(condition),
            targetValue = targetValue, secondaryValue = secondaryValue,
            interval = interval, isEnabled = isEnabled,
            isRepeating = isRepeating, repeatIntervalSec = repeatIntervalSec,
            lastTriggeredAt = lastTriggeredAt, triggerCount = triggerCount,
            createdAt = createdAt, updatedAt = updatedAt
        )
    } catch (_: IllegalArgumentException) { null }

    fun Alert.toEntity(): AlertEntity = AlertEntity(
        id = id, symbol = symbol, name = name,
        type = type.name, condition = condition.name,
        targetValue = targetValue, secondaryValue = secondaryValue,
        interval = interval, isEnabled = isEnabled,
        isRepeating = isRepeating, repeatIntervalSec = repeatIntervalSec,
        lastTriggeredAt = lastTriggeredAt, triggerCount = triggerCount,
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun IndicatorEntity.toDomain(): Indicator = Indicator(
        id = id, name = name, code = code, isEnabled = isEnabled,
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun Indicator.toEntity(): IndicatorEntity = IndicatorEntity(
        id = id, name = name, code = code, isEnabled = isEnabled,
        createdAt = createdAt, updatedAt = updatedAt
    )
}
