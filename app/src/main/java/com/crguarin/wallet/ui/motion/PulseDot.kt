package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletMotion

/** How far beyond the dot each ring grows, in dp. */
private const val RING_GROWTH_DP = 10f

/** Alpha of a ring when it is born. */
private const val RING_START_ALPHA = 0.6f

/** Width of the ring stroke, in dp. */
private const val RING_STROKE_DP = 1.5f

/**
 * A solid dot of [size] that, when [pulse] and animations are on, emits `pulseCount` (3) rings:
 * each one grows from 0 to +10 dp while its alpha falls from 0.6 to 0, takes `pulseMillis`
 * (480 ms), and the next one starts after a `pulseGap` (120 ms) pause. Then it stays still: it
 * is finite (one `Animatable`), never infinite. Without animations only the dot is drawn.
 *
 * Decorative, with no semantics: the status text next to it already says it.
 */
@Composable
fun PulseDot(
    color: Color,
    size: Dp,
    pulse: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()
    val progress = remember(pulse, animate) { Animatable(0f) }
    val total = motion.pulseCount * motion.pulseMillis + (motion.pulseCount - 1) * motion.pulseGap
    LaunchedEffect(pulse, animate) {
        if (pulse && animate) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = total, easing = LinearEasing),
            )
        } else {
            progress.snapTo(1f)
        }
    }
    val ringStep = (motion.pulseMillis + motion.pulseGap).toFloat()
    val growthPx = with(LocalDensity.current) { RING_GROWTH_DP.dp.toPx() }
    val strokePx = with(LocalDensity.current) { RING_STROKE_DP.dp.toPx() }
    Box(
        modifier =
            modifier.size(size).drawBehind {
                drawCircle(color = color)
                if (pulse && animate) {
                    val elapsed = progress.value * total
                    repeat(motion.pulseCount) { ring ->
                        val start = ring * ringStep
                        val local = ((elapsed - start) / motion.pulseMillis).coerceIn(0f, 1f)
                        if (local > 0f && local < 1f) {
                            drawCircle(
                                color = color.copy(alpha = RING_START_ALPHA * (1f - local)),
                                radius = size.toPx() / 2f + local * growthPx,
                                center = Offset(size.toPx() / 2f, size.toPx() / 2f),
                                style = Stroke(width = strokePx),
                            )
                        }
                    }
                }
            },
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PulseDotPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        PulseDot(color = MaterialTheme.colorScheme.tertiary, size = 8.dp, pulse = false)
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun PulseDotPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        PulseDot(color = MaterialTheme.colorScheme.tertiary, size = 8.dp, pulse = false)
    }
}
