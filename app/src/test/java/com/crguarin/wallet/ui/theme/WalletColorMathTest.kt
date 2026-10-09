package com.crguarin.wallet.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WalletColorMathTest {
    @Test
    fun `contrast ratio is 21 for black on white, 1 for the same color and symmetric`() {
        assertThat(contrastRatio(Color.Black, Color.White)).isWithin(0.01f).of(21f)
        assertThat(contrastRatio(Color.White, Color.White)).isWithin(0.001f).of(1f)
        val a = Color(0xFF1F5E4E)
        val b = Color(0xFFF4F7F4)
        assertThat(contrastRatio(a, b)).isEqualTo(contrastRatio(b, a))
    }

    @Test
    fun `tintOver goes from the base at 0 to the color at 1`() {
        val color = Color(0xFFEF5350)
        val base = Color(0xFFEEF2EE)

        assertThat(tintOver(color, base, 0f)).isEqualTo(base)
        assertThat(tintOver(color, base, 1f)).isEqualTo(color)
        assertThat(contrastRatio(tintOver(color, base, 0.22f), base))
            .isLessThan(contrastRatio(tintOver(color, base, 0.5f), base))
    }

    @Test
    fun `tonedFor keeps a color that already has the ratio and darkens one that does not`() {
        val dark = Color(0xFF1F5E4E)
        val white = Color.White
        assertThat(tonedFor(dark, white, toward = Color.Black)).isEqualTo(dark)

        val lightRed = Color(0xFFEF5350)
        val tint = tintOver(lightRed, white, 0.22f)
        val toned = tonedFor(lightRed, tint, toward = Color.Black)
        assertThat(toned).isNotEqualTo(lightRed)
        assertThat(contrastRatio(toned, tint)).isAtLeast(4.5f)
    }

    @Test
    fun `tonedFor falls back to black or white when the direction cannot reach the ratio`() {
        // Moving a light color towards white over a white background can never gain contrast.
        val result = tonedFor(Color(0xFFCCCCCC), Color.White, toward = Color.White)

        assertThat(result).isEqualTo(Color.Black)
    }

    @Test
    fun `contentColorOver switches at the real crossover of black and white`() {
        // Relative luminance 0.178 (white wins) and 0.185 (black wins) around the 0.179 crossover.
        assertThat(contentColorOver(Color(0xFF757575))).isEqualTo(Color.White)
        assertThat(contentColorOver(Color(0xFF777777))).isEqualTo(Color.Black)
        assertThat(contentColorOver(Color.White)).isEqualTo(Color.Black)
        assertThat(contentColorOver(Color.Black)).isEqualTo(Color.White)
    }
}
