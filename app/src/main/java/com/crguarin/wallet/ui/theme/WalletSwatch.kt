package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * How one of the 30 account and category colors is painted, so the screen does not turn into a
 * rainbow: a soft tint as the background, a toned shade of the same color for the icon or text
 * over it, and the pure color for decorative rings.
 */
@Immutable
data class WalletSwatch(
    /** The color mixed over the base layer (22% light, 30% dark). Draw the avatar background with it. */
    val container: Color,
    /** The color moved towards black (light) or white (dark) until it is at least 4.6:1 over [container]. */
    val content: Color,
    /** The pure color, for the 2 dp account ring. Decorative: never the only carrier of meaning. */
    val ring: Color,
)

/** Pure version of [rememberWalletSwatch], for tests and non-composable code. */
fun walletSwatch(
    color: Color,
    base: Color,
    isDark: Boolean,
): WalletSwatch {
    val fraction = if (isDark) DarkWalletExtendedColors.tintFraction else LightWalletExtendedColors.tintFraction
    val container = tintOver(color, base, fraction)
    val content = tonedFor(color, container, toward = if (isDark) Color.White else Color.Black, minRatio = SAFE_TEXT_CONTRAST)
    return WalletSwatch(container = container, content = content, ring = color)
}

/**
 * [WalletSwatch] of [color] over [base] (by default the layer cards sit on, `surfaceContainerLow`),
 * following the light or dark theme.
 */
@Composable
fun rememberWalletSwatch(
    color: Color,
    base: Color = MaterialTheme.colorScheme.surfaceContainerLow,
): WalletSwatch {
    val isDark = MaterialTheme.walletColors.isDark
    return remember(color, base, isDark) { walletSwatch(color, base, isDark) }
}
