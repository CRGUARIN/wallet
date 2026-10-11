package com.crguarin.wallet.ui.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.crguarin.wallet.ui.theme.WalletPalette
import com.crguarin.wallet.ui.theme.brush
import com.crguarin.wallet.ui.theme.cardFace
import com.crguarin.wallet.ui.theme.contrastRatio
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CardFaceTest {
    private val palette = WalletPalette.colors.map { Color(it) }

    @Test
    fun `content is always white on the 30 palette colors`() {
        palette.forEach { color ->
            assertThat(cardFace(color).content).isEqualTo(Color.White)
        }
    }

    @Test
    fun `white reaches 4_5 over start and end on the 30 palette colors`() {
        palette.forEachIndexed { index, color ->
            val face = cardFace(color)
            assertThat(contrastRatio(face.content, face.start)).isAtLeast(4.5f)
            assertThat(contrastRatio(face.content, face.end)).isAtLeast(4.5f)
        }
    }

    @Test
    fun `start darkens the account color and end darkens start`() {
        palette.forEach { color ->
            val face = cardFace(color)
            assertThat(face.start.luminance()).isAtMost(color.luminance())
            assertThat(face.end.luminance()).isAtMost(face.start.luminance())
        }
    }

    @Test
    fun `a color that already passes keeps itself as start`() {
        val dark = Color(0xFF1B5E20)
        val start = cardFace(dark).start

        assertThat(start.red).isWithin(0.002f).of(dark.red)
        assertThat(start.green).isWithin(0.002f).of(dark.green)
        assertThat(start.blue).isWithin(0.002f).of(dark.blue)
    }

    @Test
    fun `the brush is a diagonal gradient`() {
        val face = cardFace(Color(0xFF3A8C3F))
        assertThat(face.brush()).isNotNull()
    }

    @Test
    fun `keycapEdge darkens towards black`() {
        val face = Color(0xFFFBDDD9)
        val edge = keycapEdge(face)

        assertThat(edge.red).isAtMost(face.red)
        assertThat(edge.green).isAtMost(face.green)
        assertThat(edge.blue).isAtMost(face.blue)
        assertThat(edge.luminance()).isLessThan(face.luminance())
    }

    @Test
    fun `keycapEdge keeps the tone of the face`() {
        val face = Color(0xFF1F5E4E)
        val edge = keycapEdge(face)

        assertThat(dominantChannel(edge)).isEqualTo(dominantChannel(face))
        assertThat(edge.luminance()).isLessThan(face.luminance())
    }

    @Test
    fun `keycapEdge uses the lighter step on light faces`() {
        val light = Color(0xFFFBDDD9)
        val dark = Color(0xFF1B221F)

        assertThat(light.luminance()).isGreaterThan(0.4f)
        assertThat(keycapEdge(light)).isEqualTo(lerp(light, Color.Black, 0.20f))
        assertThat(dark.luminance()).isLessThan(0.4f)
        assertThat(keycapEdge(dark)).isEqualTo(lerp(dark, Color.Black, 0.45f))
    }

    private fun dominantChannel(color: Color): Int {
        val channels = listOf(color.red, color.green, color.blue)
        return channels.indexOf(channels.max())
    }
}
