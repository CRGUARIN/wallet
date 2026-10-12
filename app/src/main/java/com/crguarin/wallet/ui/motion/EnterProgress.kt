package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.crguarin.wallet.ui.theme.walletMotion
import kotlinx.coroutines.delay

/**
 * Entrance progress towards [target]: from 0 to [target] in `slow` (450 ms) with `emphasized`,
 * once per back-stack entry (keyed by [playKey], with [startDelayMillis] before starting). When
 * [target] changes later, it animates from the current value to the new one. Without animations
 * it returns [target] directly. Read it inside a lambda (for example the `progress` lambda of a
 * progress indicator), never in the body of a composable.
 */
@Composable
fun rememberEnterProgress(
    target: Float,
    playKey: String,
    startDelayMillis: Int = 0,
): State<Float> {
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()
    if (!animate) return rememberUpdatedState(target)
    val played = rememberPlayedOnce("enter:$playKey")
    val progress = remember(playKey) { Animatable(if (played.value) target else 0f) }
    LaunchedEffect(playKey) {
        if (!played.value) {
            played.value = true
            progress.snapTo(0f)
            if (startDelayMillis > 0) delay(startDelayMillis.toLong())
        }
        progress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = motion.slow, easing = motion.emphasized),
        )
    }
    LaunchedEffect(target) {
        if (played.value && progress.value != target) {
            progress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = motion.slow, easing = motion.emphasized),
            )
        }
    }
    return progress.asState()
}
