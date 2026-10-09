package com.crguarin.wallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Angle of the hero gradient, in degrees clockwise from "up" (CSS convention): top-left to bottom-right. */
private const val HERO_ANGLE_DEGREES = 145f

object WalletBrushes {
    /**
     * Gradient of the balance card at about 145 degrees, from `walletColors.heroStart` to `heroEnd`.
     * Text on it: `walletColors.onHero`.
     */
    @Composable
    @ReadOnlyComposable
    fun hero(): Brush {
        val colors = MaterialTheme.walletColors
        return AngledLinearGradient(listOf(colors.heroStart, colors.heroEnd), HERO_ANGLE_DEGREES)
    }

    /** Account ring: a solid pure color. Draw it 2 dp wide, it is decorative. */
    fun storyRing(color: Color): Brush = SolidColor(color)

    /**
     * Ring of an avatar that needs attention (card reminder, credit trip): a sweep from the
     * attention color to amber and back. Draw it 3 dp wide, plus a 12 dp `walletColors.attentionBadge`
     * dot at the top right with a 2 dp border in the surface color.
     */
    @Composable
    @ReadOnlyComposable
    fun attentionRing(): Brush {
        val colors = MaterialTheme.walletColors
        return Brush.sweepGradient(listOf(colors.attentionStart, colors.attentionMid, colors.attentionStart))
    }

    @Deprecated(
        message = "Not theme aware (it was a fixed Instagram-like sweep). Use attentionRing().",
        replaceWith = ReplaceWith("attentionRing()"),
    )
    val attentionRing: Brush =
        Brush.sweepGradient(
            listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFFFEDA75)),
        )

    @Deprecated(
        message = "The account header is now a tinted layer: use rememberWalletSwatch(color).container with onSurface text.",
    )
    fun account(color: Color): Brush = Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.3f)))
}

/**
 * Linear gradient at [angleDegrees] (clockwise from "up", like CSS `linear-gradient`) whose line
 * crosses the whole box, so the corners get exactly the first and last color on any aspect ratio.
 */
private class AngledLinearGradient(
    private val colors: List<Color>,
    private val angleDegrees: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val direction = Offset(dx * halfLength, dy * halfLength)
        return LinearGradientShader(from = center - direction, to = center + direction, colors = colors)
    }

    override fun equals(other: Any?): Boolean =
        other is AngledLinearGradient && other.colors == colors && other.angleDegrees == angleDegrees

    override fun hashCode(): Int = 31 * colors.hashCode() + angleDegrees.hashCode()
}
