package com.crguarin.wallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.crguarin.wallet.ui.theme.walletMotion

/**
 * Clickable that shrinks slightly while pressed (`walletMotion.pressScale` in
 * `walletMotion.pressMillis`) for a soft press feedback. With the system "remove animations"
 * setting the scale changes at once.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier =
    composed {
        val motion = MaterialTheme.walletMotion
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) motion.pressScale else 1f,
            animationSpec = tween(durationMillis = motion.pressMillis),
            label = "bounceClick",
        )
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
    }
