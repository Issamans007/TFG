package com.tfg.domain.usecase.auth

import com.tfg.domain.model.User
import com.tfg.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        return authRepository.login(email, password)
    }
}

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name is required"))
        }
        return authRepository.register(email, password, name)
    }
}

class VerifyOtpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Result<Boolean> {
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("OTP must be 6 digits"))
        }
        return authRepository.verifyOtp(email, otp)
    }
}

class SetupTradingPinUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(pin: String): Result<Boolean> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN must be 6 digits"))
        }
        return authRepository.setupTradingPin(pin)
    }
}

class VerifyTradingPinUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(pin: String): Result<Boolean> {
        return authRepository.verifyTradingPin(pin)
    }
}
