package com.crguarin.wallet.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.crguarin.wallet.ui.theme.walletMotion

/**
 * The shared-transition scope of the navigation host, provided around the `NavHost` (plumbed by
 * the leader). Null in previews and in bare screen tests.
 */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * The `AnimatedVisibilityScope` of the current navigation destination, provided per destination
 * (plumbed by the leader). Null in previews and in bare screen tests.
 */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Shared key of the card of an account between the Home deck and the detail header. */
fun accountCardKey(accountId: Long): String = "account-card-$accountId"

/**
 * Shared bounds between the deck card and the detail header of [accountId]: it flies, it
 * straightens and it becomes the header (and back). Uses short fades to cross the strip and
 * header contents, `tween(slow, easing = emphasized)` for the bounds and `RemeasureToBounds`.
 *
 * Without both scopes, without animations, or in flat mode, it does nothing, so previews, bare
 * screen tests and accessible modes work unchanged.
 */
fun Modifier.accountCardSharedBounds(accountId: Long): Modifier =
    composed {
        val shared = LocalSharedTransitionScope.current
        val nav = LocalNavAnimatedVisibilityScope.current
        val motion = MaterialTheme.walletMotion
        val animate = animationsOn()
        val deep = depthOn()
        if (shared == null || nav == null || !animate || !deep) {
            this
        } else {
            with(shared) {
                val contentState = rememberSharedContentState(accountCardKey(accountId))
                sharedBounds(
                    sharedContentState = contentState,
                    animatedVisibilityScope = nav,
                    enter = fadeIn(tween(durationMillis = motion.fast)),
                    exit = fadeOut(tween(durationMillis = motion.fast)),
                    boundsTransform =
                        BoundsTransform { _, _ ->
                            tween(durationMillis = motion.slow, easing = motion.emphasized)
                        },
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                )
            }
        }
    }

/**
 * Exit/enter of the content around the flying card: it slides [slideY] down and fades in
 * [millis] ms. Only inside an `AnimatedVisibility` (which provides the scope through
 * [LocalNavAnimatedVisibilityScope]); otherwise, without animations, or in flat mode, it does
 * nothing.
 */
fun Modifier.navEnterExit(
    slideY: Dp,
    millis: Int,
): Modifier =
    composed {
        val nav = LocalNavAnimatedVisibilityScope.current
        val animate = animationsOn()
        val deep = depthOn()
        val slidePx = with(LocalDensity.current) { slideY.roundToPx() }
        if (nav == null || !animate || !deep) {
            this
        } else {
            with(nav) {
                animateEnterExit(
                    enter = fadeIn(tween(millis)) + slideInVertically(tween(millis)) { slidePx },
                    exit = fadeOut(tween(millis)) + slideOutVertically(tween(millis)) { slidePx },
                )
            }
        }
    }

/**
 * Tilt of a shared card during its flight: from [restTilt] to 0 over the flight (`slow` with
 * `emphasized`). Without scopes, without animations, or in flat mode, it stays at [restTilt].
 */
@Composable
fun rememberSharedFlightTilt(restTilt: Float): State<Float> {
    val shared = LocalSharedTransitionScope.current
    val nav = LocalNavAnimatedVisibilityScope.current
    val animate = animationsOn()
    val deep = depthOn()
    val motion = MaterialTheme.walletMotion
    if (shared == null || nav == null || !animate || !deep) return rememberUpdatedState(restTilt)
    val tilt = remember(restTilt) { Animatable(restTilt) }
    LaunchedEffect(restTilt) {
        tilt.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = motion.slow, easing = motion.emphasized),
        )
    }
    return tilt.asState()
}
