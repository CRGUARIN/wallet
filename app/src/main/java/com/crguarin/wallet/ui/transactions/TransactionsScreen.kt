package com.crguarin.wallet.ui.transactions

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.PeriodSelector
import com.crguarin.wallet.ui.components.TransactionDayHeader
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.components.groupedItemBackground
import com.crguarin.wallet.ui.components.groupedRowShape
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.formatMonthYear
import com.crguarin.wallet.ui.util.safeHiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate

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
                        viewModel.undoDelete(event.deleted)
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val locale = LocalConfiguration.current.locales[0]
    val spacing = MaterialTheme.walletSpacing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // The search field takes no room until the magnifier is tapped. A query that is already active
    // (for example restored after the process died) keeps it visible.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var focusRequests by remember { mutableIntStateOf(0) }
    val searchVisible = searchOpen || uiState.searchQuery.isNotEmpty()
    val closeSearch = {
        searchOpen = false
        onSearchQueryChanged("")
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.transactions_title)) },
                    actions = {
                        IconButton(
                            onClick = {
                                if (searchVisible) {
                                    closeSearch()
                                } else {
                                    searchOpen = true
                                    focusRequests++
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                AnimatedVisibility(visible = searchVisible) {
                    SearchField(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChanged,
                        onClose = closeSearch,
                        focusRequests = focusRequests,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.screenGutter, vertical = spacing.xs),
                    )
                }
            }
        },
        snackbarHost = {
            // Lifted above the floating "new movement" button, which would hide the undo action.
            SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = FabClearance))
        },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
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
                PeriodSelector(
                    label = uiState.month.formatMonthYear(locale),
                    canGoPrevious = true,
                    canGoNext = uiState.canGoToNextMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    previousContentDescription = stringResource(R.string.transactions_previous_month),
                    nextContentDescription = stringResource(R.string.transactions_next_month),
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

            val isLoading =
                uiState.isLoading &&
                    pagingItems.itemCount == 0 &&
                    pagingItems.loadState.refresh is LoadState.Loading

            val refreshError = pagingItems.loadState.refresh as? LoadState.Error

            when {
                isLoading -> {
                    item(key = "loading", contentType = "status") {
                        LoadingState(fillMaxSize = false, modifier = Modifier.fillMaxWidth())
                    }
                }

                refreshError != null && pagingItems.itemCount == 0 -> {
                    item(key = "error", contentType = "status") {
                        ErrorState(
                            message = stringResource(R.string.transactions_load_error),
                            actionLabel = stringResource(R.string.action_retry),
                            onActionClick = { pagingItems.retry() },
                        )
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
                                    // The header pads its own sides; this only separates one day from the previous.
                                    modifier = Modifier.padding(top = spacing.m),
                                )
                            }
                            is TransactionListEntry.Item -> {
                                // The rows of a day share one rounded layer: find this row's place in its
                                // day. Reading the count makes the row that was last square its corners
                                // when the next page is appended.
                                val loadedCount = pagingItems.itemCount
                                var dayStart = index
                                while (dayStart > 0 && pagingItems.peek(dayStart - 1) is TransactionListEntry.Item) {
                                    dayStart--
                                }
                                var dayEnd = index
                                while (dayEnd + 1 < loadedCount && pagingItems.peek(dayEnd + 1) is TransactionListEntry.Item) {
                                    dayEnd++
                                }
                                SwipeableTransactionRow(
                                    details = entry.details,
                                    perspectiveAccountId = uiState.accountId,
                                    positionInDay = index - dayStart,
                                    rowsInDay = dayEnd - dayStart + 1,
                                    onClick = { onTransactionClick(entry.details.transaction.id) },
                                    onDelete = { onDeleteTransaction(entry.details) },
                                    modifier = Modifier.padding(horizontal = spacing.screenGutter),
                                )
                            }
                            null -> Unit
                        }
                    }
                    if (pagingItems.loadState.append is LoadState.Error) {
                        item(key = "append_error", contentType = "status") {
                            ErrorState(
                                message = stringResource(R.string.transactions_load_error),
                                actionLabel = stringResource(R.string.action_retry),
                                onActionClick = { pagingItems.retry() },
                            )
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

/** Search field: a pill that gets the focus (and the keyboard) when it appears, with a button that closes it. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequests: Int,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Only a tap on the magnifier asks for focus; a field shown because of a restored query does not steal it.
    LaunchedEffect(focusRequests) {
        if (focusRequests > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val clear = Color.Transparent
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = RoundedCornerShape(SearchFieldRadius),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = clear,
                unfocusedIndicatorColor = clear,
                disabledIndicatorColor = clear,
                errorIndicatorColor = clear,
            ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_search),
                )
            }
        },
        placeholder = { Text(text = stringResource(R.string.transactions_search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        modifier = modifier.focusRequester(focusRequester),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    details: TransactionDetails,
    perspectiveAccountId: Long?,
    positionInDay: Int,
    rowsInDay: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier.clip(groupedRowShape(positionInDay, rowsInDay)),
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = MaterialTheme.walletSpacing.xxl),
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
            modifier = Modifier.groupedItemBackground(positionInDay, rowsInDay),
        )
    }
}

/** Corner radius of the search pill. */
private val SearchFieldRadius = 26.dp

/** Space the floating action button takes at the bottom end, kept free of snackbars. */
private val FabClearance = 72.dp

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

@Preview(name = "Transactions Font Scale 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun TransactionsScreenPreviewLargeFont() {
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
