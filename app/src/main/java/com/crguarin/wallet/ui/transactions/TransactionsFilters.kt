package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.categoryFullDisplayName

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterRow(
    uiState: TransactionsUiState,
    onTypeSelected: (TransactionType?) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val categoryLabel =
        categoryFullDisplayName(
            parent = uiState.selectedParentCategory,
            category = uiState.selectedCategory,
        )

    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = uiState.type == null,
            onClick = { onTypeSelected(null) },
            label = { Text(text = stringResource(R.string.transactions_filter_all_types)) },
        )
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = uiState.type == type,
                onClick = { onTypeSelected(type) },
                label = { Text(text = stringResource(type.labelRes())) },
            )
        }
        FilterChip(
            selected = uiState.accountId != null,
            onClick = onOpenAccountSheet,
            label = {
                Text(
                    text = uiState.selectedAccountName ?: stringResource(R.string.transactions_filter_account),
                )
            },
        )
        FilterChip(
            selected = uiState.categoryId != null,
            onClick = onOpenCategorySheet,
            label = {
                Text(
                    text = categoryLabel.ifBlank { stringResource(R.string.transactions_filter_category) },
                )
            },
        )
        if (uiState.hasActiveFilters) {
            TextButton(onClick = onClearFilters) {
                Text(text = stringResource(R.string.transactions_clear_filters))
            }
        }
    }
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }
