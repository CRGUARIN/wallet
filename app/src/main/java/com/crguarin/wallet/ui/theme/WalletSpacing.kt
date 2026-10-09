package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these instead of loose dp literals in paddings and arrangements. */
@Immutable
data class WalletSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    /** Side margin of every screen. */
    val screenGutter: Dp = 16.dp,
    /** Inner padding of cards (section cards, hero). */
    val cardPadding: Dp = 20.dp,
)

val DefaultWalletSpacing = WalletSpacing()

val LocalWalletSpacing = staticCompositionLocalOf { DefaultWalletSpacing }

val MaterialTheme.walletSpacing: WalletSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletSpacing.current
