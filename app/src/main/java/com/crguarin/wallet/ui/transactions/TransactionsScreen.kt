package com.crguarin.wallet.ui.transactions

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.TransactionDayHeader
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.formatMonthYear
import com.crguarin.wallet.ui.util.safeHiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun TransactionsRoute(
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        TransactionsScreen(
            uiState = TransactionsUiState(isLoading = false, today = LocalDate.now()),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onPreviousMonth = {},
            onNextMonth = {},
            onTypeSelected = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onClearFilters = {},
            onSearchQueryChanged = {},
            onTransactionClick = onTransactionClick,
            onDeleteTransaction = {},
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
                is TransactionsEvent.ShowTransactionDeleted -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.transactions_deleted_message),
                            actionLabel = context.getString(R.string.action_undo),
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.transaction)
                    }
                }
                is TransactionsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    TransactionsScreen(
        uiState = uiState,
        pagingItems = pagingItems,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onTypeSelected = viewModel::setType,
        onOpenAccountSheet = viewModel::openAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelected = viewModel::setAccountId,
        onOpenCategorySheet = viewModel::openCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onCategorySelected = viewModel::setCategoryId,
        onClearFilters = viewModel::clearFilters,
        onSearchQueryChanged = viewModel::setSearchQuery,
        onTransactionClick = onTransactionClick,
        onDeleteTransaction = viewModel::deleteTransaction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    pagingItems: LazyPagingItems<TransactionListEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelected: (Long?) -> Unit,
    onOpenCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onDeleteTransaction: (TransactionDetails) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.transactions_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "month_selector", contentType = "controls") {
                MonthSelector(
                    month = uiState.month,
                    canGoToNextMonth = uiState.canGoToNextMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }

            item(key = "filters", contentType = "controls") {
                FilterRow(
                    uiState = uiState,
                    onTypeSelected = onTypeSelected,
                    onOpenAccountSheet = onOpenAccountSheet,
                    onOpenCategorySheet = onOpenCategorySheet,
                    onClearFilters = onClearFilters,
                )
            }

            item(key = "search", contentType = "controls") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                )
                            }
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.transactions_search_hint)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            val isLoading =
                uiState.isLoading &&
                    pagingItems.itemCount == 0 &&
                    pagingItems.loadState.refresh is LoadState.Loading

            when {
                isLoading -> {
                    item(key = "loading", contentType = "status") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                pagingItems.itemCount == 0 -> {
                    item(key = "empty", contentType = "status") {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title =
                                stringResource(
                                    if (uiState.hasActiveFilters) {
                                        R.string.transactions_empty_filtered_title
                                    } else {
                                        R.string.transactions_empty_title
                                    },
                                ),
                            message =
                                stringResource(
                                    if (uiState.hasActiveFilters) {
                                        R.string.transactions_empty_filtered_message
                                    } else {
                                        R.string.transactions_empty_message
                                    },
                                ),
                            actionLabel =
                                if (uiState.hasActiveFilters) {
                                    stringResource(R.string.transactions_clear_filters)
                                } else {
                                    null
                                },
                            onActionClick = if (uiState.hasActiveFilters) onClearFilters else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> entryKey(pagingItems.peek(index), index) },
                        contentType = { index -> entryContentType(pagingItems.peek(index)) },
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
                                SwipeableTransactionRow(
                                    details = entry.details,
                                    perspectiveAccountId = uiState.accountId,
                                    onClick = { onTransactionClick(entry.details.transaction.id) },
                                    onDelete = { onDeleteTransaction(entry.details) },
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            onAccountSelected = { onAccountSelected(it.account.id) },
            onDismissRequest = onDismissAccountSheet,
            allOptionLabel = stringResource(R.string.transactions_filter_all_accounts),
            onAllOptionSelected = { onAccountSelected(null) },
        )
    }

    if (uiState.showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categoryNodes,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { onCategorySelected(it.id) },
            onNewCategoryClick = {},
            onDismissRequest = onDismissCategorySheet,
            showNewCategoryAction = false,
            allOptionLabel = stringResource(R.string.transactions_filter_all_categories),
            onAllOptionSelected = { onCategorySelected(null) },
        )
    }
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    canGoToNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.transactions_previous_month),
            )
        }
        Text(
            text = month.formatMonthYear(locale),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(
            onClick = onNextMonth,
            enabled = canGoToNextMonth,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.transactions_next_month),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    details: TransactionDetails,
    perspectiveAccountId: Long?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_transaction),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        TransactionListItem(
            details = details,
            perspectiveAccountId = perspectiveAccountId,
            onClick = onClick,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        )
    }
}

