package com.tfg.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tfg.core.ui.theme.*
import com.tfg.feature.auth.*
import com.tfg.feature.chart.ChartScreen
import com.tfg.feature.chart.CoinDetailScreen
import com.tfg.feature.dashboard.DashboardScreen
import com.tfg.feature.markets.MarketsScreen
import com.tfg.feature.notifications.NotificationsScreen
import com.tfg.feature.portfolio.PortfolioScreen
import com.tfg.feature.risk.RiskScreen
import com.tfg.feature.script.ScriptScreen
import com.tfg.feature.settings.SettingsScreen
import com.tfg.feature.trade.TradeScreen
import com.tfg.feature.alerts.AlertsScreen
import com.tfg.feature.news.NewsScreen

// Routes
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP = "otp"
    const val PIN_SETUP = "pin_setup"
    const val API_KEY_SETUP = "api_key_setup"
    const val DASHBOARD = "dashboard"
    const val MARKETS = "markets"
    const val TRADE = "trade/{symbol}"
    const val CHART = "chart/{symbol}"
    const val COIN_DETAIL = "coin_detail/{symbol}"
    const val PORTFOLIO = "portfolio"
    const val SCRIPT = "script"
    const val SETTINGS = "settings"
    const val RISK = "risk"
    const val NOTIFICATIONS = "notifications"
    const val ALERTS = "alerts"
    const val NEWS = "news"

    fun trade(symbol: String) = "trade/$symbol"
    fun chart(symbol: String) = "chart/$symbol"
    fun coinDetail(symbol: String) = "coin_detail/$symbol"
}

// Bottom navigation items
enum class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    DASHBOARD(Routes.DASHBOARD, Icons.Default.Home, "Home"),
    MARKETS(Routes.MARKETS, Icons.Default.ShowChart, "Markets"),
    NEWS(Routes.NEWS, Icons.Default.Newspaper, "News"),
    PORTFOLIO(Routes.PORTFOLIO, Icons.Default.AccountBalance, "Portfolio"),
    SCRIPT(Routes.SCRIPT, Icons.Default.Code, "Scripts"),
    SETTINGS(Routes.SETTINGS, Icons.Default.Settings, "Settings")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val showBottomBar = currentRoute in BottomNavItem.entries.map { it.route }
    val colors = com.tfg.core.ui.theme.TfgTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentBlue,
                                selectedTextColor = AccentBlue,
                                unselectedIconColor = colors.textTertiary,
                                unselectedTextColor = colors.textTertiary,
                                indicatorColor = AccentBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Auth flow
            composable(Routes.SPLASH) {
                SplashScreen(onTimeout = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                    onSuccess = { navController.navigate(Routes.OTP) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onGoToLogin = { navController.popBackStack() },
                    onSuccess = { navController.navigate(Routes.OTP) }
                )
            }
            composable(Routes.OTP) {
                OtpScreen(onVerified = { navController.navigate(Routes.PIN_SETUP) })
            }
            composable(Routes.PIN_SETUP) {
                PinSetupScreen(onComplete = { navController.navigate(Routes.API_KEY_SETUP) })
            }
            composable(Routes.API_KEY_SETUP) {
                ApiKeySetupScreen(onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }

            // Main tabs
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToTrade = { navController.navigate(Routes.trade(it)) },
                    onNavigateToPortfolio = { navController.navigate(Routes.PORTFOLIO) },
                    onNavigateToMarkets = { navController.navigate(Routes.MARKETS) },
                    onNavigateToConsole = { navController.navigate(Routes.NOTIFICATIONS) },
                    onNavigateToAlerts = { navController.navigate(Routes.ALERTS) }
                )
            }
            composable(Routes.MARKETS) {
                MarketsScreen(onPairClick = { navController.navigate(Routes.coinDetail(it)) })
            }
            composable(Routes.PORTFOLIO) {
                PortfolioScreen(onNavigateToTrade = { navController.navigate(Routes.trade(it)) })
            }
            composable(Routes.SCRIPT) {
                ScriptScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onNavigateToApiKey = { navController.navigate(Routes.API_KEY_SETUP) })
            }

            // Detail screens
            composable(
                Routes.TRADE,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) {
                TradeScreen(onNavigateToChart = { navController.navigate(Routes.chart(it)) })
            }
            composable(
                Routes.CHART,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: "BTCUSDT"
                ChartScreen(symbol = symbol, onBack = { navController.popBackStack() })
            }

            // Coin detail
            composable(
                Routes.COIN_DETAIL,
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) {
                CoinDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTrade = { navController.navigate(Routes.trade(it)) }
                )
            }

            composable(Routes.NEWS) { NewsScreen() }
            composable(Routes.RISK) { RiskScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.ALERTS) { AlertsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
