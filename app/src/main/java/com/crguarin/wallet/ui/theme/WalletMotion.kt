package com.crguarin.wallet.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Durations (milliseconds) and scales of the animations. Subtle on purpose: nothing may get in the
 * way of quick entry. With the system "remove animations" setting everything must finish at once.
 *
 * Always use these tokens, never loose numbers.
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
    /** Card flights, entrances and the deck deal. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f),
    /** The odometer roll. */
    val roll: Easing = CubicBezierEasing(0.15f, 0.8f, 0.2f, 1f),
    /** Delay between staggered blocks. */
    val stagger: Int = 40,
    /** Delay between dealt cards. */
    val dealStagger: Int = 80,
    /** Delay between rolling digits, from the left. */
    val digitStagger: Int = 50,
    /** How long the idle loops (floating, sheen) run on a quiet screen. */
    val idleLoopWindow: Int = 10_000,
    /** Period of the floating loop. */
    val floatPeriod: Int = 6_000,
    /** Period of the sheen sweep. */
    val sheenPeriod: Int = 7_000,
    /** Rings of the pending-payment pulse. */
    val pulseCount: Int = 3,
    /** Duration of each pulse ring. */
    val pulseMillis: Int = 480,
    /** Pause between pulse rings. */
    val pulseGap: Int = 120,
)

val DefaultWalletMotion = WalletMotion()

val LocalWalletMotion = staticCompositionLocalOf { DefaultWalletMotion }

val MaterialTheme.walletMotion: WalletMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletMotion.current
