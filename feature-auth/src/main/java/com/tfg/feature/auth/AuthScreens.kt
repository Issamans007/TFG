package com.tfg.feature.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── SPLASH SCREEN ──────────────────────────────────────────────
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TradeForGood",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Trade Smart. Give Back.",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

// ─── ONBOARDING SCREEN ─────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        "Autonomous Trading" to "24/7 bot executes signals in the background, even when your phone is off.",
        "Smart Risk Control" to "Multi-layer risk engine protects your capital with stop-losses, drawdown limits, and kill switches.",
        "Trade For Good" to "A percentage of every profit automatically goes to verified NGOs. Make money, make impact."
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground).padding(24.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(state = pagerState, modifier = Modifier.weight(3f)) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pages[page].first,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pages[page].second,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Dots indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (i == pagerState.currentPage) AccentBlue else DarkBorder)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TfgButton(
            text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
            onClick = {
                if (pagerState.currentPage == pages.size - 1) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
        )

        if (pagerState.currentPage < pages.size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip", color = TextSecondary)
            }
        }
    }
}

// ─── LOGIN SCREEN ───────────────────────────────────────────────
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onGoToRegister: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.OTP) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sign in to continue trading", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.error != null) {
            ErrorMessage(message = state.error!!)
            Spacer(modifier = Modifier.height(8.dp))
        }

        TfgButton(
            text = "Sign In",
            onClick = { viewModel.login(email, password) },
            isLoading = state.isLoading,
            enabled = email.isNotBlank() && password.isNotBlank()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Register",
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onGoToRegister() }
            )
        }
    }
}

// ─── REGISTER SCREEN ────────────────────────────────────────────
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onGoToLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.OTP) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Start your trading journey", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = tfgTextFieldColors()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.error != null) {
            ErrorMessage(message = state.error!!)
            Spacer(modifier = Modifier.height(8.dp))
        }

        TfgButton(
            text = "Register",
            onClick = { viewModel.register(name, email, password) },
            isLoading = state.isLoading,
            enabled = name.isNotBlank() && email.isNotBlank() &&
                    password.length >= 8 && password == confirmPassword
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Sign In",
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onGoToLogin() }
            )
        }
    }
}

// ─── OTP VERIFICATION SCREEN ────────────────────────────────────
@Composable
fun OtpScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onVerified: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.PIN_SETUP) onVerified()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verify Your Email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter the 6-digit code sent to your email", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(40.dp))

        PinInput(
            pinLength = 6,
            onPinComplete = { viewModel.verifyOtp(it) },
            error = state.error
        )

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
        }
    }
}

// ─── PIN SETUP SCREEN ───────────────────────────────────────────
@Composable
fun PinSetupScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var step by remember { mutableStateOf(0) } // 0 = enter, 1 = confirm
    var firstPin by remember { mutableStateOf("") }

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.API_KEY_SETUP) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (step == 0) "Set Trading PIN" else "Confirm PIN",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (step == 0) "This PIN confirms every trade" else "Enter the same PIN again",
            fontSize = 14.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(40.dp))

        PinInput(
            pinLength = 6,
            onPinComplete = { pin ->
                if (step == 0) {
                    firstPin = pin
                    step = 1
                } else {
                    if (pin == firstPin) {
                        viewModel.setupPin(pin)
                    }
                }
            },
            error = if (step == 1) state.error else null
        )

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
        }
    }
}

// ─── API KEY SETUP SCREEN ────────────────────────────────────────
@Composable
fun ApiKeySetupScreen(
    onComplete: () -> Unit
) {
    val settingsViewModel: com.tfg.feature.settings.SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var isPaper by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showUpdateForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connect Binance", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter your Binance API credentials", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        // Show connected status if keys are already configured
        if (settingsState.apiKeyConfigured && !showUpdateForm) {
            TfgCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connected", fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50), fontSize = 14.sp)
                        Text(
                            if (settingsState.paperTrading) "Paper trading mode" else "Live trading mode",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TfgButton(
                text = "Update API Keys",
                onClick = { showUpdateForm = true }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    isLoading = true
                    settingsViewModel.revokeApiKeys(
                        onSuccess = {
                            isLoading = false
                        },
                        onError = { msg ->
                            isLoading = false
                            error = msg
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Text("Disconnect")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            // Show API key entry form
            TfgCard {
                Text("Security Note", fontWeight = FontWeight.SemiBold, color = AccentGold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Keys are encrypted with AES-256-GCM and stored in Android Keystore. We recommend using Spot-only permissions with IP whitelist.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paper Trading Mode", color = TextPrimary, fontSize = 14.sp)
                Switch(
                    checked = isPaper,
                    onCheckedChange = { isPaper = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
                )
            }

            if (isPaper) {
                Spacer(modifier = Modifier.height(8.dp))
                TfgCard {
                    Text(
                        "Paper trading enabled - trades will be simulated without real funds",
                        fontSize = 12.sp,
                        color = AccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("Enter your Binance API key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                colors = tfgTextFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiSecret,
                onValueChange = { apiSecret = it },
                label = { Text("API Secret") },
                placeholder = { Text("Enter your Binance API secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                colors = tfgTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                ErrorMessage(message = error!!)
                Spacer(modifier = Modifier.height(8.dp))
            }

            TfgButton(
                text = if (isPaper) "Start Paper Trading" else "Save & Connect",
                onClick = {
                    isLoading = true
                    error = null
                    if (isPaper) {
                        settingsViewModel.togglePaperTrading(true)
                        onComplete()
                    } else {
                        settingsViewModel.saveApiKeys(
                            apiKey = apiKey,
                            apiSecret = apiSecret,
                            onSuccess = {
                                isLoading = false
                                showUpdateForm = false
                                onComplete()
                            },
                            onError = { errorMsg ->
                                isLoading = false
                                error = errorMsg
                            }
                        )
                    }
                },
                isLoading = isLoading,
                enabled = if (isPaper) true else (apiKey.isNotBlank() && apiSecret.isNotBlank())
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (showUpdateForm) showUpdateForm = false
                    else onComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showUpdateForm) "Cancel" else "Skip for now", color = TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

// ─── HELPER ─────────────────────────────────────────────────────
@Composable
fun tfgTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
