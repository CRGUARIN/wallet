package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
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
import com.crguarin.wallet.ui.theme.contentColorOver

/**
 * Story-style item: a colored circle with a gradient ring, the name below and an optional subtitle.
 */
@Composable
fun StoryAvatar(
    icon: ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ring: Brush = WalletBrushes.storyRing(color),
    avatarSize: Dp = 64.dp,
    itemWidth: Dp = 96.dp,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .width(itemWidth)
                .clip(MaterialTheme.shapes.medium)
                .bounceClick(onClick = onClick)
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(avatarSize)
                    .border(2.5.dp, ring, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColorOver(color),
                modifier = Modifier.size(avatarSize * 0.42f),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

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
            ring = WalletBrushes.attentionRing,
        )
    }
}
