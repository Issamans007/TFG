package com.tfg.domain.model

data class FeeConfig(
    val makerFeeRate: Double = 0.001,
    val takerFeeRate: Double = 0.001,
    val bnbDiscountEnabled: Boolean = false,
    val bnbDiscountRate: Double = 0.25,
    val donationPercent: Double = 5.0,
    val vipLevel: Int = 0
) {
    fun effectiveMakerRate(): Double =
        if (bnbDiscountEnabled) makerFeeRate * (1 - bnbDiscountRate) else makerFeeRate

    fun effectiveTakerRate(): Double =
        if (bnbDiscountEnabled) takerFeeRate * (1 - bnbDiscountRate) else takerFeeRate

    fun estimateFee(notional: Double, isMaker: Boolean): Double =
        notional * if (isMaker) effectiveMakerRate() else effectiveTakerRate()

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
