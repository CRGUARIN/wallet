package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing

/**
 * Story-style item: a tinted pebble (rounded square) with a solid 2 dp ring in the pure color, the
 * name below and an optional subtitle.
 *
 * @param needsAttention a card reminder or a credit trip is pending: the ring becomes the warm 3 dp
 *   sweep (`WalletBrushes.attentionRing()`) and a 12 dp dot marks the corner. The caller still says
 *   it in words (subtitle), since color alone never carries the meaning.
 * @param ring brush of the ring when [needsAttention] is false; the default is the pure color.
 */
@Composable
fun StoryAvatar(
    icon: ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ring: Brush = WalletBrushes.storyRing(color),
    avatarSize: Dp = 60.dp,
    itemWidth: Dp = 96.dp,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
    needsAttention: Boolean = false,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(
        modifier =
            modifier
                .width(itemWidth)
                .clip(MaterialTheme.shapes.medium)
                .bounceClick(onClick = onClick)
                .padding(vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PebbleAvatar(
            icon = icon,
            color = color,
            size = avatarSize,
            showRing = true,
            needsAttention = needsAttention,
            ring = ring,
        )

        Spacer(modifier = Modifier.height(spacing.s))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        subtitle?.invoke(this)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun StoryAvatarPreviewLight() {
    WalletTheme(darkTheme = false) {
        StoryAvatar(
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF66BB6A),
            label = "Efectivo",
            onClick = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun StoryAvatarPreviewDark() {
    WalletTheme(darkTheme = true) {
        StoryAvatar(
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF42A5F5),
            label = "Ahorros",
            onClick = {},
            needsAttention = true,
        )
    }
}
