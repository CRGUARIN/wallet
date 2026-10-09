package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletShapeTokens
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.rememberWalletSwatch
import com.crguarin.wallet.ui.theme.walletColors

/** Share of the tile the icon takes. */
private const val ICON_FRACTION = 0.52f

private val ACCOUNT_RING_WIDTH = 2.dp
private val ATTENTION_RING_WIDTH = 3.dp
private val ATTENTION_BADGE_SIZE = 12.dp
private val ATTENTION_BADGE_BORDER = 2.dp

/** Tiles from this size up leave a wider gap between the ring and the tile. */
private val LARGE_TILE = 56.dp

/**
 * A "pebble": a rounded square (corners at 36% of the side) with an [icon] on a [container]
 * color. This is the plain tile: pass the colors of a transfer (`secondaryContainer` /
 * `onSecondaryContainer`) or of any tonal role. For the user's account and category colors use
 * [PebbleAvatar], which tints them.
 *
 * The icon is decorative (`contentDescription = null`): the row or card that uses the tile already
 * says what it is.
 */
@Composable
fun TonalPebble(
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(WalletShapeTokens.pebble)
                .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * ICON_FRACTION),
        )
    }
}

/**
 * Avatar of an account or category: a pebble tinted with its color ([color] mixed over [base], with
 * the icon in a toned shade that always reads at 4.5:1), so the 30 palette colors do not turn the
 * screen into a rainbow.
 *
 * @param size total size, ring included.
 * @param showRing adds the account ring: 2 dp in the pure color. Decorative.
 * @param needsAttention a card reminder or a credit trip is pending: the ring becomes the warm
 *   3 dp sweep and a 12 dp dot appears at the top-right corner. The dot has a 2 dp border in [base],
 *   which must be the layer the avatar sits on. The state is also conveyed by the text next to the
 *   avatar, never by color alone.
 * @param ring replaces the ring brush (2 dp, drawn even when [showRing] is false); null uses the
 *   pure color, or the attention sweep.
 * @param base layer the tint is mixed over and the dot border matches; `surfaceContainerLow` is the
 *   layer of cards and tonal groups.
 */
@Composable
fun PebbleAvatar(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showRing: Boolean = false,
    needsAttention: Boolean = false,
    ring: Brush? = null,
    base: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val swatch = rememberWalletSwatch(color = color, base = base)
    val ringBrush: Brush? =
        when {
            needsAttention -> WalletBrushes.attentionRing()
            ring != null -> ring
            showRing -> WalletBrushes.storyRing(swatch.ring)
            else -> null
        }
    val ringWidth = if (needsAttention) ATTENTION_RING_WIDTH else ACCOUNT_RING_WIDTH
    val gap = if (size >= LARGE_TILE) 3.dp else 2.dp

    Box(modifier = modifier.size(size)) {
        if (ringBrush == null) {
            TonalPebble(icon = icon, container = swatch.container, content = swatch.content, size = size)
        } else {
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .border(ringWidth, ringBrush, WalletShapeTokens.pebble)
                        .padding(ringWidth + gap),
                contentAlignment = Alignment.Center,
            ) {
                TonalPebble(
                    icon = icon,
                    container = swatch.container,
                    content = swatch.content,
                    size = size - (ringWidth + gap) * 2,
                )
            }
        }
        if (needsAttention) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = ATTENTION_BADGE_BORDER, y = -ATTENTION_BADGE_BORDER)
                        .size(ATTENTION_BADGE_SIZE)
                        .background(MaterialTheme.walletColors.attentionBadge, CircleShape)
                        .border(ATTENTION_BADGE_BORDER, base, CircleShape),
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PebbleAvatarPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        PebbleAvatarPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun PebbleAvatarPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        PebbleAvatarPreviewContent()
    }
}

@Composable
private fun PebbleAvatarPreviewContent() {
    Row(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PebbleAvatar(icon = Icons.Default.AccountBalanceWallet, color = Color(0xFF66BB6A))
        PebbleAvatar(icon = Icons.Default.AccountBalanceWallet, color = Color(0xFF42A5F5), showRing = true)
        PebbleAvatar(icon = Icons.Default.AccountBalanceWallet, color = Color(0xFFEF5350), needsAttention = true)
        PebbleAvatar(icon = Icons.Default.AccountBalanceWallet, color = Color(0xFF42A5F5), size = 60.dp, showRing = true)
        TonalPebble(
            icon = Icons.Default.SwapHoriz,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
