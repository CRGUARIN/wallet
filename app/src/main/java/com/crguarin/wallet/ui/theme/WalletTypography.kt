package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** OpenType feature that makes digits the same width, so amounts do not jiggle while they change. */
const val TABULAR_FIGURES = "tnum"

/**
 * Text styles of monetary amounts, beyond the Material 3 scale. All use Manrope 800 and tabular
 * figures. They carry no color: the caller picks it (semantic color, onSurface, onHero).
 */
@Immutable
data class WalletTypography(
    /** Main balance on the hero card: 42/50 sp. It auto-fits down to 22 sp, see `HeroAmountText`. */
    val heroAmount: TextStyle,
    /** Amount in a transaction row: 15/20 sp. */
    val rowAmount: TextStyle,
    /** Amount in the transaction form: 44 sp. */
    val formAmount: TextStyle,
)

val DefaultWalletTypography =
    WalletTypography(
        heroAmount =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 42.sp,
                lineHeight = 50.sp,
                letterSpacing = (-0.02).em,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
        rowAmount =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
        formAmount =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                letterSpacing = (-0.02).em,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
    )

val LocalWalletTypography = staticCompositionLocalOf { DefaultWalletTypography }

val MaterialTheme.walletTypography: WalletTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletTypography.current
