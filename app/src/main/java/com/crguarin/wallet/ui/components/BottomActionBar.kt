package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.motion.Keycap
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletSpacing

/**
 * Bar pinned to the bottom of a form, for its main action. It rides above the keyboard, so the
 * action stays within reach of the thumb while typing. Put it in the screen Scaffold's `bottomBar`.
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .imePadding()
                    .padding(horizontal = spacing.screenGutter, vertical = spacing.m),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Main action of a form (§9.5): a relieved key in primary (face of 56 dp, keycapDepth edge)
 * that shows a spinner instead of its text while [isLoading]. Same signature and behavior as
 * before: a button with [text] that reports `isEnabled`/`isNotEnabled`.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val depth = MaterialTheme.walletDepth.keycapDepth
    Keycap(
        onClick = onClick,
        face = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp),
        depth = depth,
        enabled = enabled,
        modifier = modifier.height(PRIMARY_FACE_HEIGHT + depth),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private val PRIMARY_FACE_HEIGHT = 56.dp

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun BottomActionBarPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        BottomActionBar {
            PrimaryActionButton(text = "Guardar", onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun BottomActionBarPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        BottomActionBar {
            PrimaryActionButton(text = "Guardar", onClick = {}, modifier = Modifier.weight(1f), isLoading = true)
        }
    }
}
