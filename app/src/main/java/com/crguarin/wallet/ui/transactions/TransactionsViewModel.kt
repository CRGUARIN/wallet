package com.crguarin.wallet.ui.transactions

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDaySeparators
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.RestoreTransactionUseCase
import com.crguarin.wallet.ui.navigation.Transactions
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Saved-state keys so the month, filters and search survive process recreation. */
private const val KEY_MONTH = "tx_list_month"
private const val KEY_TYPE = "tx_list_type"
private const val KEY_ACCOUNT = "tx_list_account"
private const val KEY_CATEGORY = "tx_list_category"
private const val KEY_SEARCH = "tx_list_search"

private const val NO_FILTER = -1L
private const val TYPE_NONE = "none"
private const val SEARCH_DEBOUNCE_MILLIS = 300L

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val searchQuery: String = "",
    val accounts: List<AccountWithBalance> = emptyList(),
    val categoryNodes: List<CategoryNode> = emptyList(),
    val selectedCategory: Category? = null,
    val selectedParentCategory: Category? = null,
    val showAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    /** Net per day for the active filters, shown in the day headers of the list. */
    val dailyTotals: Map<LocalDate, Money> = emptyMap(),
) {
    val hasActiveFilters: Boolean
        get() = type != null || accountId != null || categoryId != null || searchQuery.isNotBlank()

    val maxMonth: YearMonth
        get() = YearMonth.from(today).plusYears(1)

    val canGoToNextMonth: Boolean
        get() = month < maxMonth

    val selectedAccountName: String?
        get() = accountId?.let { id -> accounts.firstOrNull { it.account.id == id }?.account?.name }
}

sealed interface TransactionsEvent {
    /** Carries the deleted transaction so each snackbar's "undo" restores its own deletion. */
    data class ShowTransactionDeleted(
        val transaction: Transaction,
    ) : TransactionsEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : TransactionsEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val categoryRepository: CategoryRepository,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val observeCategoryTreeUseCase: ObserveCategoryTreeUseCase,
        private val observeTransactionsUseCase: ObserveTransactionsUseCase,
        private val observeDailyTotalsUseCase: ObserveDailyTotalsUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val restoreTransactionUseCase: RestoreTransactionUseCase,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        private val route: Transactions = savedStateHandle.toRoute<Transactions>()
        private val today: LocalDate = timeProvider.today()

        private val filterFlow: MutableStateFlow<FilterSelection> =
            MutableStateFlow(
                FilterSelection(
                    month = readMonth(savedStateHandle, route.month),
                    type = readType(savedStateHandle, route.type),
                    accountId = readId(savedStateHandle, KEY_ACCOUNT, route.accountId),
                    categoryId = readId(savedStateHandle, KEY_CATEGORY, route.categoryId),
                ),
            )

        private val searchFlow: MutableStateFlow<String> =
            MutableStateFlow(savedStateHandle.get<String>(KEY_SEARCH).orEmpty())

        private val sheetFlow: MutableStateFlow<SheetState> = MutableStateFlow(SheetState())
        private val categorySelectionFlow: MutableStateFlow<CategorySelection> = MutableStateFlow(CategorySelection())

        private val _events = Channel<TransactionsEvent>(Channel.BUFFERED)
        val events: Flow<TransactionsEvent> = _events.receiveAsFlow()

        // Archived accounts are included so their history can still be filtered (e.g. "View all" from their detail).
        private val accountsFlow: Flow<List<AccountWithBalance>> = observeAccountsUseCase(includeArchived = true)

        private val categoryNodesFlow: Flow<List<CategoryNode>> =
            combine(
                observeCategoryTreeUseCase(CategoryType.EXPENSE),
                observeCategoryTreeUseCase(CategoryType.INCOME),
            ) { expenses, incomes -> expenses + incomes }

        private val uiFilterFlow: Flow<ListQuery> =
            combine(filterFlow, searchFlow) { selection, search -> ListQuery(selection, search) }