private fun entryKey(
    entry: TransactionListEntry?,
    index: Int,
): Any =
    when (entry) {
        is TransactionListEntry.DayHeader -> "day_header_${entry.date.toEpochDay()}"
        is TransactionListEntry.Item -> entry.details.transaction.id
        null -> "placeholder_$index"
    }

private fun entryContentType(entry: TransactionListEntry?): Any =
    when (entry) {
        is TransactionListEntry.DayHeader -> "day_header"
        is TransactionListEntry.Item -> "transaction_item"
        null -> "placeholder"
    }

@Composable
fun rememberEntriesPagingItems(entries: List<TransactionListEntry>): LazyPagingItems<TransactionListEntry> {
    val flow = remember(entries) { MutableStateFlow(PagingData.from(entries)) }
    return flow.collectAsLazyPagingItems()
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }

private fun previewDetails(
    id: Long,
    type: TransactionType,
    date: LocalDate,
    categorySystemKey: String? = null,
    parentCategorySystemKey: String? = null,
    note: String? = null,
): TransactionDetails =
    TransactionDetails(
        transaction =
            Transaction(
                id = id,
                type = type,
                amount = Money(4_550_000L),
                accountId = 1L,
                toAccountId = if (type == TransactionType.TRANSFER) 2L else null,
                categoryId = if (type == TransactionType.TRANSFER) null else 10L,
                date = date,
                note = note,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        toAccountName = if (type == TransactionType.TRANSFER) "Ahorros" else null,
        categoryName = null,
        categorySystemKey = categorySystemKey,
        categoryColor = if (type == TransactionType.TRANSFER) null else 0xFFEF5350.toInt(),
        categoryIcon = if (type == TransactionType.TRANSFER) null else "restaurant",
        categoryParentId = if (parentCategorySystemKey != null) 1L else null,
        parentCategoryName = null,
        parentCategorySystemKey = parentCategorySystemKey,
    )

private fun previewDailyTotals(): Map<LocalDate, Money> {
    val today = LocalDate.of(2026, 9, 28)
    return mapOf(today to Money(2_000_000L), today.minusDays(1) to Money(-150_000L))
}

private fun previewEntries(): List<TransactionListEntry> {
    val today = LocalDate.of(2026, 9, 28)
    return listOf(
        TransactionListEntry.DayHeader(today),
        TransactionListEntry.Item(
            previewDetails(
                1L,
                TransactionType.EXPENSE,
                today,
                categorySystemKey = "groceries",
                parentCategorySystemKey = "food",
                note = "Mercado",
            ),
        ),
        TransactionListEntry.Item(previewDetails(2L, TransactionType.INCOME, today, categorySystemKey = "salary")),
        TransactionListEntry.DayHeader(today.minusDays(1)),
        TransactionListEntry.Item(previewDetails(3L, TransactionType.TRANSFER, today.minusDays(1))),
    )
}

@Preview(name = "Transactions Light", showBackground = true)
@Composable
private fun TransactionsScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        TransactionsScreen(
            uiState = TransactionsUiState(isLoading = false, dailyTotals = previewDailyTotals()),
            pagingItems = rememberEntriesPagingItems(previewEntries()),
            onPreviousMonth = {},
            onNextMonth = {},
            onTypeSelected = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onClearFilters = {},
            onSearchQueryChanged = {},
            onTransactionClick = {},
            onDeleteTransaction = {},
        )
    }
}

@Preview(name = "Transactions Dark", showBackground = true)
@Composable
private fun TransactionsScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        TransactionsScreen(
            uiState = TransactionsUiState(isLoading = false, dailyTotals = previewDailyTotals()).copy(searchQuery = "mercado"),
            pagingItems = rememberEntriesPagingItems(previewEntries()),
            onPreviousMonth = {},
            onNextMonth = {},
            onTypeSelected = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onClearFilters = {},
            onSearchQueryChanged = {},
            onTransactionClick = {},
            onDeleteTransaction = {},
        )
    }
}
