package com.crguarin.wallet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = GreenPrimaryDark,
        onPrimary = GreenOnPrimaryDark,
        primaryContainer = GreenPrimaryContainerDark,
        onPrimaryContainer = GreenOnPrimaryContainerDark,
        secondary = GreenSecondaryDark,
        onSecondary = GreenOnSecondaryDark,
        secondaryContainer = GreenSecondaryContainerDark,
        onSecondaryContainer = GreenOnSecondaryContainerDark,
        tertiary = GreenTertiaryDark,
        onTertiary = GreenOnTertiaryDark,
        tertiaryContainer = GreenTertiaryContainerDark,
        onTertiaryContainer = GreenOnTertiaryContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        surfaceBright = SurfaceBrightDark,
        surfaceDim = SurfaceDimDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = GreenPrimaryLight,
        onPrimary = GreenOnPrimaryLight,
        primaryContainer = GreenPrimaryContainerLight,
        onPrimaryContainer = GreenOnPrimaryContainerLight,
        secondary = GreenSecondaryLight,
        onSecondary = GreenOnSecondaryLight,
        secondaryContainer = GreenSecondaryContainerLight,
        onSecondaryContainer = GreenOnSecondaryContainerLight,
        tertiary = GreenTertiaryLight,
        onTertiary = GreenOnTertiaryLight,
        tertiaryContainer = GreenTertiaryContainerLight,
        onTertiaryContainer = GreenOnTertiaryContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        surfaceBright = SurfaceBrightLight,
        surfaceDim = SurfaceDimLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
    )

@Composable
fun WalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val semanticColors = if (darkTheme) DarkWalletSemanticColors else LightWalletSemanticColors

    CompositionLocalProvider(
        LocalWalletSemanticColors provides semanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WalletShapes,
            content = content,
        )
    }
}
