package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth and relief distances of the 3D direction: deck geometry, deal, key relief and parallax.
 * Only used when the motion policy allows depth; otherwise the flat layout of today is shown.
 */
@Immutable
data class WalletDepth(
    /** Backward tilt of each deck card, in degrees. */
    val deckTilt: Float = 24f,
    /** Visible strip of each card behind the front one. */
    val deckStrip: Dp = 54.dp,
    /** Height of a deck card. */
    val deckCardHeight: Dp = 190.dp,
    /** Corner radius of a deck card. */
    val deckCardRadius: Dp = 22.dp,
    /** Density multiplier for `graphicsLayer.cameraDistance` on the deck. */
    val deckCameraDistance: Float = 12f,
    /** Resting tilt of the detail hero card, in degrees. */
    val heroTilt: Float = 8f,
    /** Minimum height of the detail hero card. */
    val heroCardHeight: Dp = 190.dp,
    /** Height of the floor shadow under the hero card. */
    val heroFloorShadow: Dp = 18.dp,
    /** Floating amplitude of the deck and the hero card. */
    val floatAmplitude: Dp = 6.dp,
    /** How far below the dealt cards start. */
    val dealOffsetY: Dp = 90.dp,
    /** Extra backward tilt of the dealt cards at the start, in degrees. */
    val dealExtraTilt: Float = 30f,
    /** Starting scale of the dealt cards. */
    val dealStartScale: Float = 1.08f,
    /** How far the other cards go down when one flies to the detail. */
    val exitOffsetY: Dp = 90.dp,
    /** How far the rest of Home shifts when a card flies to the detail. */
    val contentShiftY: Dp = 16.dp,
    /** Relief of quick actions, detail actions and Save. */
    val keycapDepth: Dp = 5.dp,
    /** Relief of the numeric keypad keys. */
    val keyDepth: Dp = 4.dp,
    /** Maximum gyroscope parallax tilt, in degrees. */
    val parallaxMax: Float = 12f,
)

val DefaultWalletDepth = WalletDepth()

val LocalWalletDepth = staticCompositionLocalOf { DefaultWalletDepth }

val MaterialTheme.walletDepth: WalletDepth
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletDepth.current
