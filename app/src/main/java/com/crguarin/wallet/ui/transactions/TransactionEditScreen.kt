package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TransactionEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCategoryEdit: (CategoryType) -> Unit = {},
    onNavigateToAccountEdit: () -> Unit = {},
    viewModel: TransactionEditViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        TransactionEditScreen(
            uiState = inspectionUiState(),
            onNavigateBack = onNavigateBack,
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onTodayClick = {},
            onYesterdayClick = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionEditEvent.NavigateBack -> onNavigateBack()
                is TransactionEditEvent.OpenCategoryEdit -> onNavigateToCategoryEdit(event.type)
                is TransactionEditEvent.OpenAccountEdit -> onNavigateToAccountEdit()
            }
        }
    }

    TransactionEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTypeSelected = viewModel::setType,
        onAmountChanged = viewModel::setAmountText,
        onOpenAccountSheet = viewModel::openAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelected = viewModel::setAccountId,
        onOpenToAccountSheet = viewModel::openToAccountSheet,
        onDismissToAccountSheet = viewModel::dismissToAccountSheet,
        onToAccountSelected = viewModel::setToAccountId,
        onSwapAccounts = viewModel::swapAccounts,
        onOpenCategorySheet = viewModel::openCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onCategorySelected = viewModel::setCategoryId,
        onNewCategoryClick = viewModel::onNewCategoryClick,
        onNewAccountClick = viewModel::onNewAccountClick,
        onOpenDatePicker = viewModel::openDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onDateSelected = viewModel::setDate,
        onTodayClick = viewModel::setToday,
        onYesterdayClick = viewModel::setYesterday,
        onNoteChanged = viewModel::setNote,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onConfirmNegativeBalance = viewModel::confirmNegativeBalanceSave,
        onDismissNegativeBalance = viewModel::dismissNegativeBalanceDialog,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Representative state used when there is no Hilt component (previews and navigation tests),
 * so the editor layout is still visible instead of an endless loading state.
 */
private fun inspectionUiState(): TransactionEditUiState =
    TransactionEditUiState(
        isLoading = false,
        date = LocalDate.now(),
        accounts =
            listOf(
                AccountWithBalance(
                    account =
                        Account(
                            id = 1L,
                            name = "Efectivo",
                            type = AccountType.CASH,
                            initialBalance = Money.ZERO,
                            color = 0xFF42A5F5.toInt(),
                            icon = "wallet",
                            createdAt = Instant.EPOCH,
                        ),
                    balance = Money.ZERO,
                ),
                AccountWithBalance(
                    account =
                        Account(
                            id = 2L,
                            name = "Ahorros",
                            type = AccountType.SAVINGS,
                            initialBalance = Money.ZERO,
                            color = 0xFF66BB6A.toInt(),
                            icon = "savings",
                            sortOrder = 1,
                            createdAt = Instant.EPOCH,
                        ),
                    balance = Money.ZERO,
                ),
            ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    uiState: TransactionEditUiState,
    onNavigateBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelected: (Long) -> Unit,
    onOpenToAccountSheet: () -> Unit,
    onDismissToAccountSheet: () -> Unit,
    onToAccountSelected: (Long) -> Unit,
    onSwapAccounts: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    onNewCategoryClick: () -> Unit,
    onNewAccountClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmNegativeBalance: () -> Unit,
    onDismissNegativeBalance: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            if (uiState.isEditMode) {
                                stringResource(R.string.transaction_edit_title)
                            } else {
                                stringResource(R.string.transaction_edit_new_title)
                            },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_transaction),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.showTransferNeedsAccounts -> {
                    EmptyState(
                        icon = Icons.Default.SwapHoriz,
                        title = stringResource(R.string.transaction_edit_transfer_needs_accounts_title),
                        message = stringResource(R.string.transaction_edit_transfer_needs_accounts_message),
                        actionLabel = stringResource(R.string.action_new_account),
                        onActionClick = onNewAccountClick,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.showNoAccounts -> {
                    EmptyState(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = stringResource(R.string.transaction_edit_no_accounts_title),
                        message = stringResource(R.string.transaction_edit_no_accounts_message),
                        actionLabel = stringResource(R.string.action_new_account),
                        onActionClick = onNewAccountClick,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    TransactionEditForm(
                        uiState = uiState,
                        onTypeSelected = onTypeSelected,
                        onAmountChanged = onAmountChanged,
                        onOpenAccountSheet = onOpenAccountSheet,
                        onOpenToAccountSheet = onOpenToAccountSheet,
                        onSwapAccounts = onSwapAccounts,
                        onOpenCategorySheet = onOpenCategorySheet,
                        onOpenDatePicker = onOpenDatePicker,
                        onTodayClick = onTodayClick,
                        onYesterdayClick = onYesterdayClick,
                        onNoteChanged = onNoteChanged,
                        onSaveClick = onSaveClick,
                    )
                }
            }
        }
    }

    if (uiState.showAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts.filter { it.account.id != uiState.toAccountId },
            selectedAccountId = uiState.accountId,
            onAccountSelected = { onAccountSelected(it.account.id) },
            onDismissRequest = onDismissAccountSheet,
        )
    }

    if (uiState.showToAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts.filter { it.account.id != uiState.accountId },
            selectedAccountId = uiState.toAccountId,
            onAccountSelected = { onToAccountSelected(it.account.id) },
            onDismissRequest = onDismissToAccountSheet,
        )
    }

    if (uiState.showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categoryNodes,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { onCategorySelected(it.id) },
            onNewCategoryClick = onNewCategoryClick,
            onDismissRequest = onDismissCategorySheet,
        )
    }

    if (uiState.showDatePicker && uiState.date != null) {
        TransactionDatePickerDialog(
            initialDate = uiState.date,
            onDismiss = onDismissDatePicker,
            onDateSelected = onDateSelected,
        )
    }

    if (uiState.showNegativeBalanceDialog) {
        ConfirmDialog(
            title = stringResource(R.string.transaction_edit_negative_balance_title),
            message = stringResource(R.string.transaction_edit_negative_balance_message),
            onConfirm = onConfirmNegativeBalance,
            onDismiss = onDismissNegativeBalance,
        )
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.transaction_edit_delete_title),
            message = stringResource(R.string.transaction_edit_delete_message),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditForm(
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
private fun SelectorField(
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
@Composable
private fun TransactionDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }

@Preview(name = "New expense Light", showBackground = true)
@Composable
private fun TransactionEditScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        TransactionEditScreen(
            uiState = inspectionUiState().copy(amountText = "125000"),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onTodayClick = {},
            onYesterdayClick = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}

@Preview(name = "New transfer Dark", showBackground = true)
@Composable
private fun TransactionEditScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        TransactionEditScreen(
            uiState =
                inspectionUiState().copy(
                    selectedType = TransactionType.TRANSFER,
                    amountText = "125000",
                    accountId = 1L,
                    toAccountId = 2L,
                ),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onTodayClick = {},
            onYesterdayClick = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}
