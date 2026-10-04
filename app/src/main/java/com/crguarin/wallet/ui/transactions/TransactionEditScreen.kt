package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.theme.WalletTheme
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
            onInstallmentsChanged = {},
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
        onInstallmentsChanged = viewModel::setInstallments,
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
    onInstallmentsChanged: (Int) -> Unit = {},
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
                    LoadingState()
                }

                uiState.notFound -> {
                    ErrorState(
                        message = stringResource(R.string.transaction_edit_not_found),
                        actionLabel = stringResource(R.string.action_back),
                        onActionClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.Center),
                    )
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
                        onInstallmentsChanged = onInstallmentsChanged,
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
            cardOverviews = uiState.cardOverviews,
            today = uiState.today,
            onDismissRequest = onDismissAccountSheet,
        )
    }

    if (uiState.showToAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts.filter { it.account.id != uiState.accountId },
            selectedAccountId = uiState.toAccountId,
            onAccountSelected = { onToAccountSelected(it.account.id) },
            cardOverviews = uiState.cardOverviews,
            today = uiState.today,
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
        val isOverLimit = uiState.balanceWarning == BalanceWarning.OVER_CREDIT_LIMIT
        ConfirmDialog(
            title =
                stringResource(
                    if (isOverLimit) {
                        R.string.card_over_limit_title
                    } else {
                        R.string.transaction_edit_negative_balance_title
                    },
                ),
            message =
                stringResource(
                    if (isOverLimit) {
                        R.string.card_over_limit_message
                    } else {
                        R.string.transaction_edit_negative_balance_message
                    },
                ),
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
            onInstallmentsChanged = {},
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
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}
