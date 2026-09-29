package com.crguarin.wallet

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.ui.MainUiState
import com.crguarin.wallet.ui.MainViewModel
import com.crguarin.wallet.ui.WalletAppContent
import com.crguarin.wallet.ui.components.BrandIntro
import com.crguarin.wallet.ui.navigation.Home
import com.crguarin.wallet.ui.navigation.Onboarding
import com.crguarin.wallet.ui.theme.WalletTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the splash replaces the starting theme.
        val compatSplashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        compatSplashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
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

                    val dynamicColor = state.preferences.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    LaunchedEffect(dynamicColor) { rememberSplashColors(dynamicColor) }

                    WalletTheme(
                        darkTheme = darkTheme,
                        dynamicColor = dynamicColor,
                    ) {
                        // Decided once: finishing onboarding navigates to Home itself, and changing
                        // the NavHost start destination afterwards would reset the back stack.
                        val startDestination: Any =
                            remember {
                                if (state.onboardingCompleted) Home else Onboarding
                            }
                        Box {
                            WalletAppContent(startDestination = startDestination)
                            // Cold start only: saved state keeps it from replaying after recreation.
                            var showIntro by rememberSaveable { mutableStateOf(true) }
                            if (showIntro) {
                                BrandIntro(onFinished = { showIntro = false }, dynamicColor = dynamicColor)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The system draws the splash before preferences load, so it cannot read the dynamic color
     * setting. On Android 13+ the chosen splash theme is saved for the next launches instead.
     */
    private fun rememberSplashColors(dynamicColor: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(
                if (dynamicColor) R.style.Theme_Wallet_Starting else R.style.Theme_Wallet_Starting_Green,
            )
        }
    }
}
