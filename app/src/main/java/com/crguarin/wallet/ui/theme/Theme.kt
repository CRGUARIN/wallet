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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// background = surface, onBackground = onSurface, surfaceTint = primary, scrim = black.
val WalletDarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        inversePrimary = InversePrimaryDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        background = SurfaceDark,
        onBackground = OnSurfaceDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        surfaceTint = PrimaryDark,
        inverseSurface = InverseSurfaceDark,
        inverseOnSurface = InverseOnSurfaceDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        scrim = ScrimColor,
        surfaceBright = SurfaceBrightDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceDim = SurfaceDimDark,
        primaryFixed = PrimaryFixed,
        primaryFixedDim = PrimaryFixedDim,
        onPrimaryFixed = OnPrimaryFixed,
        onPrimaryFixedVariant = OnPrimaryFixedVariant,
        secondaryFixed = SecondaryFixed,
        secondaryFixedDim = SecondaryFixedDim,
        onSecondaryFixed = OnSecondaryFixed,
        onSecondaryFixedVariant = OnSecondaryFixedVariant,
        tertiaryFixed = TertiaryFixed,
        tertiaryFixedDim = TertiaryFixedDim,
        onTertiaryFixed = OnTertiaryFixed,
        onTertiaryFixedVariant = OnTertiaryFixedVariant,
    )

val WalletLightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        inversePrimary = InversePrimaryLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        background = SurfaceLight,
        onBackground = OnSurfaceLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        surfaceTint = PrimaryLight,
        inverseSurface = InverseSurfaceLight,
        inverseOnSurface = InverseOnSurfaceLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        scrim = ScrimColor,
        surfaceBright = SurfaceBrightLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceDim = SurfaceDimLight,
        primaryFixed = PrimaryFixed,
        primaryFixedDim = PrimaryFixedDim,
        onPrimaryFixed = OnPrimaryFixed,
        onPrimaryFixedVariant = OnPrimaryFixedVariant,
        secondaryFixed = SecondaryFixed,
        secondaryFixedDim = SecondaryFixedDim,
        onSecondaryFixed = OnSecondaryFixed,
        onSecondaryFixedVariant = OnSecondaryFixedVariant,
        tertiaryFixed = TertiaryFixed,
        tertiaryFixedDim = TertiaryFixedDim,
        onTertiaryFixed = OnTertiaryFixed,
        onTertiaryFixedVariant = OnTertiaryFixedVariant,
    )

/**
 * App theme. With [dynamicColor] (Android 12+) the Material roles and the hero gradient come from
 * the wallpaper; typography, shapes, money colors and the other Wallet tokens stay fixed.
 *
 * Besides Material 3 it provides `walletSemanticColors`, `walletColors`, `walletSpacing`,
 * `walletMotion` and `walletTypography` on [MaterialTheme].
 */
@Composable
fun WalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme =
        when {
            // Checked inline (not through useDynamic) so lint sees the API 31 guard.
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> WalletDarkColorScheme
            else -> WalletLightColorScheme
        }

    val semanticColors = if (darkTheme) DarkWalletSemanticColors else LightWalletSemanticColors
    val extendedColors =
        remember(useDynamic, darkTheme, colorScheme.primary, colorScheme.primaryContainer) {
            when {
                useDynamic -> dynamicWalletExtendedColors(colorScheme.primary, colorScheme.primaryContainer, darkTheme)
                darkTheme -> DarkWalletExtendedColors
                else -> LightWalletExtendedColors
            }
        }

    CompositionLocalProvider(
        LocalWalletSemanticColors provides semanticColors,
        LocalWalletExtendedColors provides extendedColors,
        LocalWalletSpacing provides DefaultWalletSpacing,
        LocalWalletMotion provides DefaultWalletMotion,
        LocalWalletTypography provides DefaultWalletTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WalletShapes,
            content = content,
        )
    }
}
