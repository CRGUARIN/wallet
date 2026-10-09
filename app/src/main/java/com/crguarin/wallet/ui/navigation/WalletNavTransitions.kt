package com.crguarin.wallet.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crguarin.wallet.ui.theme.DefaultWalletMotion

/** Total duration of a transition between tabs or screens: `walletMotion.medium` (220 ms). */
private val TRANSITION_MILLIS = DefaultWalletMotion.medium

/**
 * Fade-through: the leaving screen fades out during the first two fifths of the transition and the
 * entering one fades in during the rest, so the two never overlap.
 */
private val FADE_OUT_MILLIS = TRANSITION_MILLIS * 2 / 5
private val FADE_IN_MILLIS = TRANSITION_MILLIS - FADE_OUT_MILLIS

/** Fraction of the width a screen travels while sliding; small so it reads as a nudge, not a swipe. */
private const val SLIDE_OFFSET_DIVISOR = 8

private typealias NavTransitionScope = AnimatedContentTransitionScope<NavBackStackEntry>

fun NavDestination?.isTopLevel(): Boolean =
    this?.hierarchy?.any { destination ->
        TopLevelDestination.entries.any { destination.hasRoute(it.routeClass) }
    } == true

private fun NavTransitionScope.isTabSwitch(): Boolean = initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

private fun fadeThroughIn(): EnterTransition = fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))

private fun fadeThroughOut(): ExitTransition = fadeOut(tween(FADE_OUT_MILLIS))

// Tabs fade through (the old one leaves quickly, the new one fades in); opening and closing
// a screen also slides it slightly along the horizontal axis, like Material's shared X axis. Both
// take `walletMotion.medium` in total.

fun NavTransitionScope.walletEnterTransition(): EnterTransition =
    if (isTabSwitch()) {
        fadeThroughIn()
    } else {
        slideInHorizontally(tween(TRANSITION_MILLIS)) { it / SLIDE_OFFSET_DIVISOR } + fadeThroughIn()
    }

fun NavTransitionScope.walletExitTransition(): ExitTransition =
    if (isTabSwitch()) {
        fadeThroughOut()
    } else {
        slideOutHorizontally(tween(TRANSITION_MILLIS)) { -it / SLIDE_OFFSET_DIVISOR } + fadeThroughOut()
    }

fun NavTransitionScope.walletPopEnterTransition(): EnterTransition =
    if (isTabSwitch()) {
        fadeThroughIn()
    } else {
        slideInHorizontally(tween(TRANSITION_MILLIS)) { -it / SLIDE_OFFSET_DIVISOR } + fadeThroughIn()
    }

fun NavTransitionScope.walletPopExitTransition(): ExitTransition =
    if (isTabSwitch()) {
        fadeThroughOut()
    } else {
        slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / SLIDE_OFFSET_DIVISOR } + fadeThroughOut()
    }
