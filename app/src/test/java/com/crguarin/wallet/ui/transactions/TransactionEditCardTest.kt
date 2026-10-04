package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
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
class TransactionEditCardTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var checkNegativeBalanceUseCase: CheckNegativeBalanceUseCase

    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val baseDate = LocalDate.of(2026, 9, 28)

    private val cashAccount =
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
    private val cardAccount =
        Account(
            id = 2L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money.ZERO,
            color = 2,
            icon = "credit_card",
            sortOrder = 1,
            createdAt = baseInstant,
            creditLimit = Money(1_000_000L),
            statementDay = 15,
            paymentDueDay = 5,
        )
    private val foodCategory =
        Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        preferencesRepository = FakePreferencesRepository()
        timeProvider = FakeTimeProvider(baseInstant)

        accountRepository.setInitialData(listOf(cashAccount, cardAccount))
        categoryRepository.setInitialData(listOf(foodCategory))

        checkNegativeBalanceUseCase = CheckNegativeBalanceUseCase(accountRepository, transactionRepository)
        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = timeProvider,
                checkNegativeBalanceUseCase = checkNegativeBalanceUseCase,
            )
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle): TransactionEditViewModel =
        TransactionEditViewModel(
            savedStateHandle = savedStateHandle,
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            preferencesRepository = preferencesRepository,
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            observeCategoryTreeUseCase = ObserveCategoryTreeUseCase(categoryRepository),
            saveTransactionUseCase = saveTransactionUseCase,
            deleteTransactionUseCase = deleteTransactionUseCase,
            checkNegativeBalanceUseCase = checkNegativeBalanceUseCase,
            timeProvider = timeProvider,
        )

    private fun creationHandle(
        accountId: Long? = null,
        initialType: TransactionType = TransactionType.EXPENSE,
        toAccountId: Long? = null,
        initialAmountMinor: Long? = null,
    ): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                "transactionId" to null,
                "initialType" to initialType,
                "accountId" to accountId,
                "toAccountId" to toAccountId,
                "initialAmountMinor" to initialAmountMinor,
            ),
        )

    @Test
    fun `installments field is only visible for card expenses`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))

            assertThat(viewModel.uiState.value.showInstallments).isFalse()

            viewModel.setAccountId(2L)
            assertThat(viewModel.uiState.value.showInstallments).isTrue()

            viewModel.setType(TransactionType.INCOME)
            assertThat(viewModel.uiState.value.showInstallments).isFalse()

            viewModel.setType(TransactionType.TRANSFER)
            assertThat(viewModel.uiState.value.showInstallments).isFalse()
        }

    @Test
    fun `changing account or type clears the installments`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setInstallments(12)
            assertThat(viewModel.uiState.value.installments).isEqualTo(12)

            viewModel.setAccountId(1L)
            assertThat(viewModel.uiState.value.installments).isEqualTo(1)

            viewModel.setAccountId(2L)
            viewModel.setInstallments(6)
            viewModel.setType(TransactionType.INCOME)
            assertThat(viewModel.uiState.value.installments).isEqualTo(1)
        }

    @Test
    fun `installments are clamped to 1 to 48`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))

            viewModel.setInstallments(99)
            assertThat(viewModel.uiState.value.installments).isEqualTo(48)

            viewModel.setInstallments(0)
            assertThat(viewModel.uiState.value.installments).isEqualTo(1)
        }

    @Test
    fun `saving a card purchase keeps the installments`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("1000")
            viewModel.setCategoryId(10L)
            viewModel.setInstallments(3)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(transactionRepository.getTransaction(1L)?.installments).isEqualTo(3)
        }

    @Test
    fun `a single installment is saved as null`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("1000")
            viewModel.setCategoryId(10L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(transactionRepository.getTransaction(1L)?.installments).isNull()
        }

    @Test
    fun `income on a card never saves installments`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                listOf(
                    Transaction(
                        id = 10L,
                        type = TransactionType.INCOME,
                        amount = Money(150_000L),
                        accountId = 2L,
                        categoryId = 10L,
                        date = baseDate,
                        createdAt = baseInstant,
                        updatedAt = baseInstant,
                    ),
                ),
            )
            val viewModel =
                createViewModel(
                    SavedStateHandle(
                        mapOf(
                            "transactionId" to 10L,
                            "initialType" to TransactionType.EXPENSE,
                            "accountId" to null,
                            "toAccountId" to null,
                            "initialAmountMinor" to null,
                        ),
                    ),
                )

            assertThat(viewModel.uiState.value.showInstallments).isFalse()
            assertThat(viewModel.uiState.value.installments).isEqualTo(1)
        }

    @Test
    fun `route prefill sets destination and amount only for new transactions`() =
        runTest(testDispatcher) {
            val viewModel =
                createViewModel(
                    creationHandle(
                        accountId = 1L,
                        initialType = TransactionType.TRANSFER,
                        toAccountId = 2L,
                        initialAmountMinor = 150_050L,
                    ),
                )

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isFalse()
            assertThat(state.toAccountId).isEqualTo(2L)
            assertThat(state.amountText).isNotEmpty()
        }

    @Test
    fun `route prefill does not override restored user input`() =
        runTest(testDispatcher) {
            val handle =
                creationHandle(
                    accountId = 1L,
                    initialType = TransactionType.TRANSFER,
                    toAccountId = 2L,
                    initialAmountMinor = 150_050L,
                )
            createViewModel(handle).setAmountText("999")

            val restored =
                createViewModel(
                    SavedStateHandle(
                        handle.keys().associateWith { handle.get<Any?>(it) } +
                            mapOf(
                                "transactionId" to null,
                                "initialType" to TransactionType.TRANSFER,
                                "accountId" to 1L,
                                "toAccountId" to 2L,
                                "initialAmountMinor" to 150_050L,
                            ),
                    ),
                )

            assertThat(restored.uiState.value.amountText).isEqualTo("999")
        }

    @Test
    fun `going over the credit limit shows the card dialog`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("15000")
            viewModel.setCategoryId(10L)

            viewModel.onSaveClick()

            val state = viewModel.uiState.value
            assertThat(state.showNegativeBalanceDialog).isTrue()
            assertThat(state.balanceWarning).isEqualTo(BalanceWarning.OVER_CREDIT_LIMIT)
            assertThat(transactionRepository.getTransaction(1L)).isNull()
        }

    @Test
    fun `confirming the over limit dialog saves the card purchase`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("15000")
            viewModel.setCategoryId(10L)

            viewModel.onSaveClick()
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmNegativeBalanceSave()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(transactionRepository.getTransaction(1L)?.accountId).isEqualTo(2L)
        }

    @Test
    fun `a purchase within the limit saves without any dialog`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("1000")
            viewModel.setCategoryId(10L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isFalse()
            assertThat(viewModel.uiState.value.balanceWarning).isEqualTo(BalanceWarning.NONE)
        }
}
