package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing

/** Alpha of the arrow when it cannot be used (there is no previous or next period). */
private const val DISABLED_ARROW_ALPHA = 0.38f

/**
 * Month or year selector with a centered [label] between two navigation arrows, each one on a
 * `surfaceContainerLow` circle.
 *
 * The caller formats [label] and resolves both content descriptions so the same component can be
 * reused by Movements (month) and Reports (month or year).
 */
@Composable
fun PeriodSelector(
    label: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val arrowColors =
        IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ARROW_ALPHA),
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.walletSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            colors = arrowColors,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = previousContentDescription,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.walletSpacing.s)
                    .semantics { heading() },
        )
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            colors = arrowColors,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = nextContentDescription,
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PeriodSelectorPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        PeriodSelector(
            label = "Octubre 2026",
            canGoPrevious = true,
            canGoNext = false,
            onPrevious = {},
            onNext = {},
            previousContentDescription = "Mes anterior",
            nextContentDescription = "Mes siguiente",
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun PeriodSelectorPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        PeriodSelector(
            label = "Octubre 2026",
            canGoPrevious = true,
            canGoNext = true,
            onPrevious = {},
            onNext = {},
            previousContentDescription = "Mes anterior",
            nextContentDescription = "Mes siguiente",
        )
    }
}
