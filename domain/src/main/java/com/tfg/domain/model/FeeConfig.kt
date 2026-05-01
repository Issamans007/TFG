package com.tfg.domain.model

data class FeeConfig(
    val makerFeeRate: Double = 0.001,
    val takerFeeRate: Double = 0.001,
    // Binance USDⓈ-M futures defaults: 0.02% maker / 0.04% taker
    val futuresMakerFeeRate: Double = 0.0002,
    val futuresTakerFeeRate: Double = 0.0004,
    val bnbDiscountEnabled: Boolean = false,
    val bnbDiscountRate: Double = 0.25,
    val donationPercent: Double = 5.0,
    val vipLevel: Int = 0
) {
    fun effectiveMakerRate(): Double =
        if (bnbDiscountEnabled) makerFeeRate * (1 - bnbDiscountRate) else makerFeeRate

    fun effectiveTakerRate(): Double =
        if (bnbDiscountEnabled) takerFeeRate * (1 - bnbDiscountRate) else takerFeeRate

    fun effectiveFuturesMakerRate(): Double =
        if (bnbDiscountEnabled) futuresMakerFeeRate * (1 - bnbDiscountRate) else futuresMakerFeeRate

    fun effectiveFuturesTakerRate(): Double =
        if (bnbDiscountEnabled) futuresTakerFeeRate * (1 - bnbDiscountRate) else futuresTakerFeeRate

    fun estimateFee(notional: Double, isMaker: Boolean): Double =
        notional * if (isMaker) effectiveMakerRate() else effectiveTakerRate()

    /** Estimate fee with explicit market type. Notional should be position notional (qty*price*leverage). */
    fun estimateFee(notional: Double, isMaker: Boolean, marketType: MarketType): Double = when (marketType) {
        MarketType.SPOT -> notional * if (isMaker) effectiveMakerRate() else effectiveTakerRate()
        MarketType.FUTURES_USDM ->
            notional * if (isMaker) effectiveFuturesMakerRate() else effectiveFuturesTakerRate()
    }

    fun estimateDonation(profit: Double): Double =
        if (profit > 0) profit * donationPercent / 100.0 else 0.0
}

data class FeeRecord(
    val id: String,
    val orderId: String,
    val symbol: String,
    val feeAmount: Double,
    val feeAsset: String,
    val feeType: FeeType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FeeType { TRADING, DONATION, WITHDRAWAL, NETWORK }
