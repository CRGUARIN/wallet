package com.crguarin.wallet.ui.accounts

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
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
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }

            val isLoading =
                pagingItems.loadState.refresh is LoadState.Loading &&
                    pagingItems.itemCount == 0

            when {
                isLoading -> {
                    item(key = "transactions_loading") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
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

@Composable
private fun AccountHeaderCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(account.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconCatalog.get(account.icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val typeText =
                        when (account.type) {
                            AccountType.CASH -> stringResource(R.string.account_type_cash)
                            AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
                        }
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (account.isArchived) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.account_detail_archived_badge),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.account_detail_current_balance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            MoneyText(
                amount = balance,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.account_detail_month_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryAmount(
                    label = stringResource(R.string.account_detail_income),
                    amount = monthSummary.income,
                    type = TransactionType.INCOME,
                )
                SummaryAmount(
                    label = stringResource(R.string.account_detail_expense),
                    amount = monthSummary.expense,
                    type = TransactionType.EXPENSE,
                )
                SummaryAmount(
                    label = stringResource(R.string.account_detail_net),
                    amount = monthSummary.net,
                )
            }
        }
    }
}

@Composable
private fun SummaryAmount(
    label: String,
    amount: Money,
    type: TransactionType? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                currencyCode = "COP",
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
                currencyCode = "COP",
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
