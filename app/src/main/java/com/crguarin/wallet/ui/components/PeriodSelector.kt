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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Month or year selector with a centered [label] between two navigation arrows.
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = previousContentDescription,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
        )
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = nextContentDescription,
            )
        }
    }
}
