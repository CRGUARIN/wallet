package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.RestoreTransactionUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var transactionRepository: FakeTransactionRepository
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
    private val savings =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(0),
            color = 2,
            icon = "savings",
            sortOrder = 1,
            createdAt = baseInstant,
        )
    private val food =
        Category(id = 10L, systemKey = "food", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")
    private val groceries =
        Category(id = 11L, systemKey = "groceries", type = CategoryType.EXPENSE, parentId = 10L, color = 1, icon = "local_grocery_store")
    private val salary =
        Category(id = 20L, systemKey = "salary", type = CategoryType.INCOME, color = 2, icon = "payments")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(baseInstant)
        accountRepository.setInitialData(listOf(cash, savings))
        categoryRepository.setInitialData(listOf(food, groceries, salary))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(handle: SavedStateHandle = defaultHandle()) =
        TransactionsViewModel(
            savedStateHandle = handle,
            categoryRepository = categoryRepository,
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            observeCategoryTreeUseCase = ObserveCategoryTreeUseCase(categoryRepository),
            observeTransactionsUseCase = ObserveTransactionsUseCase(transactionRepository),
            observeDailyTotalsUseCase = ObserveDailyTotalsUseCase(transactionRepository),
            deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository),
            restoreTransactionUseCase = RestoreTransactionUseCase(transactionRepository),
            timeProvider = timeProvider,
        )

    private fun defaultHandle(
        accountId: Long? = null,
        categoryId: Long? = null,
    ) = SavedStateHandle(
        mapOf(
            "accountId" to accountId,
            "categoryId" to categoryId,
        ),
    )

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        date: LocalDate = today,
        accountId: Long = 1L,
        toAccountId: Long? = null,
        categoryId: Long? = 10L,
        note: String? = null,
        createdAt: Instant = baseInstant,
    ) = Transaction(
        id = id,
        type = type,
        amount = Money(amount),
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        date = date,
        note = note,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun TestScope.observeState(viewModel: TransactionsViewModel): TransactionsViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `starts on the current month and cannot advance past one year`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.month).isEqualTo(YearMonth.of(2026, 9))
            assertThat(state.maxMonth).isEqualTo(YearMonth.of(2027, 9))
            assertThat(state.canGoToNextMonth).isTrue()

            repeat(12) { viewModel.nextMonth() }
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.month).isEqualTo(YearMonth.of(2027, 9))
            assertThat(viewModel.uiState.value.canGoToNextMonth).isFalse()
        }

    @Test
    fun `previous and next month update the selected month`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.previousMonth()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.month).isEqualTo(YearMonth.of(2026, 8))

            viewModel.nextMonth()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.month).isEqualTo(YearMonth.of(2026, 9))
        }

    @Test
    fun `route arguments preload the account and category filters`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel(defaultHandle(accountId = 2L, categoryId = 10L)))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.accountId).isEqualTo(2L)
            assertThat(state.categoryId).isEqualTo(10L)
            assertThat(state.selectedCategory).isEqualTo(food)
            assertThat(state.hasActiveFilters).isTrue()
        }

    @Test
    fun `combined filters are reflected in the state`() =
        runTest(testDispatcher) {
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            viewModel.setType(TransactionType.EXPENSE)
            viewModel.setAccountId(1L)
            viewModel.setCategoryId(11L)
            viewModel.setSearchQuery("mercado")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.categoryId).isEqualTo(11L)
            assertThat(state.selectedParentCategory).isEqualTo(food)
            assertThat(state.searchQuery).isEqualTo("mercado")

            viewModel.clearFilters()
            advanceUntilIdle()
            val cleared = viewModel.uiState.value
            assertThat(cleared.hasActiveFilters).isFalse()
            assertThat(cleared.searchQuery).isEmpty()
        }

    @Test
    fun `changing the month updates the paged date filter`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val job = backgroundScope.launch { viewModel.pagingData.collect {} }
            advanceUntilIdle()

            assertThat(transactionRepository.lastPagedFilter?.startDate).isEqualTo(today.withDayOfMonth(1))
            assertThat(transactionRepository.lastPagedFilter?.endDate).isEqualTo(today.withDayOfMonth(today.lengthOfMonth()))

            viewModel.previousMonth()
            advanceUntilIdle()

            val previousMonth = today.minusMonths(1)
            assertThat(transactionRepository.lastPagedFilter?.startDate).isEqualTo(previousMonth.withDayOfMonth(1))
            assertThat(transactionRepository.lastPagedFilter?.endDate)
                .isEqualTo(previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()))

            job.cancel()
        }

    @Test
    fun `search is debounced before querying the repository`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                listOf(
                    transaction(id = 1L, type = TransactionType.EXPENSE, amount = 100_000L, note = "mercado"),
                    transaction(id = 2L, type = TransactionType.EXPENSE, amount = 200_000L, note = "taxi"),
                ),
            )
            val viewModel = observeState(createViewModel())
            val job = backgroundScope.launch { viewModel.pagingData.collect {} }
            advanceUntilIdle()
            assertThat(transactionRepository.lastPagedFilter?.noteQuery).isNull()

            viewModel.setSearchQuery("mercado")
            runCurrent()
            // The debounce timer has not elapsed yet, so the query was not re-run.
            assertThat(transactionRepository.lastPagedFilter?.noteQuery).isNull()
            assertThat(viewModel.uiState.value.searchQuery).isEqualTo("mercado")

            advanceUntilIdle()
            assertThat(transactionRepository.lastPagedFilter?.noteQuery).isEqualTo("mercado")

            job.cancel()
        }

    @Test
    fun `delete transaction emits an event and undo restores it`() =
        runTest(testDispatcher) {
            val original = transaction(id = 5L, type = TransactionType.EXPENSE, amount = 150_000L, note = "Almuerzo")
            transactionRepository.setInitialData(listOf(original))
            val viewModel = createViewModel()
            val details = transactionRepository.getTransactionDetails(5L)!!

            viewModel.events.test {
                viewModel.deleteTransaction(details)
                assertThat(awaitItem()).isEqualTo(TransactionsEvent.ShowTransactionDeleted)
                assertThat(transactionRepository.getTransaction(5L)).isNull()

                viewModel.undoDelete()
                advanceUntilIdle()
                assertThat(transactionRepository.getTransaction(5L)).isEqualTo(original)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `filters survive recreation through saved state`() =
        runTest(testDispatcher) {
            val handle = defaultHandle()
            val first = observeState(createViewModel(handle))
            advanceUntilIdle()
            first.setType(TransactionType.INCOME)
            first.setSearchQuery("salario")
            advanceUntilIdle()

            val recreated = observeState(createViewModel(handle))
            advanceUntilIdle()

            assertThat(recreated.uiState.value.type).isEqualTo(TransactionType.INCOME)
            assertThat(recreated.uiState.value.searchQuery).isEqualTo("salario")
        }
}