        private val queryFlow: Flow<ListQuery> =
            combine(
                filterFlow,
                searchFlow.debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }.distinctUntilChanged(),
            ) { selection, search -> ListQuery(selection, search) }

        private val dailyTotalsFlow: Flow<Map<LocalDate, Money>> =
            queryFlow.flatMapLatest { query -> observeDailyTotalsUseCase(query.toTransactionFilter()) }

        private val panelFlow: Flow<PanelState> =
            combine(categorySelectionFlow, sheetFlow, dailyTotalsFlow, ::PanelState)

        val uiState: StateFlow<TransactionsUiState> =
            combine(
                uiFilterFlow,
                accountsFlow,
                categoryNodesFlow,
                panelFlow,
            ) { query, accounts, categoryNodes, panel ->
                TransactionsUiState(
                    isLoading = false,
                    month = query.selection.month,
                    today = today,
                    type = query.selection.type,
                    accountId = query.selection.accountId,
                    categoryId = query.selection.categoryId,
                    searchQuery = query.searchQuery,
                    accounts = accounts,
                    categoryNodes = categoryNodes,
                    selectedCategory = panel.categorySelection.category,
                    selectedParentCategory = panel.categorySelection.parent,
                    showAccountSheet = panel.sheetState.showAccountSheet,
                    showCategorySheet = panel.sheetState.showCategorySheet,
                    dailyTotals = panel.dailyTotals,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TransactionsUiState(today = today, isLoading = true),
            )

        /**
         * Paginated transactions with a day header inserted before each new day. The day totals
         * live in [TransactionsUiState.dailyTotals]; they are not combined here because every
         * re-emission would collect the same `PagingData` twice.
         */
        val pagingData: Flow<PagingData<TransactionListEntry>> =
            queryFlow
                .flatMapLatest { query -> observeTransactionsUseCase(query.toTransactionFilter()) }
                .map { paging ->
                    paging
                        .map { TransactionListEntry.Item(it) }
                        .insertSeparators<TransactionListEntry.Item, TransactionListEntry> { before, after ->
                            TransactionDaySeparators.headerFor(before, after)
                        }
                }.cachedIn(viewModelScope)

        init {
            if (filterFlow.value.categoryId != null) {
                viewModelScope.launch { resolveCategorySelection(filterFlow.value.categoryId) }
            }
        }

        fun previousMonth() {
            setMonth(filterFlow.value.month.minusMonths(1))
        }

        fun nextMonth() {
            val next = filterFlow.value.month.plusMonths(1)
            if (next <= YearMonth.from(today).plusYears(1)) {
                setMonth(next)
            }
        }

        fun setType(type: TransactionType?) {
            savedStateHandle[KEY_TYPE] = type?.name ?: TYPE_NONE
            filterFlow.update { it.copy(type = type) }
        }

        fun setAccountId(accountId: Long?) {
            savedStateHandle[KEY_ACCOUNT] = accountId ?: NO_FILTER
            filterFlow.update { it.copy(accountId = accountId) }
            dismissAccountSheet()
        }

        fun setCategoryId(categoryId: Long?) {
            savedStateHandle[KEY_CATEGORY] = categoryId ?: NO_FILTER
            filterFlow.update { it.copy(categoryId = categoryId) }
            dismissCategorySheet()
            viewModelScope.launch { resolveCategorySelection(categoryId) }
        }

        fun setSearchQuery(query: String) {
            savedStateHandle[KEY_SEARCH] = query
            searchFlow.value = query
        }

        fun clearFilters() {
            savedStateHandle[KEY_TYPE] = TYPE_NONE
            savedStateHandle[KEY_ACCOUNT] = NO_FILTER
            savedStateHandle[KEY_CATEGORY] = NO_FILTER
            savedStateHandle[KEY_SEARCH] = ""
            filterFlow.update { it.copy(type = null, accountId = null, categoryId = null) }
            searchFlow.value = ""
            viewModelScope.launch { resolveCategorySelection(null) }
        }

        fun openAccountSheet() {
            sheetFlow.update { it.copy(showAccountSheet = true) }
        }

        fun dismissAccountSheet() {
            sheetFlow.update { it.copy(showAccountSheet = false) }
        }

        fun openCategorySheet() {
            sheetFlow.update { it.copy(showCategorySheet = true) }
        }

        fun dismissCategorySheet() {
            sheetFlow.update { it.copy(showCategorySheet = false) }
        }

        fun deleteTransaction(details: TransactionDetails) {
            viewModelScope.launch {
                when (val result = deleteTransactionUseCase(details.transaction.id)) {
                    is DomainResult.Success -> {
                        _events.send(TransactionsEvent.ShowTransactionDeleted(details.transaction))
                    }
                    is DomainResult.Failure -> {
                        _events.send(TransactionsEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
            }
        }

        fun undoDelete(transaction: Transaction) {
            viewModelScope.launch {
                when (val result = restoreTransactionUseCase(transaction)) {
                    is DomainResult.Success -> Unit
                    is DomainResult.Failure -> {
                        _events.send(TransactionsEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
            }
        }

        private fun setMonth(month: YearMonth) {
            savedStateHandle[KEY_MONTH] = month.atDay(1).toEpochDay()
            filterFlow.update { it.copy(month = month) }
        }

        private suspend fun resolveCategorySelection(categoryId: Long?) {
            if (categoryId == null) {
                categorySelectionFlow.value = CategorySelection()
                return
            }
            val category = categoryRepository.getCategory(categoryId)
            val parent = category?.parentId?.let { categoryRepository.getCategory(it) }
            categorySelectionFlow.value = CategorySelection(category = category, parent = parent)
        }

        private fun readMonth(
            savedStateHandle: SavedStateHandle,
            routeMonth: String?,
        ): YearMonth {
            savedStateHandle.get<Long>(KEY_MONTH)?.let { epochDay ->
                return YearMonth.from(LocalDate.ofEpochDay(epochDay))
            }
            // The route carries ISO `yyyy-MM`. A malformed value is ignored: fall back to this month.
            return routeMonth
                ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                ?: YearMonth.from(today)
        }

        private fun readType(
            savedStateHandle: SavedStateHandle,
            routeType: TransactionType?,
        ): TransactionType? {
            val stored = savedStateHandle.get<String>(KEY_TYPE) ?: return routeType
            return stored
                .takeIf { it != TYPE_NONE }
                ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
        }

        private fun readId(
            savedStateHandle: SavedStateHandle,
            key: String,
            routeValue: Long?,
        ): Long? {
            val stored = savedStateHandle.get<Long>(key) ?: routeValue ?: NO_FILTER
            return stored.takeIf { it != NO_FILTER }
        }

        private data class FilterSelection(
            val month: YearMonth,
            val type: TransactionType?,
            val accountId: Long?,
            val categoryId: Long?,
        )

        private data class ListQuery(
            val selection: FilterSelection,
            val searchQuery: String,
        ) {
            fun toTransactionFilter(): TransactionFilter =
                TransactionFilter(
                    startDate = selection.month.atDay(1),
                    endDate = selection.month.atEndOfMonth(),
                    accountId = selection.accountId,
                    categoryId = selection.categoryId,
                    type = selection.type,
                    noteQuery = searchQuery.ifBlank { null },
                )
        }

        private data class SheetState(
            val showAccountSheet: Boolean = false,
            val showCategorySheet: Boolean = false,
        )

        private data class PanelState(
            val categorySelection: CategorySelection,
            val sheetState: SheetState,
            val dailyTotals: Map<LocalDate, Money>,
        )

        private data class CategorySelection(
            val category: Category? = null,
            val parent: Category? = null,
        )
    }
