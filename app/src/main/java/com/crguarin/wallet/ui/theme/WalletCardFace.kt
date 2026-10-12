@file:Suppress("ktlint:standard:filename")

package com.crguarin.wallet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** How far towards black each step of the card-face darkening moves, as a fraction. */
private const val FACE_DARKEN_STEP = 0.04f

/**
 * The face of an account card, like a physical card: gradient background with white content.
 * Pure and JVM-testable: [cardFace] derives it from the account color.
 */
@Immutable
data class CardFace(
    val start: Color,
    val end: Color,
    val content: Color,
)

/**
 * The face for an account [color]: `content` is always white, `start` is the account color
 * darkened in 4 % steps towards black until white reaches [SAFE_TEXT_CONTRAST] over it (light
 * colors such as yellows end up much darker, on purpose), and `end` is `start` darkened 45 % more.
 */
fun cardFace(color: Color): CardFace {
    var start = color
    for (step in 0..25) {
        val candidate = lerp(color, Color.Black, step * FACE_DARKEN_STEP)
        if (contrastRatio(Color.White, candidate) >= SAFE_TEXT_CONTRAST) {
            start = candidate
            break
        }
        start = candidate
    }
    return CardFace(
        start = start,
        end = lerp(start, Color.Black, 0.45f),
        content = Color.White,
    )
}

/** Linear gradient from the top-left to the bottom-right (135 degrees). */
fun CardFace.brush(): Brush =
    Brush.linearGradient(
        colors = listOf(start, end),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
