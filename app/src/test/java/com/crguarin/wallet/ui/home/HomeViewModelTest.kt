package com.crguarin.wallet.ui.home

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var timeProvider: FakeTimeProvider

    private val cash =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(100_000L),
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
            initialBalance = Money(200_000L),
            color = 2,
            icon = "savings",
            sortOrder = 1,
            createdAt = baseInstant,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(baseInstant)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        HomeViewModel(
            observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository),
            observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository),
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            transactionRepository = transactionRepository,
            timeProvider = timeProvider,
        )

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        date: LocalDate = today,
        accountId: Long = 1L,
        categoryId: Long? = 10L,
        createdAt: Instant = baseInstant,
    ) = Transaction(
        id = id,
        type = type,
        amount = Money(amount),
        accountId = accountId,
        categoryId = categoryId,
        date = date,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun TestScope.observeState(viewModel: HomeViewModel): HomeViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `combines total balance current month summary accounts and recent transactions`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(cash, savings),
                balanceMap = mapOf(1L to Money(100_000L), 2L to Money(200_000L)),
            )
            transactionRepository.setInitialData(
                listOf(
                    transaction(id = 1L, type = TransactionType.INCOME, amount = 500_000L, categoryId = 20L),
                    transaction(id = 2L, type = TransactionType.EXPENSE, amount = 120_000L),
                    transaction(id = 3L, type = TransactionType.TRANSFER, amount = 50_000L, categoryId = null),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.totalBalance).isEqualTo(Money(300_000L))
            assertThat(state.monthSummary.income).isEqualTo(Money(500_000L))
            assertThat(state.monthSummary.expense).isEqualTo(Money(120_000L))
            assertThat(state.monthSummary.net).isEqualTo(Money(380_000L))
            assertThat(state.accounts).hasSize(2)
            assertThat(state.recentTransactions).hasSize(3)
        }

    @Test
    fun `recent transactions are limited to ten`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                (1L..12L).map { id ->
                    transaction(
                        id = id,
                        type = TransactionType.EXPENSE,
                        amount = 10_000L,
                        createdAt = Instant.ofEpochMilli(id),
                    )
                },
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recentTransactions).hasSize(HOME_RECENT_LIMIT)
        }

    @Test
    fun `transfer quick action is enabled only with at least two active accounts`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cash))
            val single = observeState(createViewModel())
            advanceUntilIdle()
            assertThat(single.uiState.value.canTransfer).isFalse()

            accountRepository.saveAccount(savings)
            advanceUntilIdle()
            assertThat(single.uiState.value.canTransfer).isTrue()
        }

    @Test
    fun `month summary only counts the current month`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                listOf(
                    transaction(id = 1L, type = TransactionType.EXPENSE, amount = 1_000L, date = today),
                    transaction(id = 2L, type = TransactionType.EXPENSE, amount = 2_000L, date = today.minusMonths(1)),
                ),
            )
            val viewModel = observeState(createViewModel())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.monthSummary.expense).isEqualTo(Money(1_000L))
        }
}
