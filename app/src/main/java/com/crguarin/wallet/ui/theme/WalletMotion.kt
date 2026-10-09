package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Durations (milliseconds) and scales of the animations. Subtle on purpose: nothing may get in the
 * way of quick entry. With the system "remove animations" setting everything must finish at once.
 */
@Immutable
data class WalletMotion(
    /** Color changes of chips and segments. */
    val fast: Int = 150,
    /** Fade-through between tabs and screens. */
    val medium: Int = 220,
    /** Entrances such as the brand intro rise. */
    val slow: Int = 450,
    /** Tracing of the hero contour lines, the first time Home is composed in the process. */
    val contour: Int = 700,
    /** Scale of a pressed element. */
    val pressScale: Float = 0.97f,
    /** Duration of the press scale. */
    val pressMillis: Int = 100,
)

val DefaultWalletMotion = WalletMotion()

val LocalWalletMotion = staticCompositionLocalOf { DefaultWalletMotion }

val MaterialTheme.walletMotion: WalletMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletMotion.current
