package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletMotion

/** Content of a disabled keycap, as a fraction of full opacity. */
private const val DISABLED_ALPHA = 0.38f

/**
 * The edge ("canto") under a [face]: the face darkened towards black. Light faces (luminance >
 * 0.4) move 20 % towards black, dark faces 45 %. Pure: the same face always gives the same edge.
 */
fun keycapEdge(face: Color): Color =
    if (face.luminance() > 0.4f) {
        lerp(face, Color.Black, 0.20f)
    } else {
        lerp(face, Color.Black, 0.45f)
    }

/**
 * A key with relief: the face ([face] in [shape]) sits on top, below it the edge (the same shape
 * in [edge], shifted [depth] down) and behind a soft shadow. The total height is face + [depth]
 * and the touch area covers everything, at least 48 dp.
 *
 * On press the face goes down [depth] and covers the edge, in `pressMillis` (100 ms); on release
 * it comes back. It uses the click [interactionSource] and adds no extra scale; the press value
 * is read in `graphicsLayer`, so nothing recomposes per frame. Without animations the change is
 * instant but still there: it is feedback, not decoration. Disabled shows the content at 0.38
 * alpha with a flat face (no edge) and is not clickable.
 */
@Composable
fun Keycap(
    onClick: () -> Unit,
    face: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    edge: Color = keycapEdge(face),
    depth: Dp = MaterialTheme.walletDepth.keycapDepth,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    haptic: HapticFeedbackType? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()
    val dark = MaterialTheme.walletColors.isDark
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val down = pressed && enabled
    val press = remember { Animatable(0f) }
    LaunchedEffect(down, animate) {
        val target = if (down) 1f else 0f
        if (animate) {
            press.animateTo(target, tween(durationMillis = motion.pressMillis))
        } else {
            press.snapTo(target)
        }
    }
    val haptics = LocalHapticFeedback.current
    val shadow =
        remember(dark) {
            Shadow(
                radius = 16.dp,
                color = Color.Black,
                offset = DpOffset(0.dp, 10.dp),
                alpha = if (dark) 0.45f else 0.12f,
            )
        }
    val depthPx = with(LocalDensity.current) { depth.toPx() }

    Box(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .dropShadow(shape, shadow)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onLongClick = onLongClick,
                    onClick = {
                        if (haptic != null) haptics.performHapticFeedback(haptic)
                        onClick()
                    },
                ),
    ) {
        if (enabled) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = depth)
                        .background(edge, shape),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = press.value * depthPx }
                    .padding(bottom = if (enabled) depth else 0.dp)
                    .background(face, shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) {
                content()
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun KeycapPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        KeycapPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun KeycapPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        KeycapPreviewContent()
    }
}

@Composable
private fun KeycapPreviewContent() {
    Column {
        Keycap(
            onClick = {},
            face = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(R.string.action_save))
        }
    }
}
