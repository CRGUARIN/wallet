package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for monetary amounts (income, expense, transfer). They are the same with dynamic
 * color. Money is never told apart by color alone: the sign and the icon stay.
 */
@Immutable
data class WalletSemanticColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
)

// At least 4.7:1 over surface and every surfaceContainer*, checked by ThemeContrastTest.
val LightWalletSemanticColors =
    WalletSemanticColors(
        income = Color(0xFF15703F),
        expense = Color(0xFFB3261E),
        transfer = Color(0xFF4C635A),
    )

// At least 5.6:1 over surface and every surfaceContainer*.
val DarkWalletSemanticColors =
    WalletSemanticColors(
        income = Color(0xFF7BDB9B),
        expense = Color(0xFFFF8F86),
        transfer = Color(0xFFB2CCC0),
    )

val LocalWalletSemanticColors =
    staticCompositionLocalOf { LightWalletSemanticColors }

val MaterialTheme.walletSemanticColors: WalletSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletSemanticColors.current
