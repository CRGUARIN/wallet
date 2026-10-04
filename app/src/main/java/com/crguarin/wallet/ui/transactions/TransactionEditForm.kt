package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.util.formatMedium

@Composable
internal fun TransactionEditForm(
    uiState: TransactionEditUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onOpenToAccountSheet: () -> Unit,
    onSwapAccounts: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onInstallmentsChanged: (Int) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!uiState.isEditMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val selectedAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.accountId }?.account
            ?: uiState.archivedAccount
    val selectedToAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.toAccountId }?.account
            ?: uiState.archivedToAccount
    val selectedCategoryText =
        categoryFullDisplayName(
            parent = uiState.selectedParentCategory,
            category = uiState.selectedCategory,
        )
    val isTransfer = uiState.selectedType == TransactionType.TRANSFER

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TRANSACTION_CREATION_TYPES.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = uiState.selectedType == type,
                    onClick = { onTypeSelected(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TRANSACTION_CREATION_TYPES.size),
                    label = { Text(text = stringResource(type.labelRes())) },
                )
            }
        }

        AmountTextField(
            value = uiState.amountText,
            onValueChange = onAmountChanged,
            errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
        )

        if (isTransfer) {
            SelectorField(
                label = stringResource(R.string.transaction_edit_from_label),
                value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenAccountSheet,
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onSwapAccounts,
                    enabled = uiState.accountId != null && uiState.toAccountId != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.transaction_edit_swap_accounts),
                    )
                }
            }

            SelectorField(
                label = stringResource(R.string.transaction_edit_to_label),
                value = selectedToAccount?.name ?: stringResource(R.string.transaction_edit_to_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenToAccountSheet,
            )
        } else {
            SelectorField(
                label = stringResource(R.string.transaction_edit_account_label),
                value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenAccountSheet,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectorField(
                    label = stringResource(R.string.transaction_edit_category_label),
                    value =
                        selectedCategoryText.ifBlank {
                            stringResource(R.string.transaction_edit_category_placeholder)
                        },
                    icon = Icons.Default.Category,
                    isError = uiState.categoryErrorRes != null,
                    onClick = onOpenCategorySheet,
                )
                if (uiState.categoryErrorRes != null) {
                    Text(
                        text = stringResource(uiState.categoryErrorRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        if (uiState.showInstallments) {
            // Local text so the field can be emptied while typing; an empty field means 1.
            var installmentsText by rememberSaveable { mutableStateOf(uiState.installments.toString()) }
            OutlinedTextField(
                value = installmentsText,
                onValueChange = { text ->
                    val digits = text.filter(Char::isDigit)
                    val parsed = digits.toIntOrNull()
                    if (digits.isEmpty() || (parsed != null && parsed in 1..SaveTransactionUseCase.MAX_INSTALLMENTS)) {
                        installmentsText = parsed?.toString().orEmpty()
                        onInstallmentsChanged(parsed ?: 1)
                    }
                },
                label = { Text(text = stringResource(R.string.card_installments_label)) },
                supportingText = { Text(text = stringResource(R.string.card_installments_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onTodayClick) {
                    Text(text = stringResource(R.string.transaction_edit_today))
                }
                TextButton(onClick = onYesterdayClick) {
                    Text(text = stringResource(R.string.transaction_edit_yesterday))
                }
            }
            SelectorField(
                label = stringResource(R.string.transaction_edit_date_label),
                value = uiState.date?.formatMedium().orEmpty(),
                icon = Icons.Default.CalendarMonth,
                isError = uiState.dateErrorRes != null,
                onClick = onOpenDatePicker,
            )
            if (uiState.dateErrorRes != null) {
                Text(
                    text = stringResource(uiState.dateErrorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            label = { Text(text = stringResource(R.string.transaction_edit_note_label)) },
            isError = uiState.noteErrorRes != null,
            minLines = 2,
            maxLines = 4,
            supportingText = {
                Text(
                    text =
                        stringResource(
                            R.string.transaction_edit_note_counter,
                            uiState.note.length,
                            DomainLimits.NOTE_MAX_LENGTH,
                        ),
                    color =
                        if (uiState.noteErrorRes != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.generalErrorRes != null) {
            Text(
                text = stringResource(uiState.generalErrorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = uiState.isSaveEnabled,
            shape = RoundedCornerShape(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.action_save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun SelectorField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }
