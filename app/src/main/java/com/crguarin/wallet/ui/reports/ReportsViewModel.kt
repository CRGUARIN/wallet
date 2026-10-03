package com.crguarin.wallet.ui.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveCategoryBreakdownUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveMonthlyTotalsUseCase
import com.crguarin.wallet.domain.usecase.report.ObservePeriodSummaryUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveSpendingPaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import javax.inject.Inject

/** Saved-state keys with the `reports_` prefix; the month is the epochDay of its first day. */
private const val KEY_MODE = "reports_mode"
private const val KEY_MONTH = "reports_month"
private const val KEY_TYPE = "reports_type"
private const val KEY_ACCOUNT = "reports_account"
private const val KEY_DRILL_PARENT = "reports_drill_parent"

private const val NO_ACCOUNT = -1L
private const val NO_PARENT = -1L

/** Number of months drawn by the bar chart in MONTH mode. */
private const val BAR_MONTHS = 6L

/** Number of months in a year, drawn by the bar chart in YEAR mode. */
private const val MONTHS_IN_YEAR = 12

private val EMPTY_SUMMARY = PeriodSummary(Money.ZERO, Money.ZERO, null, Money.ZERO)
private val EMPTY_BREAKDOWN = CategoryBreakdown(Money.ZERO, emptyList(), null)

