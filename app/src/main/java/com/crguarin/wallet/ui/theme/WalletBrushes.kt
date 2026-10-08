package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

object WalletBrushes {
    /** Signature gradient of hero cards: primary to tertiary, so it follows dynamic color. Text on it: onPrimary. */
    @Composable
    @ReadOnlyComposable
    fun hero(): Brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))

    /** Card-like gradient from an account color to a darker shade of it. Text on it: [contentColorOver]. */
    fun account(color: Color): Brush = Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.3f)))

    /** Story ring around an account avatar. */
    fun storyRing(color: Color): Brush = Brush.sweepGradient(listOf(color, lerp(color, Color.White, 0.5f), color))

    /** Warm "needs attention" ring (yellow, orange, pink, purple), like an unseen story. */
    val attentionRing: Brush =
        Brush.sweepGradient(
            listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFFFEDA75)),
        )
}

/** Readable content color over [background]: near-black (0xDE000000) when luminance > 0.55, else white. */
fun contentColorOver(background: Color): Color = if (background.luminance() > 0.55f) Color(0xDE000000) else Color.White
