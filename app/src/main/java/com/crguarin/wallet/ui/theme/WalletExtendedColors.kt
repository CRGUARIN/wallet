package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.min

/**
 * Colors the Material 3 scheme has no role for: the balance card ("hero"), the warm "needs
 * attention" accent and how strongly account and category colors are tinted.
 *
 * With dynamic color the hero follows the wallpaper scheme ([dynamicWalletExtendedColors]); the
 * rest stays fixed.
 */
@Immutable
data class WalletExtendedColors(
    /** Start (top-left) of the hero gradient. */
    val heroStart: Color,
    /** End (bottom-right) of the hero gradient. */
    val heroEnd: Color,
    /** Text and icons over the hero gradient. Always at least 4.5:1 against both ends. */
    val onHero: Color,
    /** Contour lines drawn over the hero: [onHero] at 11% opacity. */
    val heroContour: Color,
    /** Start (and end) color of the attention ring sweep. */
    val attentionStart: Color,
    /** Amber in the middle of the attention ring sweep. */
    val attentionMid: Color,
    /** Dot that flags an avatar that needs attention. Draw it with a 2 dp border of the surface color. */
    val attentionBadge: Color,
    /** Fraction of an account or category color mixed over its base layer: 0.22 light, 0.30 dark. */
    val tintFraction: Float,
    val isDark: Boolean,
)

private const val HERO_CONTOUR_ALPHA = 0.11f
private const val TINT_FRACTION_LIGHT = 0.22f
private const val TINT_FRACTION_DARK = 0.30f
private const val DYNAMIC_HERO_DARKEN_LIGHT = 0.35f
private const val DYNAMIC_HERO_DARKEN_DARK = 0.4f
private const val HERO_FIX_STEPS = 50
private val AttentionAmber = Color(0xFFE59E2A)

val LightWalletExtendedColors =
    WalletExtendedColors(
        heroStart = Color(0xFF1E5A4A),
        heroEnd = Color(0xFF0F3B3A),
        onHero = Color.White,
        heroContour = Color.White.copy(alpha = HERO_CONTOUR_ALPHA),
        attentionStart = TertiaryLight,
        attentionMid = AttentionAmber,
        attentionBadge = TertiaryLight,
        tintFraction = TINT_FRACTION_LIGHT,
        isDark = false,
    )

val DarkWalletExtendedColors =
    WalletExtendedColors(
        heroStart = Color(0xFF22594A),
        heroEnd = Color(0xFF10302F),
        onHero = Color.White,
        heroContour = Color.White.copy(alpha = HERO_CONTOUR_ALPHA),
        attentionStart = TertiaryDark,
        attentionMid = AttentionAmber,
        attentionBadge = TertiaryDark,
        tintFraction = TINT_FRACTION_DARK,
        isDark = true,
    )

/** The hero gradient ends with the color of the text over them. */
@Immutable
data class WalletHeroColors(
    val start: Color,
    val end: Color,
    val onHero: Color,
)

/**
 * Hero gradient for a dynamic color scheme: light uses [primary] fading to black (35%), dark uses
 * [primaryContainer] fading to black (40%).
 *
 * The text is black or white, whichever is the most legible over both ends (over the lightest one
 * for any real Material You scheme). For the rare mid-luminance color where neither reaches 4.6:1
 * on both ends, the gradient is darkened in steps of 2% until white does.
 */
fun dynamicHeroColors(
    primary: Color,
    primaryContainer: Color,
    isDark: Boolean,
): WalletHeroColors {
    val start = if (isDark) primaryContainer else primary
    val end = lerp(start, Color.Black, if (isDark) DYNAMIC_HERO_DARKEN_DARK else DYNAMIC_HERO_DARKEN_LIGHT)

    fun worst(
        content: Color,
        a: Color,
        b: Color,
    ) = min(contrastRatio(content, a), contrastRatio(content, b))

    val best = listOf(Color.White, Color.Black).maxBy { worst(it, start, end) }
    if (worst(best, start, end) >= SAFE_TEXT_CONTRAST) return WalletHeroColors(start, end, best)

    for (step in 1..HERO_FIX_STEPS) {
        val fraction = step.toFloat() / HERO_FIX_STEPS
        val darkStart = lerp(start, Color.Black, fraction)
        val darkEnd = lerp(end, Color.Black, fraction)
        if (worst(Color.White, darkStart, darkEnd) >= SAFE_TEXT_CONTRAST) {
            return WalletHeroColors(darkStart, darkEnd, Color.White)
        }
    }
    return WalletHeroColors(Color.Black, Color.Black, Color.White)
}

/** Extended colors for a dynamic scheme: hero from the wallpaper colors, the rest as the fixed scheme. */
fun dynamicWalletExtendedColors(
    primary: Color,
    primaryContainer: Color,
    isDark: Boolean,
): WalletExtendedColors {
    val fixed = if (isDark) DarkWalletExtendedColors else LightWalletExtendedColors
    val hero = dynamicHeroColors(primary, primaryContainer, isDark)
    return fixed.copy(
        heroStart = hero.start,
        heroEnd = hero.end,
        onHero = hero.onHero,
        heroContour = hero.onHero.copy(alpha = HERO_CONTOUR_ALPHA),
    )
}

val LocalWalletExtendedColors = staticCompositionLocalOf { LightWalletExtendedColors }

val MaterialTheme.walletColors: WalletExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletExtendedColors.current