/**
 * ViewModel of the Reports screen. The whole selection persists in [SavedStateHandle] with the
 * `reports_` keys, so rotating or recreating the screen keeps the mode, period, type, account and
 * drill-down.
 *
 * Changing the period, type or account always clears the selected slice and the drill-down: it is
 * the simplest behavior allowed by the spec and avoids showing a parent without data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val categoryRepository: CategoryRepository,
        observeCategoryBreakdownUseCase: ObserveCategoryBreakdownUseCase,
        observeMonthlyTotalsUseCase: ObserveMonthlyTotalsUseCase,
        observePeriodSummaryUseCase: ObservePeriodSummaryUseCase,
        observeSpendingPaceUseCase: ObserveSpendingPaceUseCase,
        observeAccountsUseCase: ObserveAccountsUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        /** "Today", read fresh on every use so a date change is picked up without recreating. */
        private fun today(): LocalDate = timeProvider.today()

        private val selectionFlow = MutableStateFlow(readSelection(savedStateHandle))
        private val selectedSliceFlow = MutableStateFlow<SliceKey?>(null)
        private val parentSliceFlow = MutableStateFlow<CategorySlice?>(null)
        private val sheetFlow = MutableStateFlow(false)
        private val refreshFlow = MutableStateFlow(0)

        private val queryFlow: Flow<ReportsQuery> =
            combine(selectionFlow, refreshFlow) { selection, refresh -> ReportsQuery(selection, refresh) }

        private val dataFlow: Flow<ReportData> =
            queryFlow.flatMapLatest { query ->
                val selection = query.selection
                flow {
                    // The pace query only runs in MONTH mode, where its card is visible.
                    val queryToday = today()
                    val paceFlow: Flow<SpendingPace?> =
                        if (selection.mode == ReportMode.MONTH) {
                            observeSpendingPaceUseCase(selection.yearMonth, selection.type, selection.accountId, queryToday)
                        } else {
                            flowOf(null)
                        }
                    emitAll(
                        combine(
                            observePeriodSummaryUseCase(selection.period(), selection.type, selection.accountId, queryToday),
                            observeCategoryBreakdownUseCase(
                                period = selection.period(),
                                type = selection.type,
                                accountId = selection.accountId,
                                parentId = selection.drillDownParentId,
                            ),
                            observeMonthlyTotalsUseCase(barMonths(selection), selection.accountId),
                            paceFlow,
                        ) { summary, breakdown, monthlyTotals, pace ->
                            ReportData(query, queryToday, summary, breakdown, monthlyTotals, pace, hasError = false)
                        },
                    )
                }.catch {
                    emit(ReportData(query, today(), EMPTY_SUMMARY, EMPTY_BREAKDOWN, emptyList(), null, hasError = true))
                }
            }

        // Archived accounts are kept so their history can still be filtered, like in Movements.
        private val accountsFlow = observeAccountsUseCase(includeArchived = true)

        private val panelFlow: Flow<PanelState> =
            combine(selectedSliceFlow, parentSliceFlow, sheetFlow) { slice, parent, showSheet ->
                PanelState(selectedSliceKey = slice, parentSlice = parent, showAccountSheet = showSheet)
            }

        val uiState: StateFlow<ReportsUiState> =
            combine(
                queryFlow,
                dataFlow,
                accountsFlow,
                panelFlow,
            ) { query, data, accounts, panel ->
                // The controls follow the new selection at once, while the data shown (and its error
                // flag) is still the previous complete snapshot until the new query emits; then
                // isRefreshing is false. A retry therefore keeps the error visible instead of
                // flashing the empty state.
                val selection = query.selection
                ReportsUiState(
                    isLoading = false,
                    hasError = data.hasError,
                    mode = selection.mode,
                    yearMonth = selection.yearMonth,
                    today = data.today,
                    type = selection.type,
                    accountId = selection.accountId,
                    selectedAccountName =
                        selection.accountId?.let { id ->
                            accounts.firstOrNull { it.account.id == id }?.account?.name
                        },
                    accounts = accounts,
                    showAccountSheet = panel.showAccountSheet,
                    summary = data.summary,
                    breakdown = data.breakdown,
                    drillDownParent = panel.parentSlice,
                    selectedSliceKey = panel.selectedSliceKey,
                    monthlyTotals = data.monthlyTotals,
                    pace = data.pace.takeIf { selection.mode == ReportMode.MONTH },
                    canGoNext = canGoNext(selection, today()),
                    isRefreshing = data.query != query,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = initialUiState(),
            )

        /** Loading state shown before the first query emits; it already carries the restored selection. */
        private fun initialUiState(): ReportsUiState {
            val selection = selectionFlow.value
            return ReportsUiState(
                isLoading = true,
                mode = selection.mode,
                yearMonth = selection.yearMonth,
                today = today(),
                type = selection.type,
                accountId = selection.accountId,
            )
        }

        init {
            selectionFlow.value.drillDownParentId?.let { parentId ->
                viewModelScope.launch { loadParentSlice(parentId) }
            }
        }

        fun setMode(mode: ReportMode) {
            val selection = selectionFlow.value
            if (selection.mode == mode) return
            applySelection(selection.copy(mode = mode, drillDownParentId = null))
        }

        fun previousPeriod() {
            val selection = selectionFlow.value
            val yearMonth =
                when (selection.mode) {
                    ReportMode.MONTH -> selection.yearMonth.minusMonths(1)
                    ReportMode.YEAR -> selection.yearMonth.minusYears(1)
                }
            applySelection(selection.copy(yearMonth = yearMonth, drillDownParentId = null))
        }

        fun nextPeriod() {
            val selection = selectionFlow.value
            if (!canGoNext(selection)) return
            val yearMonth =
                when (selection.mode) {
                    ReportMode.MONTH -> selection.yearMonth.plusMonths(1)
                    ReportMode.YEAR -> selection.yearMonth.plusYears(1)
                }
            applySelection(selection.copy(yearMonth = yearMonth, drillDownParentId = null))
        }

        fun setType(type: TransactionType) {
            if (type == TransactionType.TRANSFER) return
            applySelection(selectionFlow.value.copy(type = type, drillDownParentId = null))
        }

        fun setAccount(accountId: Long?) {
            applySelection(selectionFlow.value.copy(accountId = accountId, drillDownParentId = null))
            dismissAccountSheet()
        }

        fun openAccountSheet() {
            sheetFlow.value = true
        }

        fun dismissAccountSheet() {
            sheetFlow.value = false
        }

        fun selectSlice(key: SliceKey) {
            selectedSliceFlow.value = if (selectedSliceFlow.value == key) null else key
        }

        fun drillInto(parentId: Long) {
            val selection = selectionFlow.value
            if (selection.drillDownParentId == parentId) return
            selectedSliceFlow.value = null
            parentSliceFlow.value = null
            persist(selection.copy(drillDownParentId = parentId))
            selectionFlow.value = selection.copy(drillDownParentId = parentId)
            viewModelScope.launch { loadParentSlice(parentId) }
        }

        fun drillUp() {
            val selection = selectionFlow.value
            selectedSliceFlow.value = null
            parentSliceFlow.value = null
            persist(selection.copy(drillDownParentId = null))
            selectionFlow.value = selection.copy(drillDownParentId = null)
        }

        fun selectMonth(yearMonth: YearMonth) {
            // The bar chart must not navigate into the future (spec 1): ignore future months.
            if (yearMonth > YearMonth.from(today())) return
            applySelection(
                selectionFlow.value.copy(
                    mode = ReportMode.MONTH,
                    yearMonth = yearMonth,
                    drillDownParentId = null,
                ),
            )
        }

        /** Re-subscribes every report flow; used by the error state "Retry" action. */
        fun retry() {
            refreshFlow.value = refreshFlow.value + 1
        }

        private fun applySelection(selection: ReportsSelection) {
            val clamped = clampToToday(selection)
            selectedSliceFlow.value = null
            parentSliceFlow.value = null
            persist(clamped)
            selectionFlow.value = clamped
        }

        /** The selection can never point past the current month (no future periods). */
        private fun clampToToday(selection: ReportsSelection): ReportsSelection {
            val maxMonth = YearMonth.from(today())
            return if (selection.yearMonth > maxMonth) selection.copy(yearMonth = maxMonth) else selection
        }

        private fun persist(selection: ReportsSelection) {
            savedStateHandle[KEY_MODE] = selection.mode.name
            savedStateHandle[KEY_MONTH] = selection.yearMonth.atDay(1).toEpochDay()
            savedStateHandle[KEY_TYPE] = selection.type.name
            savedStateHandle[KEY_ACCOUNT] = selection.accountId ?: NO_ACCOUNT
            savedStateHandle[KEY_DRILL_PARENT] = selection.drillDownParentId ?: NO_PARENT
        }

        private suspend fun loadParentSlice(parentId: Long) {
            val category = categoryRepository.getCategory(parentId)
            // A newer drill (or drill-up) happened while loading: this result is stale.
            if (selectionFlow.value.drillDownParentId != parentId) return
            if (category == null) {
                // The drilled parent no longer exists: back out to the root instead of getting stuck.
                drillUp()
                return
            }
            parentSliceFlow.value =
                CategorySlice(
                    key = SliceKey.Category(parentId),
                    name = category.name,
                    systemKey = category.systemKey,
                    color = category.color,
                    icon = category.icon,
                    amount = Money.ZERO,
                    perMille = 0,
                    hasChildren = true,
                )
        }

        private fun canGoNext(
            selection: ReportsSelection,
            today: LocalDate = today(),
        ): Boolean =
            when (selection.mode) {
                ReportMode.MONTH -> selection.yearMonth < YearMonth.from(today)
                ReportMode.YEAR -> selection.yearMonth.year < today.year
            }

        private fun barMonths(selection: ReportsSelection): List<YearMonth> =
            when (selection.mode) {
                ReportMode.MONTH ->
                    (BAR_MONTHS - 1 downTo 0).map { monthsBack -> selection.yearMonth.minusMonths(monthsBack) }
                ReportMode.YEAR ->
                    (1..MONTHS_IN_YEAR).map { month -> YearMonth.of(selection.yearMonth.year, month) }
            }

        private fun readSelection(handle: SavedStateHandle): ReportsSelection {
            val mode =
                handle
                    .get<String>(KEY_MODE)
                    ?.let { runCatching { ReportMode.valueOf(it) }.getOrNull() }
                    ?: ReportMode.MONTH
            val yearMonth =
                handle
                    .get<Long>(KEY_MONTH)
                    ?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
                    ?: YearMonth.from(today())
            val type =
                handle
                    .get<String>(KEY_TYPE)
                    ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    ?.takeIf { it != TransactionType.TRANSFER }
                    ?: TransactionType.EXPENSE
            val accountId = handle.get<Long>(KEY_ACCOUNT)?.takeIf { it != NO_ACCOUNT }
            val drillDownParentId = handle.get<Long>(KEY_DRILL_PARENT)?.takeIf { it != NO_PARENT }
            return clampToToday(
                ReportsSelection(
                    mode = mode,
                    yearMonth = yearMonth,
                    type = type,
                    accountId = accountId,
                    drillDownParentId = drillDownParentId,
                ),
            )
        }

        private data class ReportsSelection(
            val mode: ReportMode,
            val yearMonth: YearMonth,
            val type: TransactionType,
            val accountId: Long?,
            val drillDownParentId: Long?,
        ) {
            fun period(): ReportPeriod =
                when (mode) {
                    ReportMode.MONTH -> ReportPeriod.Month(yearMonth)
                    ReportMode.YEAR -> ReportPeriod.Year(Year.of(yearMonth.year))
                }
        }

        /**
         * Rows combined from the four report use cases. Each snapshot travels with the [ReportsQuery]
         * it was loaded for (and the "today" it used), so the UI never mixes a new selection with
         * half-loaded data. [hasError] marks the placeholder emitted when the query failed.
         */
        private data class ReportData(
            val query: ReportsQuery,
            val today: LocalDate,
            val summary: PeriodSummary,
            val breakdown: CategoryBreakdown,
            val monthlyTotals: List<MonthTotals>,
            val pace: SpendingPace?,
            val hasError: Boolean,
        )

        /** A selection plus the retry counter, so a retry of the same selection is a new query. */
        private data class ReportsQuery(
            val selection: ReportsSelection,
            val refresh: Int,
        )

        private data class PanelState(
            val selectedSliceKey: SliceKey?,
            val parentSlice: CategorySlice?,
            val showAccountSheet: Boolean,
        )
    }
