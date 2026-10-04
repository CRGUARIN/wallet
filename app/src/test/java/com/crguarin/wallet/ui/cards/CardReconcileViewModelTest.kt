package com.crguarin.wallet.ui.cards

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.card.ApplyReconciliationUseCase
import com.crguarin.wallet.domain.usecase.card.PreviewReconciliationUseCase
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
class CardReconcileViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var cardStatementRepository: FakeCardStatementRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var previewUseCase: PreviewReconciliationUseCase
    private lateinit var applyUseCase: ApplyReconciliationUseCase

    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val closingDate = LocalDate.of(2026, 9, 15)

    private val cardAccount =
        Account(
            id = 1L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money.ZERO,
            color = 1,
            icon = "credit_card",
            createdAt = baseInstant,
            creditLimit = Money(5_000_000L),
            statementDay = 15,
            paymentDueDay = 5,
        )
    private val foodCategory =
        Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")

    private fun purchase(
        id: Long = 20L,
        amount: Money = Money(1_000_000L),
        date: LocalDate = LocalDate.of(2026, 9, 10),
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = amount,
        accountId = 1L,
        categoryId = 10L,
        date = date,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        cardStatementRepository = FakeCardStatementRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        timeProvider = FakeTimeProvider(baseInstant)

        accountRepository.setInitialData(listOf(cardAccount))
        categoryRepository.setInitialData(listOf(foodCategory))

        val checkNegativeBalanceUseCase = CheckNegativeBalanceUseCase(accountRepository, transactionRepository)
        val saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = timeProvider,
                checkNegativeBalanceUseCase = checkNegativeBalanceUseCase,
            )
        previewUseCase = PreviewReconciliationUseCase(accountRepository, cardStatementRepository, timeProvider)
        applyUseCase =
            ApplyReconciliationUseCase(
                cardStatementRepository = cardStatementRepository,
                categoryRepository = categoryRepository,
                saveTransactionUseCase = saveTransactionUseCase,
                timeProvider = timeProvider,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))) =
        CardReconcileViewModel(
            savedStateHandle = savedStateHandle,
            accountRepository = accountRepository,
            previewReconciliationUseCase = previewUseCase,
            applyReconciliationUseCase = applyUseCase,
            timeProvider = timeProvider,
        )

    @Test
    fun `defaults are the last closing and its due date`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.notFound).isFalse()
            assertThat(state.accountName).isEqualTo("RappiCard")
            assertThat(state.closingDate).isEqualTo(closingDate)
            assertThat(state.dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        }

    @Test
    fun `missing or non card accounts are reported as not found`() =
        runTest(testDispatcher) {
            val missing = createViewModel(SavedStateHandle(mapOf("accountId" to 999L)))
            assertThat(missing.uiState.value.notFound).isTrue()

            accountRepository.setInitialData(
                listOf(cardAccount.copy(id = 7L, type = AccountType.CASH, creditLimit = null)),
            )
            val notACard = createViewModel(SavedStateHandle(mapOf("accountId" to 7L)))
            assertThat(notACard.uiState.value.notFound).isTrue()
        }

    @Test
    fun `positive difference proposes interest and saves an expense adjustment`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("11000")

            viewModel.calculatePreview()
            val preview = viewModel.uiState.value.preview
            assertThat(preview).isNotNull()
            assertThat(preview!!.difference).isEqualTo(Money(100_000L))
            assertThat(preview.interestMismatch).isFalse()

            viewModel.events.test {
                viewModel.save("Extracto 15 sep", false)
                assertThat(awaitItem()).isEqualTo(CardReconcileEvent.ReconciliationSaved)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = cardStatementRepository.observeLastStatement(1L)
            var statementId = 0L
            saved.test {
                statementId = (awaitItem()?.id ?: 0L)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(statementId).isGreaterThan(0L)
        }

    @Test
    fun `matching figures save the statement without any adjustment`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("10000")

            viewModel.calculatePreview()
            val preview = viewModel.uiState.value.preview
            assertThat(preview?.difference).isEqualTo(Money.ZERO)

            viewModel.events.test {
                viewModel.save("Extracto 15 sep", false)
                assertThat(awaitItem()).isEqualTo(CardReconcileEvent.ReconciliationSaved)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `negative difference saves without adjustment unless forced`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("9000")

            viewModel.calculatePreview()
            val preview = viewModel.uiState.value.preview
            assertThat(preview).isNotNull()
            assertThat(preview!!.difference).isEqualTo(Money(-100_000L))

            viewModel.events.test {
                viewModel.save("Extracto 15 sep", false)
                assertThat(awaitItem()).isEqualTo(CardReconcileEvent.ReconciliationSaved)
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.save("Extracto 15 sep", true)
        }

    @Test
    fun `interest mismatch is reported when the positive difference differs`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("11000")
            viewModel.setInterestText("500")

            viewModel.calculatePreview()

            val preview = viewModel.uiState.value.preview
            assertThat(preview?.interestMismatch).isTrue()
        }

    @Test
    fun `reconciling the same closing twice updates the existing statement`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("10000")
            viewModel.calculatePreview()

            viewModel.events.test {
                viewModel.save("Extracto 15 sep", false)
                assertThat(awaitItem()).isEqualTo(CardReconcileEvent.ReconciliationSaved)
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.calculatePreview()
            viewModel.events.test {
                viewModel.save("Extracto 15 sep", false)
                assertThat(awaitItem()).isEqualTo(CardReconcileEvent.ReconciliationSaved)
                cancelAndIgnoreRemainingEvents()
            }

            cardStatementRepository.observeStatements(1L).test {
                assertThat(awaitItem()).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saving recalculates a stale preview instead of saving the old difference`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("9000")
            viewModel.calculatePreview()
            assertThat(
                viewModel.uiState.value.preview
                    ?.difference,
            ).isEqualTo(Money(-100_000L))

            // While reviewing the cycle, the user records the missing refund.
            val refund =
                purchase(id = 21L, amount = Money(100_000L), date = LocalDate.of(2026, 9, 12))
                    .copy(type = TransactionType.INCOME, categoryId = null)
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase(), refund))

            viewModel.events.test {
                viewModel.save("Extracto 15 sep", true)
                expectNoEvents()
            }
            val state = viewModel.uiState.value
            assertThat(state.preview?.difference).isEqualTo(Money.ZERO)
            assertThat(state.generalErrorRes).isEqualTo(R.string.card_reconcile_recalculated)
            assertThat(state.isSaving).isFalse()
            cardStatementRepository.observeStatements(1L).test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `calculate without a valid balance shows an error`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            assertThat(viewModel.uiState.value.isCalculateEnabled).isFalse()

            viewModel.setStatementBalanceText("abc")
            viewModel.calculatePreview()

            assertThat(viewModel.uiState.value.preview).isNull()
            assertThat(viewModel.uiState.value.statementBalanceErrorRes).isNotNull()
        }

    @Test
    fun `form entries survive process death through SavedStateHandle`() =
        runTest(testDispatcher) {
            val handle = SavedStateHandle(mapOf("accountId" to 1L))
            val first = createViewModel(handle)
            first.setStatementBalanceText("11000")
            first.setMinimumPaymentText("500")
            first.setInterestText("300")
            first.setClosingDate(LocalDate.of(2026, 8, 15))

            val restored = createViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))

            val state = restored.uiState.value
            assertThat(state.statementBalanceText).isEqualTo("11000")
            assertThat(state.minimumPaymentText).isEqualTo("500")
            assertThat(state.interestText).isEqualTo("300")
            assertThat(state.closingDate).isEqualTo(LocalDate.of(2026, 8, 15))
            assertThat(state.dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        }

    @Test
    fun `changing an entry clears a stale preview`() =
        runTest(testDispatcher) {
            cardStatementRepository.setInitialData(emptyList(), listOf(purchase()))
            val viewModel = createViewModel()
            viewModel.setStatementBalanceText("11000")
            viewModel.calculatePreview()
            assertThat(viewModel.uiState.value.preview).isNotNull()

            viewModel.setStatementBalanceText("12000")
            assertThat(viewModel.uiState.value.preview).isNull()
        }
}
