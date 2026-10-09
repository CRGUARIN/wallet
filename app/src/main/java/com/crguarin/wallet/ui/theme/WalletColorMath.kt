package com.crguarin.wallet.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

// Pure color helpers (no Compose runtime): testable on the JVM. All of them assume opaque colors.

/** Smallest contrast WCAG AA asks for normal text. */
const val MIN_TEXT_CONTRAST = 4.5f

/**
 * Target for colors the theme derives at run time. A little above [MIN_TEXT_CONTRAST] so the
 * rounding of real 8-bit pixels cannot take the result below 4.5:1.
 */
const val SAFE_TEXT_CONTRAST = 4.6f

/** Smallest contrast WCAG AA asks for large text and interface components. */
const val MIN_UI_CONTRAST = 3f

/** WCAG contrast ratio between [a] and [b], from 1 (identical) to 21 (black on white). */
fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** [color] mixed over [base]: `fraction` 0 is [base], 1 is [color]. */
fun tintOver(
    color: Color,
    base: Color,
    fraction: Float,
): Color = lerp(base, color, fraction)

/**
 * [color] moved towards [toward] (black on light backgrounds, white on dark ones) in steps of 2%
 * until it reaches [minRatio] against [background]. If no step gets there, the black or white with
 * the most contrast is returned, so the result always meets the ratio a black or white can meet.
 */
fun tonedFor(
    color: Color,
    background: Color,
    toward: Color,
    minRatio: Float = MIN_TEXT_CONTRAST,
): Color {
    for (step in 0..TONE_STEPS) {
        val candidate = lerp(color, toward, step.toFloat() / TONE_STEPS)
        if (contrastRatio(candidate, background) >= minRatio) return candidate
    }
    return contentColorOver(background)
}

private const val TONE_STEPS = 50

/**
 * Readable content color over [background]: opaque black or white, whichever has more contrast
 * (the real crossover is at a relative luminance of about 0.179). It reaches at least 4.58:1 over
 * any color.
 */
fun contentColorOver(background: Color): Color =
    if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) {
        Color.Black
    } else {
        Color.White
    }
