package com.tfg.domain.repository

import com.tfg.domain.model.User
import com.tfg.domain.model.KycStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun verifyOtp(email: String, otp: String): Result<Boolean>
    suspend fun sendOtp(email: String): Result<Boolean>
    suspend fun submitKyc(documentUrl: String): Result<Boolean>
    fun getKycStatus(): Flow<KycStatus>
    suspend fun setupTradingPin(pin: String): Result<Boolean>
    suspend fun verifyTradingPin(pin: String): Result<Boolean>
    suspend fun enableBiometric(): Result<Boolean>
    suspend fun logout()
    fun getCurrentUser(): Flow<User?>
    fun isLoggedIn(): Flow<Boolean>
}
