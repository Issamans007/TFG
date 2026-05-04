package com.tfg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tfg.core.ui.theme.TfgTheme
import com.tfg.core.ui.theme.LocalTfgColors
import com.tfg.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import com.tfg.domain.repository.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent the app from appearing in the recent-apps thumbnail and block
        // OS-level screenshot / screen-recording capture on all screens.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        setContent {
            val themeFlow = remember { settingsRepository.getTheme() }
            val themePref by themeFlow.collectAsState(initial = "dark")
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePref) {
                "light" -> false
                "system" -> systemDark
                else -> true
            }

            TfgTheme(darkTheme = isDark) {
                val colors = LocalTfgColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
