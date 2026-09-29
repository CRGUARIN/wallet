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

private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210
private const val SLIDE_MILLIS = 300

/** Fraction of the width a screen travels while sliding; small so it reads as a nudge, not a swipe. */
private const val SLIDE_OFFSET_DIVISOR = 8

private typealias NavTransitionScope = AnimatedContentTransitionScope<NavBackStackEntry>

fun NavDestination?.isTopLevel(): Boolean =
    this?.hierarchy?.any { destination ->
        TopLevelDestination.entries.any { destination.hasRoute(it.routeClass) }
    } == true

private fun NavTransitionScope.isTabSwitch(): Boolean = initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

// Tabs fade through (the old one leaves quickly, the new one fades in); opening and closing
// a screen slides it slightly along the horizontal axis, like Material's shared X axis.

fun NavTransitionScope.walletEnterTransition(): EnterTransition =
    if (isTabSwitch()) {
        fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
    } else {
        slideInHorizontally(tween(SLIDE_MILLIS)) { it / SLIDE_OFFSET_DIVISOR } +
            fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
    }

fun NavTransitionScope.walletExitTransition(): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(tween(FADE_OUT_MILLIS))
    } else {
        slideOutHorizontally(tween(SLIDE_MILLIS)) { -it / SLIDE_OFFSET_DIVISOR } + fadeOut(tween(FADE_OUT_MILLIS))
    }

fun NavTransitionScope.walletPopEnterTransition(): EnterTransition =
    if (isTabSwitch()) {
        fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
    } else {
        slideInHorizontally(tween(SLIDE_MILLIS)) { -it / SLIDE_OFFSET_DIVISOR } +
            fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
    }

fun NavTransitionScope.walletPopExitTransition(): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(tween(FADE_OUT_MILLIS))
    } else {
        slideOutHorizontally(tween(SLIDE_MILLIS)) { it / SLIDE_OFFSET_DIVISOR } + fadeOut(tween(FADE_OUT_MILLIS))
    }
