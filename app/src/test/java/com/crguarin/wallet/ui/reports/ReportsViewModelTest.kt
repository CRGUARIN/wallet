package com.crguarin.wallet.ui.reports

import androidx.lifecycle.SavedStateHandle
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
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveCategoryBreakdownUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveMonthlyTotalsUseCase
import com.crguarin.wallet.domain.usecase.report.ObservePeriodSummaryUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveSpendingPaceUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        ReportsViewModel(
            savedStateHandle = handle,
            categoryRepository = categoryRepository,
            observeCategoryBreakdownUseCase = ObserveCategoryBreakdownUseCase(reportRepository),
            observeMonthlyTotalsUseCase = ObserveMonthlyTotalsUseCase(reportRepository),
            observePeriodSummaryUseCase = ObservePeriodSummaryUseCase(reportRepository),
            observeSpendingPaceUseCase = ObserveSpendingPaceUseCase(reportRepository),
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
    fun `changing the type clears the selection and the drill down`() =
        runTest(testDispatcher) {
            categoryRepository.setInitialData(listOf(food))
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.drillInto(10L)
            viewModel.selectSlice(SliceKey.Category(10L))
            advanceUntilIdle()

            viewModel.setType(TransactionType.INCOME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
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
            first.setType(TransactionType.INCOME)
            first.setAccount(2L)
            advanceUntilIdle()

            val recreated = observeState(createViewModel(handle))
            advanceUntilIdle()

            val state = recreated.uiState.value
            assertThat(state.mode).isEqualTo(ReportMode.YEAR)
            assertThat(state.yearMonth).isEqualTo(YearMonth.of(2025, 9))
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

            viewModel.setType(TransactionType.INCOME)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isEmpty).isFalse()
            assertThat(viewModel.uiState.value.hasNoDataOfType).isTrue()
        }
}
