package com.tfg.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.User
import com.tfg.domain.usecase.auth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val step: AuthStep = AuthStep.SPLASH,
    val otpSent: Boolean = false,
    val pinSetupDone: Boolean = false,
    val apiKeyConfigured: Boolean = false
)

enum class AuthStep { SPLASH, ONBOARDING, LOGIN, REGISTER, OTP, PIN_SETUP, API_KEY_SETUP, BIOMETRIC_SETUP, DONE }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val setupTradingPinUseCase: SetupTradingPinUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginUseCase(email, password)
                .onSuccess { user ->
                    _state.update { it.copy(isLoading = false, user = user, step = AuthStep.OTP) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            registerUseCase(name, email, password)
                .onSuccess { user ->
                    _state.update { it.copy(isLoading = false, user = user, step = AuthStep.OTP) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            verifyOtpUseCase(_state.value.user?.id ?: "", code)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, otpSent = true, step = AuthStep.PIN_SETUP) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            setupTradingPinUseCase(pin)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, pinSetupDone = true, step = AuthStep.API_KEY_SETUP) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun setApiKeyConfigured() {
        _state.update { it.copy(apiKeyConfigured = true, step = AuthStep.BIOMETRIC_SETUP) }
    }

    fun completeBiometric() {
        _state.update { it.copy(step = AuthStep.DONE) }
    }

    fun skipToLogin() {
        _state.update { it.copy(step = AuthStep.LOGIN) }
    }

    fun goToRegister() {
        _state.update { it.copy(step = AuthStep.REGISTER) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
