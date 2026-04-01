package com.tfg.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val kycStatus: KycStatus = KycStatus.PENDING,
    val biometricEnabled: Boolean = false,
    val tradingPinSet: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class KycStatus { PENDING, SUBMITTED, VERIFIED, REJECTED }
