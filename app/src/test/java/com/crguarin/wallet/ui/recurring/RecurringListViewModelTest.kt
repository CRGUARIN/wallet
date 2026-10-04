package com.crguarin.wallet.ui.recurring

import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.recurring.ConfirmRecurringOccurrenceUseCase
import com.crguarin.wallet.domain.usecase.recurring.ObserveRecurringOverviewUseCase
import com.crguarin.wallet.domain.usecase.recurring.SkipRecurringOccurrenceUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class RecurringListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var timeProvider: FakeTimeProvider

    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

    private val savings =
        Account(
            id = 1L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(1_000_000_00L),
            color = 1,
            icon = "savings",
            createdAt = baseInstant,
        )
    private val card =
        Account(
            id = 2L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money.ZERO,
            color = 2,
            icon = "credit_card",
            createdAt = baseInstant,
            creditLimit = Money(5_000_000_00L),
            statementDay = 15,
            paymentDueDay = 5,
        )
    private val expenseCategory =
        Category(id = 10L, name = "Suscripciones", type = CategoryType.EXPENSE, color = 1, icon = "movie")
    private val incomeCategory =
        Category(id = 20L, name = "Salario", type = CategoryType.INCOME, color = 2, icon = "payments")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(baseInstant)
        accountRepository.setInitialData(listOf(savings, card))
        categoryRepository.setInitialData(listOf(expenseCategory, incomeCategory))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun payment(
        id: Long,
        name: String = "Netflix",
        type: TransactionType = TransactionType.EXPENSE,
        amount: Money = Money(4_590_000L),
        accountId: Long = 1L,
        categoryId: Long = 10L,
        startDate: LocalDate = LocalDate.of(2026, 8, 28),
        nextDueDate: LocalDate = LocalDate.of(2026, 10, 28),
        mode: RecurringMode = RecurringMode.AUTO,
        isActive: Boolean = true,
    ) = RecurringPayment(
        id = id,
        name = name,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = categoryId,
        frequency = RecurrenceFrequency.MONTHLY,
        interval = 1,
        startDate = startDate,
        nextDueDate = nextDueDate,
        mode = mode,
        isActive = isActive,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    private fun createViewModel(): RecurringListViewModel {
        val checkNegativeBalance = CheckNegativeBalanceUseCase(accountRepository, transactionRepository)
        val saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = timeProvider,
                checkNegativeBalanceUseCase = checkNegativeBalance,
            )
        return RecurringListViewModel(
            observeRecurringOverviewUseCase =
                ObserveRecurringOverviewUseCase(
                    recurringRepository = recurringRepository,
                    accountRepository = accountRepository,
                    categoryRepository = categoryRepository,
                    timeProvider = timeProvider,
                ),
            confirmRecurringOccurrenceUseCase =
                ConfirmRecurringOccurrenceUseCase(
                    recurringRepository = recurringRepository,
                    accountRepository = accountRepository,
                    categoryRepository = categoryRepository,
                    saveTransactionUseCase = saveTransactionUseCase,
                    timeProvider = timeProvider,
                ),
            skipRecurringOccurrenceUseCase = SkipRecurringOccurrenceUseCase(recurringRepository, timeProvider),
            timeProvider = timeProvider,
        )
    }

    @Test
    fun `overview shows the three sections and the monthly totals`() {
        recurringRepository.setInitialData(
            listOf(
                payment(id = 1L, name = "Energía", mode = RecurringMode.CONFIRM, nextDueDate = today),
                payment(id = 2L, name = "Netflix"),
                payment(
                    id = 3L,
                    name = "Salario",
                    type = TransactionType.INCOME,
                    amount = Money(50_000_000L),
                    categoryId = 20L,
                ),
                payment(id = 4L, name = "Gimnasio", isActive = false),
            ),
        )

        val state = createViewModel().uiState.value

        assertThat(state.isLoading).isFalse()
        val overview = state.overview
        assertThat(overview).isNotNull()
        assertThat(overview!!.pending.map { it.payment.id }).containsExactly(1L)
        assertThat(overview.active.map { it.payment.id }).containsExactly(2L, 3L)
        assertThat(overview.inactive.map { it.payment.id }).containsExactly(4L)
        // The pending CONFIRM expense counts too: 4.590.000 + 4.590.000.
        assertThat(overview.monthlyExpense).isEqualTo(Money(9_180_000L))
        assertThat(overview.monthlyIncome).isEqualTo(Money(50_000_000L))
        assertThat(overview.pending.first().pendingCount).isEqualTo(1)
    }

    @Test
    fun `confirming a pending charge records it and emits the message`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(
                listOf(
                    payment(
                        id = 10L,
                        name = "Energía",
                        mode = RecurringMode.CONFIRM,
                        amount = Money(4_590_000L),
                        startDate = LocalDate.of(2026, 8, 28),
                        nextDueDate = today,
                    ),
                ),
            )
            val viewModel = createViewModel()
            val item =
                viewModel.uiState.value.overview!!
                    .pending
                    .single()

            viewModel.openConfirm(item)
            assertThat(viewModel.uiState.value.confirmAmountText).isEqualTo("45900")
            assertThat(viewModel.uiState.value.confirmDate).isEqualTo(today)

            viewModel.setConfirmAmountText("50000")
            viewModel.setConfirmDate(today.plusDays(3))
            viewModel.events.test {
                viewModel.confirmRecord()
                assertThat(awaitItem())
                    .isEqualTo(RecurringListEvent.ShowMessage(R.string.recurring_recorded))
                cancelAndIgnoreRemainingEvents()
            }

            val saved = recurringRepository.get(10L)
            assertThat(saved!!.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 28))
            val recorded = recurringRepository.completedTransactions.single()
            assertThat(recorded.amount).isEqualTo(Money(5_000_000L))
            assertThat(recorded.date).isEqualTo(today.plusDays(3))
            assertThat(recorded.recurringId).isEqualTo(10L)
            assertThat(recorded.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(recorded.accountId).isEqualTo(1L)
            assertThat(recorded.categoryId).isEqualTo(10L)
            assertThat(viewModel.uiState.value.confirmItem).isNull()
        }

    @Test
    fun `skipping advances the next due date without recording anything`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(
                listOf(
                    payment(
                        id = 10L,
                        name = "Energía",
                        mode = RecurringMode.CONFIRM,
                        startDate = LocalDate.of(2026, 8, 28),
                        nextDueDate = today,
                    ),
                ),
            )
            val viewModel = createViewModel()
            val item =
                viewModel.uiState.value.overview!!
                    .pending
                    .single()

            viewModel.openSkip(item)
            assertThat(viewModel.uiState.value.skipItem).isEqualTo(item)
            viewModel.confirmSkip()

            assertThat(recurringRepository.get(10L)!!.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 28))
            assertThat(recurringRepository.completedTransactions).isEmpty()
            assertThat(viewModel.uiState.value.skipItem).isNull()
            assertThat(viewModel.uiState.value.isProcessing).isFalse()
        }

    @Test
    fun `invalid confirm amount is reported before calling the use case`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(
                listOf(
                    payment(
                        id = 10L,
                        mode = RecurringMode.CONFIRM,
                        startDate = LocalDate.of(2026, 8, 28),
                        nextDueDate = today,
                    ),
                ),
            )
            val viewModel = createViewModel()
            viewModel.openConfirm(
                viewModel.uiState.value.overview!!
                    .pending
                    .single(),
            )

            viewModel.setConfirmAmountText("abc")
            assertThat(viewModel.uiState.value.confirmAmountErrorRes).isEqualTo(R.string.error_amount_invalid)

            viewModel.confirmRecord()
            assertThat(recurringRepository.get(10L)!!.nextDueDate).isEqualTo(today)
            assertThat(recurringRepository.completedTransactions).isEmpty()
            assertThat(viewModel.uiState.value.confirmItem).isNotNull()
        }
}
