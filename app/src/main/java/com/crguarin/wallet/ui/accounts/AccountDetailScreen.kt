package com.crguarin.wallet.ui.accounts

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.TransactionDayHeader
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.transactions.rememberEntriesPagingItems
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AccountDetailRoute(
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onTransferClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        AccountDetailScreen(
            uiState = AccountDetailUiState(isLoading = false),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = onNavigateBack,
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = onTransactionClick,
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountDetailEvent.NavigateBack -> onNavigateBack()
                is AccountDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    AccountDetailScreen(
        uiState = uiState,
        pagingItems = pagingItems,
        onNavigateBack = onNavigateBack,
        onEditClick = { onEditClick(viewModel.accountId) },
        onTransferClick = { onTransferClick(viewModel.accountId) },
        onTransactionClick = onTransactionClick,
        onViewAllTransactions = { onViewAllTransactions(viewModel.accountId) },
        onArchiveClick = viewModel::archiveAccount,
        onUnarchiveClick = viewModel::unarchiveAccount,
        onDeleteClick = viewModel::showDeleteDialog,
        onConfirmDelete = viewModel::deleteAccount,
        onConfirmArchiveInsteadOfDelete = viewModel::confirmArchiveInsteadOfDelete,
        onDismissDialogs = viewModel::dismissDialogs,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    uiState: AccountDetailUiState,
    pagingItems: LazyPagingItems<TransactionListEntry>,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onTransferClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmArchiveInsteadOfDelete: () -> Unit,
    onDismissDialogs: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val account = uiState.accountWithBalance?.account

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.account_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (account != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account_detail_menu_edit)) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                            )
                            if (!account.isArchived) {
                                if (uiState.canTransfer) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.account_detail_menu_transfer)) },
                                        onClick = {
                                            menuExpanded = false
                                            onTransferClick()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.account_detail_menu_archive)) },
                                    onClick = {
                                        menuExpanded = false
                                        onArchiveClick()
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.account_detail_menu_unarchive)) },
                                    onClick = {
                                        menuExpanded = false
                                        onUnarchiveClick()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.account_detail_menu_delete),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (account == null) {
            // The account was deleted (or the id never existed): explain it and let the user leave.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = stringResource(R.string.account_detail_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (account != null) {
                val balance = uiState.accountWithBalance.balance
                item(key = "header") {
                    AccountHeaderCard(
                        account = account,
                        balance = balance,
                        monthSummary = uiState.monthSummary,
                    )
                }
            }

            item(key = "transactions_header") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.account_detail_transactions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics { heading() },
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }

            val isLoading =
                pagingItems.loadState.refresh is LoadState.Loading &&
                    pagingItems.itemCount == 0

            val refreshError = pagingItems.loadState.refresh as? LoadState.Error

            when {
                isLoading -> {
                    item(key = "transactions_loading") {
                        LoadingState(fillMaxSize = false, modifier = Modifier.fillMaxWidth())
                    }
                }

                refreshError != null && pagingItems.itemCount == 0 -> {
                    item(key = "transactions_error") {
                        ErrorState(
                            message = stringResource(R.string.transactions_load_error),
                            actionLabel = stringResource(R.string.action_retry),
                            onActionClick = { pagingItems.retry() },
                        )
                    }
                }

                pagingItems.itemCount == 0 -> {
                    item(key = "transactions_empty") {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = stringResource(R.string.account_detail_transactions_empty_title),
                            message = stringResource(R.string.account_detail_transactions_empty_message),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    items(
                        count = pagingItems.itemCount,
                        key =
                            pagingItems.itemKey { entry ->
                                when (entry) {
                                    is TransactionListEntry.DayHeader -> "day_header_${entry.date.toEpochDay()}"
                                    is TransactionListEntry.Item -> entry.details.transaction.id
                                }
                            },
                        contentType =
                            pagingItems.itemContentType { entry ->
                                when (entry) {
                                    is TransactionListEntry.DayHeader -> "day_header"
                                    is TransactionListEntry.Item -> "transaction_item"
                                }
                            },
                    ) { index ->
                        when (val entry = pagingItems[index]) {
                            is TransactionListEntry.DayHeader -> {
                                TransactionDayHeader(
                                    date = entry.date,
                                    net = uiState.dailyTotals[entry.date] ?: Money.ZERO,
                                    today = uiState.today,
                                )
                            }
                            is TransactionListEntry.Item -> {
                                TransactionListItem(
                                    details = entry.details,
                                    perspectiveAccountId = account?.id,
                                    onClick = { onTransactionClick(entry.details.transaction.id) },
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Delete Confirm Dialog
    if (uiState.showDeleteConfirmDialog && account != null) {
        ConfirmDialog(
            title = stringResource(R.string.account_detail_delete_title),
            message = stringResource(R.string.account_detail_delete_message, account.name),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDialogs,
        )
    }

    // Offer Archive Dialog (when deleting failed because account has transactions)
    if (uiState.showOfferArchiveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.account_detail_cannot_delete_has_transactions_title),
            message = stringResource(R.string.account_detail_cannot_delete_has_transactions_message),
            confirmText = stringResource(R.string.action_archive),
            onConfirm = onConfirmArchiveInsteadOfDelete,
            onDismiss = onDismissDialogs,
        )
    }
}

@Preview(name = "Account Detail Light", showBackground = true)
@Composable
private fun AccountDetailPreviewLight() {
    WalletTheme(darkTheme = false) {
        val sampleAccount =
            Account(
                id = 1L,
                name = "Efectivo",
                type = AccountType.CASH,
                currencyCode = APP_CURRENCY,
                initialBalance = Money(150_000L),
                color = -16711936,
                icon = "wallet",
                includeInTotal = true,
                isArchived = false,
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        AccountDetailScreen(
            uiState =
                AccountDetailUiState(
                    accountWithBalance = AccountWithBalance(sampleAccount, Money(450_000L)),
                    monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L)),
                ),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = {},
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
        )
    }
}

@Preview(name = "Account Detail Dark", showBackground = true)
@Composable
private fun AccountDetailPreviewDark() {
    WalletTheme(darkTheme = true) {
        val sampleAccount =
            Account(
                id = 1L,
                name = "Cuenta de Ahorros",
                type = AccountType.SAVINGS,
                currencyCode = APP_CURRENCY,
                initialBalance = Money(1_000_000L),
                color = -16711936,
                icon = "savings",
                includeInTotal = true,
                isArchived = true,
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        AccountDetailScreen(
            uiState =
                AccountDetailUiState(
                    accountWithBalance = AccountWithBalance(sampleAccount, Money(1_500_000L)),
                    monthSummary = MonthSummary(income = Money(800_000L), expense = Money(300_000L)),
                ),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = {},
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
        )
    }
}
