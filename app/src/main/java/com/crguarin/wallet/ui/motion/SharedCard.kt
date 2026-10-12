package com.crguarin.wallet.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.crguarin.wallet.ui.theme.walletMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

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

/**
 * True while the current navigation is Home to an account detail, or back: the only navigation
 * where an account card flies. Provided by the navigation host; false in previews and bare tests.
 * The shared helpers animate only when it is true, so Home and the detail do not animate on
 * every other visit (Settings, the Accounts tab, a notification).
 */
val LocalCardFlightRoute = compositionLocalOf { false }

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
 * [LocalNavAnimatedVisibilityScope]) and only on a card flight ([LocalCardFlightRoute]); otherwise,
 * without animations, or in flat mode, it does nothing.
 */
fun Modifier.navEnterExit(
    slideY: Dp,
    millis: Int,
): Modifier =
    composed {
        val nav = LocalNavAnimatedVisibilityScope.current
        val flight = LocalCardFlightRoute.current
        val animate = animationsOn()
        val deep = depthOn()
        val slidePx = with(LocalDensity.current) { slideY.roundToPx() }
        if (nav == null || !flight || !animate || !deep) {
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

/** Settle of the hero tilt once the shared flight lands (§7.2.4: about 275 ms). */
private const val HERO_SETTLE_MILLIS = 275

/**
 * Tilt of the flying card, from [restTilt] to 0 over the flight (`slow` with `emphasized`).
 * [flying] is true only for the tapped card while Home exits: at rest, without scopes, without
 * animations, or in flat mode, it stays at [restTilt].
 */
@Composable
fun rememberSharedFlightTilt(
    restTilt: Float,
    flying: Boolean = false,
): State<Float> {
    val shared = LocalSharedTransitionScope.current
    val nav = LocalNavAnimatedVisibilityScope.current
    val animate = animationsOn()
    val deep = depthOn()
    val motion = MaterialTheme.walletMotion
    if (shared == null || nav == null || !animate || !deep || !flying) {
        return rememberUpdatedState(restTilt)
    }
    val tilt = remember(restTilt) { Animatable(restTilt) }
    LaunchedEffect(restTilt) {
        tilt.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = motion.slow, easing = motion.emphasized),
        )
    }
    return tilt.asState()
}

/**
 * Tilt of the landing hero: flat while the card is still flying, then settling to [heroTilt]
 * once it lands (`slow` of delay, [HERO_SETTLE_MILLIS] of motion), once per back-stack entry.
 * Without scopes, outside a card flight ([LocalCardFlightRoute]), without animations, or in flat
 * mode, it is [heroTilt] from the start.
 */
@Composable
fun rememberHeroLandingTilt(heroTilt: Float): State<Float> {
    val shared = LocalSharedTransitionScope.current
    val nav = LocalNavAnimatedVisibilityScope.current
    val flight = LocalCardFlightRoute.current
    val animate = animationsOn()
    val deep = depthOn()
    val motion = MaterialTheme.walletMotion
    if (shared == null || nav == null || !flight || !animate || !deep) return rememberUpdatedState(heroTilt)
    val played = rememberPlayedOnce("hero-landing")
    val tilt = remember(heroTilt) { Animatable(if (played.value) heroTilt else 0f) }
    LaunchedEffect(heroTilt) {
        if (!played.value) {
            played.value = true
            delay(motion.slow.toLong())
            tilt.animateTo(
                targetValue = heroTilt,
                animationSpec = tween(durationMillis = HERO_SETTLE_MILLIS),
            )
        }
    }
    return tilt.asState()
}

/**
 * Mid-flight lift of the landing hero: 1 to [peak] and back over the flight (`slow`), so the
 * card rises a little halfway, once per back-stack entry. Without scopes, outside a card flight
 * ([LocalCardFlightRoute]), without animations, or in flat mode, it is 1. Read it inside
 * `graphicsLayer`, never in the body of a composable.
 */
@Composable
fun rememberFlightLift(peak: Float = 1.04f): State<Float> {
    val shared = LocalSharedTransitionScope.current
    val nav = LocalNavAnimatedVisibilityScope.current
    val flight = LocalCardFlightRoute.current
    val animate = animationsOn()
    val deep = depthOn()
    val motion = MaterialTheme.walletMotion
    if (shared == null || nav == null || !flight || !animate || !deep) return rememberUpdatedState(1f)
    val played = rememberPlayedOnce("flight-lift")
    val progress = remember { Animatable(if (played.value) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played.value) {
            played.value = true
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = motion.slow, easing = LinearEasing),
            )
        }
    }
    return remember(progress) {
        derivedStateOf { 1f + (peak - 1f) * sin(progress.value * PI).toFloat() }
    }
}
