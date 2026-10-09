package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.theme.walletSpacing

/**
 * All filters in one row that scrolls sideways: All, Expense, Income, Transfer, Account and
 * Category (these two open a list), plus "Clear" while any filter is on. Expense comes before
 * Income, as in the movement form.
 */
@Composable
internal fun FilterRow(
    uiState: TransactionsUiState,
    onTypeSelected: (TransactionType?) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    val categoryLabel =
        categoryFullDisplayName(
            parent = uiState.selectedParentCategory,
            category = uiState.selectedCategory,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TonalChip(
            label = stringResource(R.string.transactions_filter_all_types),
            selected = uiState.type == null,
            onClick = { onTypeSelected(null) },
        )
        TRANSACTION_CREATION_TYPES.forEach { type ->
            TonalChip(
                label = stringResource(type.labelRes()),
                selected = uiState.type == type,
                onClick = { onTypeSelected(type) },
            )
        }
        TonalChip(
            label = uiState.selectedAccountName ?: stringResource(R.string.transactions_filter_account),
            selected = uiState.accountId != null,
            onClick = onOpenAccountSheet,
            dropdown = true,
        )
        TonalChip(
            label = categoryLabel.ifBlank { stringResource(R.string.transactions_filter_category) },
            selected = uiState.categoryId != null,
            onClick = onOpenCategorySheet,
            dropdown = true,
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
