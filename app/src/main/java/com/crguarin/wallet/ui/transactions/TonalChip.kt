package com.crguarin.wallet.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing

private val ChipHeight = 36.dp
private val ChipIconSize = 18.dp

/** Longest a chip label gets at the default font size; it grows with the font scale. */
private val ChipMaxLabelWidth = 220.dp

/**
 * Tonal pill without border, for filters and quick choices. Unselected it rests on
 * `surfaceContainerHigh`; selected it turns `secondaryContainer` and gets a leading check (the
 * state is never told by color alone). It looks 36 dp tall and keeps a 48 dp touch target.
 * [dropdown] adds a trailing arrow for chips that open a list.
 */
@Composable
internal fun TonalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dropdown: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.walletSpacing
    val fade = tween<Color>(durationMillis = MaterialTheme.walletMotion.fast)
    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        animationSpec = fade,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurface,
        animationSpec = fade,
        label = "chipContent",
    )
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier =
                Modifier
                    .height(ChipHeight)
                    .padding(horizontal = spacing.m + spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    // Decorative: the selected state is already announced by the chip.
                    contentDescription = null,
                    modifier = Modifier.size(ChipIconSize),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = ChipMaxLabelWidth * LocalDensity.current.fontScale),
            )
            if (dropdown) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(ChipIconSize),
                )
            }
        }
    }
}
