package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.walletMotion
import kotlin.math.min

/** Blocks from this index on share the delay of [MAX_INDEX]: the tail enters as a group. */
private const val MAX_INDEX = 4

/** How far below its resting place a block starts, in dp. */
private const val ENTER_SHIFT_DP = 16f

/**
 * The single clock of a screen for staggered entrances: every block with [staggeredEnter] reads
 * the same [elapsed] inside its `graphicsLayer` (a redraw, never a recomposition), so a screen
 * needs no `Animatable` per block.
 */
@Stable
class StaggerClock internal constructor(
    val startDelayMillis: Int,
    val staggerMillis: Int,
    val durationMillis: Int,
    internal val elapsed: Animatable<Float, AnimationVector1D>,
) {
    private val totalMillis: Int get() = startDelayMillis + MAX_INDEX * staggerMillis + durationMillis

    /** When the block at [index] starts: `startDelay + min(index, 4) × stagger`. */
    fun delayFor(index: Int): Int = startDelayMillis + min(index, MAX_INDEX) * staggerMillis

    /**
     * Fraction (0 to 1) of the entrance of the block at [index] at the current clock time. Always
     * 1 when the clock is not playing.
     */
    fun fractionFor(index: Int): Float {
        val local = (elapsed.value - delayFor(index)) / durationMillis.toFloat()
        return local.coerceIn(0f, 1f)
    }
}

/**
 * Remembers the stagger clock of a screen. Each block goes from alpha 0 and +16 dp to its final
 * state in `medium` (220 ms), starting at `startDelay + min(index, 4) × stagger`. It plays once
 * per back-stack entry; without animations, or when it already played, every block is at its
 * final state with no intermediate frame.
 */
@Composable
fun rememberStaggerClock(
    playKey: String,
    startDelayMillis: Int = 0,
): StaggerClock {
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()
    val played = rememberPlayedOnce("stagger:$playKey")
    val total = startDelayMillis + MAX_INDEX * motion.stagger + motion.medium
    val clock =
        remember(playKey) {
            StaggerClock(
                startDelayMillis = startDelayMillis,
                staggerMillis = motion.stagger,
                durationMillis = motion.medium,
                elapsed = Animatable(if (animate && !played.value) 0f else total.toFloat()),
            )
        }
    // The effect stays in the tree unconditionally: removing it when `played` flips would
    // cancel the entrance it just started.
    LaunchedEffect(playKey, animate) {
        if (animate && !played.value) {
            played.value = true
            clock.elapsed.snapTo(0f)
            clock.elapsed.animateTo(
                targetValue = total.toFloat(),
                animationSpec = tween(durationMillis = total, easing = LinearEasing),
            )
        }
    }
    return clock
}

/**
 * Staggered entrance of the block at [index] on [clock]: alpha 0 and +16 dp to the final state.
 * Only the drawing changes, so the node stays in the semantics tree and stays touchable from the
 * very first frame.
 */
fun Modifier.staggeredEnter(
    clock: StaggerClock,
    index: Int,
): Modifier =
    composed {
        val emphasized = MaterialTheme.walletMotion.emphasized
        val shiftPx = with(LocalDensity.current) { ENTER_SHIFT_DP.dp.toPx() }
        graphicsLayer {
            val eased = emphasized.transform(clock.fractionFor(index))
            alpha = eased
            translationY = (1f - eased) * shiftPx
        }
    }
