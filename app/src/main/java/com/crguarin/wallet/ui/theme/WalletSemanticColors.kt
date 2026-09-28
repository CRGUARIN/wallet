package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for monetary amounts (income, expense, transfer).
 */
@Immutable
data class WalletSemanticColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
)

val LightWalletSemanticColors =
    WalletSemanticColors(
        income = Color(0xFF2E7D32), // Green 800
        expense = Color(0xFFC62828), // Red 800
        transfer = Color(0xFF455A64), // Blue Grey 700 (neutral)
    )

val DarkWalletSemanticColors =
    WalletSemanticColors(
        income = Color(0xFF81C784), // Green 300
        expense = Color(0xFFE57373), // Red 300
        transfer = Color(0xFFB0BEC5), // Blue Grey 200 (neutral)
    )

val LocalWalletSemanticColors =
    staticCompositionLocalOf { LightWalletSemanticColors }

val MaterialTheme.walletSemanticColors: WalletSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletSemanticColors.current
