package com.crguarin.wallet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.ui.MainUiState
import com.crguarin.wallet.ui.MainViewModel
import com.crguarin.wallet.ui.WalletAppContent
import com.crguarin.wallet.ui.navigation.Home
import com.crguarin.wallet.ui.navigation.Onboarding
import com.crguarin.wallet.ui.theme.WalletTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            when (val state = uiState) {
                is MainUiState.Loading -> {
                    // Do not show navigation while loading to avoid flashing Home
                }
                is MainUiState.Ready -> {
                    val darkTheme =
                        when (state.preferences.themeMode) {
                            ThemeMode.SYSTEM -> isSystemInDarkTheme()
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                        }

                    // Keep status/navigation bar icons readable when the app theme differs from the system one.
                    DisposableEffect(darkTheme) {
                        val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                        onDispose {}
                    }

                    WalletTheme(
                        darkTheme = darkTheme,
                        dynamicColor = state.preferences.dynamicColor,
                    ) {
                        // Decided once: finishing onboarding navigates to Home itself, and changing
                        // the NavHost start destination afterwards would reset the back stack.
                        val startDestination: Any =
                            remember {
                                if (state.onboardingCompleted) Home else Onboarding
                            }
                        WalletAppContent(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
