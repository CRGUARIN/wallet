package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.crguarin.wallet.ui.theme.walletMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * True only while the bounded idle loops (floating, sheen) may run: animations are on, the intro
 * is finished, and less than `idleLoopWindow` (10 s) has passed since this screen showed. It
 * turns false forever in this entry when [interrupted] (for example the list scrolled), also when
 * that is already true at the start, and it never starts again once its window has closed.
 */
@Composable
fun rememberIdleLoop(
    playKey: String,
    interrupted: Boolean,
): State<Boolean> {
    val animate = animationsOn()
    val introFinished = LocalIntroFinished.current
    val window = MaterialTheme.walletMotion.idleLoopWindow
    val running = remember(playKey) { mutableStateOf(false) }
    // Set for good once the loop is interrupted or its window closes: this entry never restarts it.
    val spent = remember(playKey) { mutableStateOf(false) }
    LaunchedEffect(interrupted) {
        if (interrupted) {
            spent.value = true
            running.value = false
        }
    }
    LaunchedEffect(playKey, animate, introFinished, interrupted) {
        val mayRun = animate && introFinished && !interrupted && !spent.value
        running.value = mayRun
        if (mayRun) {
            delay(window.toLong())
            running.value = false
            spent.value = true
        }
    }
    return running
}

/**
 * Gentle floating: `translationY` goes 0 → −[amplitude] → 0 every `floatPeriod`, with ease-in-out,
 * only while [running]. When it stops it eases back to 0 with no jump. Reads the animated value
 * in `graphicsLayer`, so nothing recomposes per frame.
 */
fun Modifier.floating(
    running: () -> Boolean,
    amplitude: Dp,
): Modifier =
    composed {
        val motion = MaterialTheme.walletMotion
        val run = running()
        val amplitudePx = with(LocalDensity.current) { amplitude.toPx() }
        val phase = remember { Animatable(0f) }
        LaunchedEffect(run) {
            if (run) {
                phase.snapTo(0f)
                phase.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = motion.floatPeriod, easing = LinearEasing),
                        ),
                )
            } else {
                phase.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = motion.medium),
                )
            }
        }
        graphicsLayer {
            translationY = -sin(phase.value * PI).toFloat() * amplitudePx
        }
    }

/**
 * Diagonal shine band sweeping the card: transparent at 35 %, white at [peakAlpha] in the middle,
 * transparent at 65 %. The brush is 2.6 times the width and sweeps right to left and back every
 * `sheenPeriod`. The caller clips to the card shape (this modifier only paints inside its own
 * bounds). Decorative: no semantics.
 */
fun Modifier.sheen(
    running: () -> Boolean,
    angle: Float = 105f,
    peakAlpha: Float = 0.16f,
): Modifier =
    composed {
        val motion = MaterialTheme.walletMotion
        val run = running()
        val sweep = remember { Animatable(0f) }
        LaunchedEffect(run) {
            if (run) {
                sweep.snapTo(0f)
                sweep.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = motion.sheenPeriod, easing = LinearEasing),
                        ),
                )
            } else {
                // Both ends of the sweep park the band fully outside: settle at the nearest one.
                sweep.animateTo(
                    targetValue = if (sweep.value < 0.5f) 0f else 1f,
                    animationSpec = tween(durationMillis = motion.medium),
                )
            }
        }
        // The sweep parks the band fully outside at both ends (triangle 0 and 1), so settling
        // at the nearest end is invisible. The angle tilts the band: 90 is vertical, higher
        // angles lean it towards the top-left.
        drawWithCache {
            val tilt = (angle - 90f) * size.height / 180f
            val bandWidth = size.width * SHEEN_BRUSH_WIDTH
            val brush =
                Brush.linearGradient(
                    0.35f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = peakAlpha),
                    0.65f to Color.Transparent,
                    start = Offset(0f, size.height),
                    end = Offset(bandWidth, tilt),
                )
            onDrawWithContent {
                drawContent()
                val travel = size.width + bandWidth
                val triangle = if (sweep.value < 0.5f) sweep.value * 2f else 2f - sweep.value * 2f
                if (triangle > 0f && triangle < 1f) {
                    translate(left = size.width - triangle * travel) {
                        drawRect(brush = brush)
                    }
                }
            }
        }
    }

/** How many times wider than the card the sheen brush is. */
private const val SHEEN_BRUSH_WIDTH = 2.6f
