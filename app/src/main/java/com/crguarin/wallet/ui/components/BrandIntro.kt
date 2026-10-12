package com.crguarin.wallet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.walletColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Size the system splash draws an icon without background at (measured on Android 16 and the
 * core-splashscreen default below Android 12). Matching it makes the hand-off seamless.
 */
private val SPLASH_ICON_SIZE = 288.dp

private val ICON_RISE = 48.dp
private val NAME_OFFSET = 56.dp
private const val RISE_MILLIS = 450
private const val NAME_DELAY_MILLIS = 150
private const val HOLD_MILLIS = 500L
private const val FADE_OUT_MILLIS = 300

/**
 * Brief branded intro shown right after the system splash on a cold start: the splash icon rises,
 * the app name fades in below it and the whole screen fades out to reveal the app.
 *
 * [dynamicColor] must match the splash that was shown (wallpaper colors or the app pine green:
 * `splash_background`, #1E5A4A light and #0F2420 dark), so the hand-off has no color jump. The
 * name is set in Manrope 800 (the theme's displaySmall).
 *
 * [onReveal] is called when the fade-out starts, so the app can begin its entrances underneath;
 * [onFinished] is called when the fade-out ends.
 */
@Composable
fun BrandIntro(
    onFinished: () -> Unit,
    onReveal: () -> Unit = {},
    modifier: Modifier = Modifier,
    dynamicColor: Boolean = false,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnReveal by rememberUpdatedState(onReveal)
    val rise = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { rise.animateTo(1f, tween(RISE_MILLIS, easing = FastOutSlowInEasing)) }
            launch {
                nameAlpha.animateTo(1f, tween(RISE_MILLIS, delayMillis = NAME_DELAY_MILLIS))
            }
        }
        delay(HOLD_MILLIS)
        currentOnReveal()
        screenAlpha.animateTo(0f, tween(FADE_OUT_MILLIS))
        currentOnFinished()
    }

    val density = LocalDensity.current
    val backgroundColor =
        colorResource(if (dynamicColor) R.color.splash_dynamic_background else R.color.splash_background)
    val nameColor =
        if (dynamicColor) colorResource(R.color.splash_dynamic_icon_body) else MaterialTheme.walletColors.onHero
    val icon = if (dynamicColor) R.drawable.ic_splash_dynamic else R.drawable.ic_launcher_foreground
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { alpha = screenAlpha.value }
                .background(backgroundColor)
                // Swallow touches so the screen underneath cannot be tapped while the intro plays.
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            // Decorative: the app name below is read instead.
            contentDescription = null,
            modifier =
                Modifier
                    .size(SPLASH_ICON_SIZE)
                    .graphicsLayer { translationY = -with(density) { ICON_RISE.toPx() } * rise.value },
        )
        Text(
            text = stringResource(R.string.app_name),
            color = nameColor,
            // displaySmall is Manrope 800; the wide tracking is the wordmark's own.
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            modifier =
                Modifier
                    .offset(y = NAME_OFFSET)
                    .graphicsLayer {
                        alpha = nameAlpha.value
                        translationY = with(density) { 12.dp.toPx() } * (1f - nameAlpha.value)
                    },
        )
    }
}

@Preview
@Composable
private fun BrandIntroPreview() {
    BrandIntro(onFinished = {})
}

@Preview(apiLevel = 35)
@Composable
private fun BrandIntroDynamicPreview() {
    BrandIntro(onFinished = {}, dynamicColor = true)
}
