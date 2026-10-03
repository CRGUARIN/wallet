package com.crguarin.wallet.ui.reports

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceGranularity
import com.crguarin.wallet.domain.model.report.BalancePoint
import com.crguarin.wallet.domain.model.report.BalanceSummary
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.ReportFocus
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.repository.ReportRepository
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveBalanceEvolutionUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveBalanceSummaryUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveCategoryBreakdownUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveMonthlyTotalsUseCase
import com.crguarin.wallet.domain.usecase.report.ObservePeriodSummaryUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveSpendingPaceUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var timeProvider: FakeTimeProvider

    private val cash =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(0),
            color = 1,
            icon = "wallet",
            sortOrder = 0,
            createdAt = baseInstant,
        )
    private val savings = cash.copy(id = 2L, name = "Ahorros", icon = "savings", sortOrder = 1)
    private val food =
        Category(id = 10L, systemKey = "food", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")
    private val groceries =
        Category(id = 11L, systemKey = "groceries", type = CategoryType.EXPENSE, parentId = 10L, color = 1, icon = "local_grocery_store")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        reportRepository = FakeReportRepository()
        timeProvider = FakeTimeProvider(baseInstant)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        report: ReportRepository = reportRepository,
    ) = ReportsViewModel(
        savedStateHandle = handle,
        categoryRepository = categoryRepository,
        observeCategoryBreakdownUseCase = ObserveCategoryBreakdownUseCase(report),
        observeMonthlyTotalsUseCase = ObserveMonthlyTotalsUseCase(report),
        observePeriodSummaryUseCase = ObservePeriodSummaryUseCase(report),
        observeSpendingPaceUseCase = ObserveSpendingPaceUseCase(report),
        observeBalanceSummaryUseCase = ObserveBalanceSummaryUseCase(report),
        observeBalanceEvolutionUseCase = ObserveBalanceEvolutionUseCase(report, accountRepository),
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
        timeProvider = timeProvider,
    )

    private fun TestScope.observeState(viewModel: ReportsViewModel): ReportsViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun categoryRow(
        categoryId: Long?,
        name: String?,
        parentId: Long?,
        parentName: String?,
        total: Long,
    ) = CategoryTotal(
        categoryId = categoryId,
        name = name,
        systemKey = null,
        color = 1,
        icon = "restaurant",
        parentId = parentId,
        parentName = parentName,
        parentSystemKey = null,
        parentColor = 1,
        parentIcon = "restaurant",
        total = Money(total),
        transactionCount = 1,
    )

    @Test
    fun `starts on the current month with expenses and all accounts`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash, savings))
            reportRepository.setInitialData(
                dailyIncomeExpense = listOf(DailyIncomeExpense(today, Money(10_000L), Money(5_000L))),
            )

            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.hasError).isFalse()
            assertThat(state.mode).isEqualTo(ReportMode.MONTH)
            assertThat(state.yearMonth).isEqualTo(YearMonth.of(2026, 9))
            assertThat(state.today).isEqualTo(today)
            assertThat(state.focus).isEqualTo(ReportFocus.EXPENSE)
            assertThat(state.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(state.accountId).isNull()
            assertThat(state.accounts).hasSize(2)
            assertThat(state.canGoNext).isFalse()
            assertThat(state.monthlyTotals).hasSize(6)
        }

    @Test
    fun `previous and next period move by month and next stops at the current month`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.previousPeriod()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 8))
            assertThat(viewModel.uiState.value.canGoNext).isTrue()

            viewModel.nextPeriod()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 9))
            assertThat(viewModel.uiState.value.canGoNext).isFalse()

            viewModel.nextPeriod()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 9))
        }

    @Test
    fun `year mode moves by year and cannot go past the current year`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.setMode(ReportMode.YEAR)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.mode).isEqualTo(ReportMode.YEAR)
            assertThat(viewModel.uiState.value.yearMonth.year).isEqualTo(2026)
            assertThat(viewModel.uiState.value.canGoNext).isFalse()
            assertThat(viewModel.uiState.value.monthlyTotals).hasSize(12)

            viewModel.previousPeriod()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth.year).isEqualTo(2025)
            assertThat(viewModel.uiState.value.canGoNext).isTrue()
        }

    @Test
    fun `selecting a bar switches to month mode and selects that month`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            viewModel.setMode(ReportMode.YEAR)
            advanceUntilIdle()

            viewModel.selectMonth(YearMonth.of(2026, 5))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.mode).isEqualTo(ReportMode.MONTH)
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 5))
        }

    @Test
    fun `selecting a future month is ignored`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            viewModel.setMode(ReportMode.YEAR)
            advanceUntilIdle()

            viewModel.selectMonth(YearMonth.of(2026, 12))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.mode).isEqualTo(ReportMode.YEAR)
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 9))
        }

    @Test
    fun `drill down and up moves the breakdown and loads the parent`() =
        runTest(testDispatcher) {
            categoryRepository.setInitialData(listOf(food, groceries))
            reportRepository.setInitialData(
                categoryTotalsByType =
                    mapOf(
                        TransactionType.EXPENSE to
                            listOf(
                                categoryRow(10L, "Alimentación", null, null, 1_000L),
                                categoryRow(11L, "Mercado", 10L, "Alimentación", 500L),
                            ),
                    ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.drillInto(10L)
            advanceUntilIdle()
            val drilled = viewModel.uiState.value
            assertThat(drilled.breakdown.parentId).isEqualTo(10L)
            assertThat(drilled.drillDownParent?.key).isEqualTo(SliceKey.Category(10L))
            assertThat(drilled.breakdown.slices.map { it.key })
                .containsAtLeast(SliceKey.Category(11L), SliceKey.ParentDirect(10L))

            viewModel.drillUp()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.breakdown.parentId).isNull()
            assertThat(viewModel.uiState.value.drillDownParent).isNull()
        }

    @Test
    fun `selecting the same slice twice deselects it`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.selectedSliceKey).isEqualTo(SliceKey.Category(10L))

            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.selectedSliceKey).isNull()
        }

    @Test
    fun `changing the focus clears the selection and the drill down`() =
        runTest(testDispatcher) {
            categoryRepository.setInitialData(listOf(food))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.drillInto(10L)
            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()

            viewModel.setFocus(ReportFocus.INCOME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.INCOME)
            assertThat(state.type).isEqualTo(TransactionType.INCOME)
            assertThat(state.selectedSliceKey).isNull()
            assertThat(state.drillDownParent).isNull()
            assertThat(state.breakdown.parentId).isNull()
        }

    @Test
    fun `changing the period or account also clears the selection and drill down`() =
        runTest(testDispatcher) {
            categoryRepository.setInitialData(listOf(food))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.drillInto(10L)
            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()
            viewModel.setAccount(2L)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.accountId).isEqualTo(2L)
            assertThat(viewModel.uiState.value.selectedSliceKey).isNull()
            assertThat(viewModel.uiState.value.drillDownParent).isNull()
            assertThat(viewModel.uiState.value.breakdown.parentId).isNull()

            viewModel.drillInto(10L)
            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()
            viewModel.previousPeriod()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 8))
            assertThat(viewModel.uiState.value.selectedSliceKey).isNull()
            assertThat(viewModel.uiState.value.drillDownParent).isNull()
            assertThat(viewModel.uiState.value.breakdown.parentId).isNull()
        }

    @Test
    fun `selection survives recreation through saved state`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash, savings))
            val handle = SavedStateHandle()
            val first = observeState(createViewModel(handle))
            advanceUntilIdle()

            first.setMode(ReportMode.YEAR)
            first.previousPeriod()
            first.setFocus(ReportFocus.INCOME)
            first.setAccount(2L)
            advanceUntilIdle()

            val recreated = observeState(createViewModel(handle))
            advanceUntilIdle()

            val state = recreated.uiState.value
            assertThat(state.mode).isEqualTo(ReportMode.YEAR)
            assertThat(state.yearMonth).isEqualTo(YearMonth.of(2025, 9))
            assertThat(state.focus).isEqualTo(ReportFocus.INCOME)
            assertThat(state.type).isEqualTo(TransactionType.INCOME)
            assertThat(state.accountId).isEqualTo(2L)
            assertThat(state.selectedAccountName).isEqualTo("Ahorros")
        }

    @Test
    fun `drill down also survives recreation`() =
        runTest(testDispatcher) {
            categoryRepository.setInitialData(listOf(food, groceries))
            val handle = SavedStateHandle()
            val first = observeState(createViewModel(handle))
            advanceUntilIdle()

            first.drillInto(10L)
            advanceUntilIdle()

            val recreated = observeState(createViewModel(handle))
            advanceUntilIdle()

            assertThat(recreated.uiState.value.breakdown.parentId).isEqualTo(10L)
            val parent = recreated.uiState.value.drillDownParent
            assertThat(parent?.key).isEqualTo(SliceKey.Category(10L))
        }

    @Test
    fun `isEmpty looks at every bar month and hasNoDataOfType looks at the selected period`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isEmpty).isTrue()
            assertThat(viewModel.uiState.value.hasNoDataOfType).isTrue()

            reportRepository.setInitialData(
                categoryTotalsByType =
                    mapOf(
                        TransactionType.EXPENSE to listOf(categoryRow(10L, "Alimentación", null, null, 700L)),
                    ),
                dailyIncomeExpense = listOf(DailyIncomeExpense(today, Money.ZERO, Money(700L))),
            )
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isEmpty).isFalse()
            assertThat(viewModel.uiState.value.hasNoDataOfType).isFalse()

            viewModel.setFocus(ReportFocus.INCOME)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isEmpty).isFalse()
            assertThat(viewModel.uiState.value.hasNoDataOfType).isTrue()
        }

    @Test
    fun `year mode next from a december carryover clamps to the current month`() =
        runTest(testDispatcher) {
            timeProvider.setDate(LocalDate.of(2026, 10, 3))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            repeat(10) { viewModel.previousPeriod() }
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2025, 12))

            viewModel.setMode(ReportMode.YEAR)
            advanceUntilIdle()
            viewModel.nextPeriod()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.yearMonth).isEqualTo(YearMonth.of(2026, 10))
            assertThat(state.canGoNext).isFalse()

            viewModel.setMode(ReportMode.MONTH)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 10))
        }

    @Test
    fun `a restored future month is clamped to the current month`() =
        runTest(testDispatcher) {
            timeProvider.setDate(LocalDate.of(2026, 10, 3))
            // Keys mirror the private reports_* keys of ReportsViewModel.
            val handle =
                SavedStateHandle(
                    mapOf(
                        "reports_mode" to ReportMode.MONTH.name,
                        "reports_month" to LocalDate.of(2026, 12, 1).toEpochDay(),
                    ),
                )
            val viewModel = observeState(createViewModel(handle))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 10))
            assertThat(viewModel.uiState.value.canGoNext).isFalse()
        }

    @Test
    fun `today is read fresh for navigation limits`() =
        runTest(testDispatcher) {
            // Noon UTC keeps the same date in any time zone west of UTC+12.
            timeProvider.setTime(Instant.parse("2026-10-31T12:00:00Z"))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 10))
            assertThat(viewModel.uiState.value.canGoNext).isFalse()

            timeProvider.setTime(Instant.parse("2026-11-01T12:00:00Z"))
            viewModel.nextPeriod()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.yearMonth).isEqualTo(YearMonth.of(2026, 11))
            assertThat(state.today).isEqualTo(LocalDate.of(2026, 11, 1))
            assertThat(state.canGoNext).isFalse()
        }

    @Test
    fun `changing month never pairs new controls with old data unless refreshing`() =
        runTest(testDispatcher) {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        DailyIncomeExpense(LocalDate.of(2026, 8, 15), Money.ZERO, Money(8_000L)),
                        DailyIncomeExpense(LocalDate.of(2026, 9, 10), Money.ZERO, Money(9_000L)),
                    ),
            )
            val viewModel = createViewModel()
            viewModel.uiState.test {
                var current = awaitItem()
                while (current.isLoading) current = awaitItem()
                val loaded = current
                assertThat(loaded.isLoading).isFalse()
                assertThat(loaded.isRefreshing).isFalse()
                assertThat(loaded.yearMonth).isEqualTo(YearMonth.of(2026, 9))
                assertThat(loaded.summary.total).isEqualTo(Money(9_000L))

                viewModel.previousPeriod()

                val seen = mutableListOf<ReportsUiState>()
                var settled: ReportsUiState? = null
                while (settled == null) {
                    val state = awaitItem()
                    seen += state
                    if (!state.isLoading && state.yearMonth == YearMonth.of(2026, 8) && !state.isRefreshing) {
                        settled = state
                    }
                }
                val done = checkNotNull(settled)
                assertThat(done.summary.total).isEqualTo(Money(8_000L))
                assertThat(done.monthlyTotals.last().yearMonth).isEqualTo(YearMonth.of(2026, 8))

                val refreshing = seen.filter { it.isRefreshing }
                assertThat(refreshing).isNotEmpty()
                refreshing.forEach { state ->
                    // New controls (August) over the previous complete snapshot (September).
                    assertThat(state.isLoading).isFalse()
                    assertThat(state.yearMonth).isEqualTo(YearMonth.of(2026, 8))
                    assertThat(state.summary.total).isEqualTo(Money(9_000L))
                    assertThat(state.monthlyTotals.last().yearMonth).isEqualTo(YearMonth.of(2026, 9))
                }
                seen.filter { !it.isLoading && !it.isRefreshing }.forEach { state ->
                    val expected =
                        if (state.yearMonth == YearMonth.of(2026, 8)) Money(8_000L) else Money(9_000L)
                    assertThat(state.summary.total).isEqualTo(expected)
                }
            }
        }

    @Test
    fun `initial loading state already carries the restored selection`() =
        runTest(testDispatcher) {
            // Keys mirror the private reports_* keys of ReportsViewModel.
            val handle =
                SavedStateHandle(
                    mapOf(
                        "reports_mode" to ReportMode.YEAR.name,
                        "reports_month" to LocalDate.of(2024, 3, 1).toEpochDay(),
                        "reports_type" to TransactionType.INCOME.name,
                    ),
                )
            val viewModel = createViewModel(handle)
            val first = viewModel.uiState.value
            assertThat(first.isLoading).isTrue()
            assertThat(first.mode).isEqualTo(ReportMode.YEAR)
            assertThat(first.yearMonth).isEqualTo(YearMonth.of(2024, 3))
            assertThat(first.focus).isEqualTo(ReportFocus.INCOME)
            assertThat(first.type).isEqualTo(TransactionType.INCOME)
        }

    @Test
    fun `drill into a deleted category backs out to the root`() =
        runTest(testDispatcher) {
            // Keys mirror the private reports_* keys of ReportsViewModel.
            val handle = SavedStateHandle(mapOf("reports_drill_parent" to 10L))
            val viewModel = observeState(createViewModel(handle))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.drillDownParent).isNull()
            assertThat(state.breakdown.parentId).isNull()
        }

    @Test
    fun `query error shows hasError and retry recovers`() =
        runTest(testDispatcher) {
            val flaky = FlakyReportRepository(reportRepository)
            flaky.fail = true
            val viewModel = observeState(createViewModel(report = flaky))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.hasError).isTrue()

            flaky.fail = false
            reportRepository.setInitialData(
                dailyIncomeExpense = listOf(DailyIncomeExpense(today, Money(10_000L), Money(5_000L))),
            )
            val afterRetry = mutableListOf<ReportsUiState>()
            backgroundScope.launch { viewModel.uiState.collect { afterRetry += it } }
            viewModel.retry()
            advanceUntilIdle()

            // Until the new data arrives the error stays on screen: no flash of the empty state.
            assertThat(afterRetry.filter { !it.hasError && !it.isRefreshing }.all { it.monthlyTotals.size == 6 })
                .isTrue()
            val state = viewModel.uiState.value
            assertThat(state.hasError).isFalse()
            assertThat(state.isRefreshing).isFalse()
            assertThat(state.summary.total).isEqualTo(Money(5_000L))
            assertThat(state.monthlyTotals).hasSize(6)
        }

    @Test
    fun `BALANCE loads balance data without breakdown or pace`() =
        runTest(testDispatcher) {
            // No accounts and no rows: the real use cases also answer EMPTY here.
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            reportRepository.categoryRangeRequests.clear()

            viewModel.setFocus(ReportFocus.BALANCE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.BALANCE)
            assertThat(state.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(state.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(state.balanceEvolution).isEqualTo(BalanceEvolution.EMPTY)
            assertThat(state.monthlyTotals).hasSize(6)
            assertThat(state.monthlyTotals.all { it.income.isZero && it.expense.isZero }).isTrue()
            assertThat(state.breakdown.total.isZero).isTrue()
            assertThat(state.breakdown.slices).isEmpty()
            assertThat(state.pace).isNull()
            assertThat(state.isEmpty).isTrue()
            assertThat(reportRepository.categoryRangeRequests).isEmpty()
        }

    @Test
    fun `BALANCE queries balance deltas with the period accounts and today`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash, savings))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            reportRepository.balanceRequests.clear()

            viewModel.setFocus(ReportFocus.BALANCE)
            advanceUntilIdle()

            assertThat(reportRepository.balanceRequests).containsExactly(
                Triple(setOf(1L, 2L), LocalDate.of(2026, 9, 1), today),
            )
        }

    @Test
    fun `BALANCE keeps the account filter`() =
        runTest(testDispatcher) {
            // Unknown account: the real evolution use case also answers EMPTY for it in MONTH mode.
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()
            viewModel.setAccount(99L)
            viewModel.setFocus(ReportFocus.BALANCE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.BALANCE)
            assertThat(state.accountId).isEqualTo(99L)
            assertThat(state.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(state.balanceEvolution).isEqualTo(BalanceEvolution.EMPTY)
            assertThat(state.monthlyTotals).hasSize(6)
            assertThat(reportRepository.dailyAccountRequests).contains(99L)
        }

    @Test
    fun `back to EXPENSE restores expenses and clears balance data`() =
        runTest(testDispatcher) {
            reportRepository.setInitialData(
                categoryTotalsByType =
                    mapOf(
                        TransactionType.EXPENSE to listOf(categoryRow(10L, "Alimentación", null, null, 700L)),
                    ),
                dailyIncomeExpense = listOf(DailyIncomeExpense(today, Money.ZERO, Money(700L))),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.setFocus(ReportFocus.BALANCE)
            advanceUntilIdle()
            viewModel.setFocus(ReportFocus.EXPENSE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.EXPENSE)
            assertThat(state.summary.total).isEqualTo(Money(700L))
            assertThat(state.breakdown.total).isEqualTo(Money(700L))
            assertThat(state.pace).isNotNull()
            assertThat(state.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(state.balanceEvolution).isNull()
            assertThat(state.isEmpty).isFalse()
        }

    @Test
    fun `BALANCE focus is restored from saved state`() =
        runTest(testDispatcher) {
            // Keys mirror the private reports_* keys of ReportsViewModel.
            val handle = SavedStateHandle(mapOf("reports_type" to ReportFocus.BALANCE.name))
            val viewModel = createViewModel(handle)
            assertThat(viewModel.uiState.value.focus).isEqualTo(ReportFocus.BALANCE)

            observeState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.BALANCE)
            assertThat(state.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(state.balanceEvolution).isEqualTo(BalanceEvolution.EMPTY)
            assertThat(state.monthlyTotals).hasSize(6)
        }

    @Test
    fun `unknown saved focus falls back to EXPENSE`() =
        runTest(testDispatcher) {
            listOf("GASTOS", TransactionType.TRANSFER.name).forEach { raw ->
                // Keys mirror the private reports_* keys of ReportsViewModel.
                val handle = SavedStateHandle(mapOf("reports_type" to raw))
                val viewModel = observeState(createViewModel(handle))
                advanceUntilIdle()
                assertThat(viewModel.uiState.value.focus).isEqualTo(ReportFocus.EXPENSE)
            }
        }

    @Test
    fun `switching year to month keeps the BALANCE focus`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.setFocus(ReportFocus.BALANCE)
            viewModel.setMode(ReportMode.YEAR)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.focus).isEqualTo(ReportFocus.BALANCE)
            assertThat(viewModel.uiState.value.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(viewModel.uiState.value.monthlyTotals).hasSize(12)

            viewModel.setMode(ReportMode.MONTH)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertThat(state.focus).isEqualTo(ReportFocus.BALANCE)
            assertThat(state.balanceSummary).isEqualTo(BalanceSummary.EMPTY)
            assertThat(state.balanceEvolution).isEqualTo(BalanceEvolution.EMPTY)
            assertThat(state.monthlyTotals).hasSize(6)
        }

    @Test
    fun `changing focus never pairs new controls with old data unless refreshing`() =
        runTest(testDispatcher) {
            reportRepository.setInitialData(
                dailyIncomeExpense = listOf(DailyIncomeExpense(today, Money(10_000L), Money(9_000L))),
            )
            val viewModel = createViewModel()
            viewModel.uiState.test {
                var current = awaitItem()
                while (current.isLoading) current = awaitItem()
                val loaded = current
                assertThat(loaded.isLoading).isFalse()
                assertThat(loaded.isRefreshing).isFalse()
                assertThat(loaded.focus).isEqualTo(ReportFocus.EXPENSE)
                assertThat(loaded.summary.total).isEqualTo(Money(9_000L))

                viewModel.setFocus(ReportFocus.BALANCE)

                val seen = mutableListOf<ReportsUiState>()
                var settled: ReportsUiState? = null
                while (settled == null) {
                    val state = awaitItem()
                    seen += state
                    if (!state.isLoading && state.focus == ReportFocus.BALANCE && !state.isRefreshing) {
                        settled = state
                    }
                }
                val done = checkNotNull(settled)
                assertThat(done.summary.total).isEqualTo(Money.ZERO)
                assertThat(done.breakdown.total.isZero).isTrue()
                assertThat(done.pace).isNull()
                assertThat(done.monthlyTotals).hasSize(6)

                val refreshing = seen.filter { it.isRefreshing }
                assertThat(refreshing).isNotEmpty()
                refreshing.forEach { state ->
                    // New controls (BALANCE) over the previous complete snapshot (EXPENSE).
                    assertThat(state.isLoading).isFalse()
                    assertThat(state.focus).isEqualTo(ReportFocus.BALANCE)
                    assertThat(state.summary.total).isEqualTo(Money(9_000L))
                }
            }
        }

    @Test
    fun `BALANCE isEmpty needs zero months and a still evolution`() {
        val zeroMonths = listOf(MonthTotals(YearMonth.of(2026, 9), Money.ZERO, Money.ZERO))
        val movingMonths = listOf(MonthTotals(YearMonth.of(2026, 9), Money(1_000L), Money.ZERO))
        val flatZero =
            BalanceEvolution(
                granularity = BalanceGranularity.DAILY,
                opening = Money.ZERO,
                points = listOf(BalancePoint(LocalDate.of(2026, 9, 28), Money.ZERO)),
            )
        // Initial balance without movements: a flat non-zero line is shown, not the empty state.
        val flatNonZero =
            BalanceEvolution(
                granularity = BalanceGranularity.DAILY,
                opening = Money(5_000L),
                points =
                    listOf(
                        BalancePoint(LocalDate.of(2026, 9, 27), Money(5_000L)),
                        BalancePoint(LocalDate.of(2026, 9, 28), Money(5_000L)),
                    ),
            )
        val moving =
            BalanceEvolution(
                granularity = BalanceGranularity.DAILY,
                opening = Money.ZERO,
                points =
                    listOf(
                        BalancePoint(LocalDate.of(2026, 9, 27), Money.ZERO),
                        BalancePoint(LocalDate.of(2026, 9, 28), Money(1_000L)),
                    ),
            )

        fun emptyOf(
            focus: ReportFocus,
            monthlyTotals: List<MonthTotals>,
            evolution: BalanceEvolution?,
        ) = ReportsUiState(isLoading = false, focus = focus, monthlyTotals = monthlyTotals, balanceEvolution = evolution).isEmpty

        assertThat(emptyOf(ReportFocus.BALANCE, zeroMonths, BalanceEvolution.EMPTY)).isTrue()
        assertThat(emptyOf(ReportFocus.BALANCE, zeroMonths, flatZero)).isTrue()
        assertThat(emptyOf(ReportFocus.BALANCE, zeroMonths, null)).isTrue()
        assertThat(emptyOf(ReportFocus.BALANCE, zeroMonths, flatNonZero)).isFalse()
        assertThat(emptyOf(ReportFocus.BALANCE, zeroMonths, moving)).isFalse()
        assertThat(emptyOf(ReportFocus.BALANCE, movingMonths, BalanceEvolution.EMPTY)).isFalse()
        assertThat(emptyOf(ReportFocus.EXPENSE, zeroMonths, null)).isTrue()
        assertThat(emptyOf(ReportFocus.EXPENSE, movingMonths, null)).isFalse()
        assertThat(ReportsUiState(isLoading = true, focus = ReportFocus.BALANCE).isEmpty).isFalse()
        assertThat(ReportsUiState(isLoading = false, hasError = true, focus = ReportFocus.BALANCE).isEmpty).isFalse()
    }
}

/** [ReportRepository] that throws on demand; used for the error and retry test. */
private class FlakyReportRepository(
    private val delegate: FakeReportRepository,
) : ReportRepository by delegate {
    var fail: Boolean = false

    override fun observeCategoryTotals(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
        accountId: Long?,
    ): Flow<List<CategoryTotal>> =
        if (fail) {
            flow { throw IllegalStateException("reports query failed") }
        } else {
            delegate.observeCategoryTotals(from, to, type, accountId)
        }

    override fun observeDailyIncomeExpense(
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): Flow<List<DailyIncomeExpense>> =
        if (fail) {
            flow { throw IllegalStateException("reports query failed") }
        } else {
            delegate.observeDailyIncomeExpense(from, to, accountId)
        }
}
