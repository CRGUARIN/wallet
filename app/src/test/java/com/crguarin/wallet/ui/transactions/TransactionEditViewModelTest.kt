package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
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
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.ui.navigation.NEW_CATEGORY_RESULT_KEY
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditViewModelTest {
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
    private val savingsAccount =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(500_000L),
            color = 2,
            icon = "savings",
            sortOrder = 1,
            createdAt = baseInstant,
        )
    private val archivedAccount =
        Account(
            id = 3L,
            name = "Vieja",
            type = AccountType.CASH,
            initialBalance = Money(10_000L),
            color = 3,
            icon = "wallet",
            isArchived = true,
            sortOrder = 2,
            createdAt = baseInstant,
        )
    private val foodCategory =
        Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")
    private val salaryCategory =
        Category(id = 20L, name = "Salario", type = CategoryType.INCOME, color = 2, icon = "payments")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        preferencesRepository = FakePreferencesRepository()
        timeProvider = FakeTimeProvider(baseInstant)

        accountRepository.setInitialData(listOf(cashAccount, savingsAccount, archivedAccount))
        categoryRepository.setInitialData(listOf(foodCategory, salaryCategory))

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

    private fun createViewModel(savedStateHandle: SavedStateHandle = creationHandle()): TransactionEditViewModel =
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
    ): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                "transactionId" to null,
                "initialType" to initialType,
                "accountId" to accountId,
            ),
        )

    private fun editHandle(transactionId: Long): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                "transactionId" to transactionId,
                "initialType" to TransactionType.EXPENSE,
                "accountId" to null,
            ),
        )

    private fun existingExpense(
        id: Long = 10L,
        amount: Money = Money(150_000L),
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = amount,
        accountId = 1L,
        categoryId = 10L,
        date = baseDate,
        note = "Almuerzo",
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    private fun existingTransfer(
        id: Long = 10L,
        amount: Money = Money(150_000L),
    ) = Transaction(
        id = id,
        type = TransactionType.TRANSFER,
        amount = amount,
        accountId = 1L,
        toAccountId = 2L,
        date = baseDate,
        note = "Almuerzo",
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    @Test
    fun `creation mode defaults to the route account and today's date`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isFalse()
            assertThat(state.accountId).isEqualTo(2L)
            assertThat(state.selectedType).isEqualTo(TransactionType.EXPENSE)
            assertThat(state.date).isEqualTo(baseDate)
            assertThat(state.isSaveEnabled).isFalse()
        }

    @Test
    fun `creation mode prefers the last used account when the route has none`() =
        runTest(testDispatcher) {
            preferencesRepository = FakePreferencesRepository(UserPreferences(lastUsedAccountId = 2L))
            val viewModel = createViewModel()

            assertThat(viewModel.uiState.value.accountId).isEqualTo(2L)
        }

    @Test
    fun `creation mode falls back to the first active account`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            assertThat(viewModel.uiState.value.accountId).isEqualTo(1L)
        }

    @Test
    fun `transfer initial type opens the transfer tab with the route account as origin`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L, initialType = TransactionType.TRANSFER))

            val state = viewModel.uiState.value
            assertThat(state.selectedType).isEqualTo(TransactionType.TRANSFER)
            assertThat(state.accountId).isEqualTo(2L)
            assertThat(state.toAccountId).isNull()
            assertThat(state.isSaveEnabled).isFalse()
        }

    @Test
    fun `transfer save is enabled only with amount origin and a different destination`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L, initialType = TransactionType.TRANSFER))

            viewModel.setAmountText("50000")
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.setToAccountId(1L)
            assertThat(viewModel.uiState.value.toAccountId).isNull()
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.setToAccountId(2L)
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()
        }

    @Test
    fun `changing the origin clears a destination that becomes equal`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L, initialType = TransactionType.TRANSFER))
            viewModel.setAmountText("50000")
            viewModel.setToAccountId(1L)

            viewModel.setAccountId(1L)

            assertThat(viewModel.uiState.value.accountId).isEqualTo(1L)
            assertThat(viewModel.uiState.value.toAccountId).isNull()
        }

    @Test
    fun `swapping origin and destination keeps both accounts`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L, initialType = TransactionType.TRANSFER))
            viewModel.setAmountText("50000")
            viewModel.setToAccountId(1L)

            viewModel.swapAccounts()

            assertThat(viewModel.uiState.value.accountId).isEqualTo(1L)
            assertThat(viewModel.uiState.value.toAccountId).isEqualTo(2L)
        }

    @Test
    fun `changing type clears the category when entering and the destination when leaving transfer`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setAmountText("50000")
            viewModel.setCategoryId(10L)
            viewModel.setDate(baseDate.minusDays(1))
            viewModel.setNote("Nota")

            viewModel.setType(TransactionType.TRANSFER)

            val transferState = viewModel.uiState.value
            assertThat(transferState.selectedType).isEqualTo(TransactionType.TRANSFER)
            assertThat(transferState.categoryId).isNull()
            assertThat(transferState.selectedCategory).isNull()
            assertThat(transferState.amountText).isEqualTo("50000")
            assertThat(transferState.accountId).isEqualTo(1L)
            assertThat(transferState.date).isEqualTo(baseDate.minusDays(1))
            assertThat(transferState.note).isEqualTo("Nota")

            viewModel.setToAccountId(2L)
            viewModel.setType(TransactionType.EXPENSE)

            val expenseState = viewModel.uiState.value
            assertThat(expenseState.selectedType).isEqualTo(TransactionType.EXPENSE)
            assertThat(expenseState.toAccountId).isNull()
            assertThat(expenseState.categoryId).isNull()
            assertThat(expenseState.amountText).isEqualTo("50000")
            assertThat(expenseState.accountId).isEqualTo(1L)
        }

    @Test
    fun `transfer needs a second active account and other tabs keep working`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(cashAccount))
            val viewModel = createViewModel(creationHandle(accountId = 1L, initialType = TransactionType.TRANSFER))

            val transferState = viewModel.uiState.value
            assertThat(transferState.activeAccountCount).isEqualTo(1)
            assertThat(transferState.showTransferNeedsAccounts).isTrue()
            assertThat(transferState.showNoAccounts).isFalse()

            viewModel.setType(TransactionType.EXPENSE)

            assertThat(viewModel.uiState.value.showTransferNeedsAccounts).isFalse()
            assertThat(viewModel.uiState.value.showNoAccounts).isFalse()
        }

    @Test
    fun `changing type clears the category and keeps amount, date, note and account`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setAmountText("50000")
            viewModel.setCategoryId(10L)
            viewModel.setDate(baseDate.minusDays(1))
            viewModel.setNote("Nota")

            viewModel.setType(TransactionType.INCOME)

            val state = viewModel.uiState.value
            assertThat(state.selectedType).isEqualTo(TransactionType.INCOME)
            assertThat(state.categoryId).isNull()
            assertThat(state.selectedCategory).isNull()
            assertThat(state.amountText).isEqualTo("50000")
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.date).isEqualTo(baseDate.minusDays(1))
            assertThat(state.note).isEqualTo("Nota")
        }

    @Test
    fun `validation flags invalid amount, long note and out of range date`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setCategoryId(10L)

            viewModel.setAmountText("abc")
            assertThat(viewModel.uiState.value.amountErrorRes).isEqualTo(R.string.error_amount_invalid)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.setAmountText("50000")
            assertThat(viewModel.uiState.value.amountErrorRes).isNull()
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()

            viewModel.setNote("x".repeat(NOTE_MAX_LENGTH + 1))
            assertThat(viewModel.uiState.value.noteErrorRes).isEqualTo(R.string.error_note_too_long)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.setNote("ok")
            viewModel.setDate(baseDate.plusYears(2))
            assertThat(viewModel.uiState.value.dateErrorRes).isEqualTo(R.string.error_date_out_of_range)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()
        }

    @Test
    fun `creation mode shows the no-accounts state when there are no active accounts`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(emptyList())
            val viewModel = createViewModel(creationHandle(accountId = null))

            assertThat(viewModel.uiState.value.showNoAccounts).isTrue()
        }

    @Test
    fun `save persists the transaction updates last used account and navigates back`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L))
            viewModel.setAmountText("1000")
            viewModel.setCategoryId(10L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = transactionRepository.getTransaction(1L)
            assertThat(saved).isNotNull()
            assertThat(saved?.amount).isEqualTo(Money(100_000L))
            assertThat(saved?.accountId).isEqualTo(2L)
            assertThat(saved?.categoryId).isEqualTo(10L)
            assertThat(saved?.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(preferencesRepository.getUserPreferences().lastUsedAccountId).isEqualTo(2L)
        }

    @Test
    fun `negative balance warning is shown and dismissing keeps the form unchanged`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setAmountText("150000")
            viewModel.setCategoryId(10L)

            viewModel.onSaveClick()
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isTrue()
            assertThat(transactionRepository.getTransaction(1L)).isNull()

            viewModel.dismissNegativeBalanceDialog()
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isFalse()
            assertThat(transactionRepository.getTransaction(1L)).isNull()
        }

    @Test
    fun `confirming the negative balance warning saves the transaction`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setAmountText("150000")
            viewModel.setCategoryId(10L)

            viewModel.onSaveClick()
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmNegativeBalanceSave()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(transactionRepository.getTransaction(1L)?.amount).isEqualTo(Money(15_000_000L))
        }

    @Test
    fun `income never triggers the negative balance warning`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 1L))
            viewModel.setType(TransactionType.INCOME)
            viewModel.setAmountText("150000")
            viewModel.setCategoryId(20L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isFalse()
            assertThat(transactionRepository.getTransaction(1L)?.type).isEqualTo(TransactionType.INCOME)
        }

    @Test
    fun `edit mode preloads all fields from the existing transaction`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingExpense(amount = Money(150_000L))))
            val viewModel = createViewModel(editHandle(10L))

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.editingTransactionId).isEqualTo(10L)
            assertThat(state.amountText).isEqualTo("1500")
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.categoryId).isEqualTo(10L)
            assertThat(state.selectedCategory?.id).isEqualTo(10L)
            assertThat(state.date).isEqualTo(baseDate)
            assertThat(state.note).isEqualTo("Almuerzo")
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `edit mode keeps unsaved edits restored after process death`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingExpense(amount = Money(150_000L))))
            val handle = editHandle(10L)
            createViewModel(handle).setNote("Cena")

            val restored = createViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))

            val state = restored.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.note).isEqualTo("Cena")
            assertThat(state.amountText).isEqualTo("1500")
            assertThat(state.selectedCategory?.id).isEqualTo(10L)
        }

    @Test
    @Config(qualifiers = "es")
    fun `edit mode shows cents with the locale decimal separator`() =
        runTest(testDispatcher) {
            val defaultLocale = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag("es-CO"))
            try {
                transactionRepository.setInitialData(listOf(existingExpense(amount = Money(150_050L))))
                val viewModel = createViewModel(editHandle(10L))

                assertThat(viewModel.uiState.value.amountText).isEqualTo("1500,50")
            } finally {
                Locale.setDefault(defaultLocale)
            }
        }

    @Test
    fun `editing an archived account keeps it selected but the picker only has active accounts`() =
        runTest(testDispatcher) {
            val archivedTransaction =
                existingExpense().copy(accountId = 3L)
            transactionRepository.setInitialData(listOf(archivedTransaction))
            val viewModel = createViewModel(editHandle(10L))

            val state = viewModel.uiState.value
            assertThat(state.accountId).isEqualTo(3L)
            assertThat(state.archivedAccount?.id).isEqualTo(3L)
            assertThat(state.accounts.map { it.account.id }).containsExactly(1L, 2L)
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `edit mode preloads a transfer with origin and destination`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingTransfer()))
            val viewModel = createViewModel(editHandle(10L))

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.selectedType).isEqualTo(TransactionType.TRANSFER)
            assertThat(state.amountText).isEqualTo("1500")
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.toAccountId).isEqualTo(2L)
            assertThat(state.categoryId).isNull()
            assertThat(state.date).isEqualTo(baseDate)
            assertThat(state.note).isEqualTo("Almuerzo")
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `editing a transfer with an archived destination keeps it and can still be saved`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(
                listOf(existingTransfer().copy(toAccountId = 3L)),
            )
            val viewModel = createViewModel(editHandle(10L))

            val state = viewModel.uiState.value
            assertThat(state.toAccountId).isEqualTo(3L)
            assertThat(state.archivedToAccount?.id).isEqualTo(3L)
            assertThat(state.accounts.map { it.account.id }).containsExactly(1L, 2L)
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `editing a transfer shows the negative balance warning on the origin account`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingTransfer(amount = Money(150_000L))))
            val viewModel = createViewModel(editHandle(10L))
            viewModel.setAmountText("150000")

            viewModel.onSaveClick()
            assertThat(viewModel.uiState.value.showNegativeBalanceDialog).isTrue()
        }

    @Test
    fun `changing an expense into a transfer saves with destination and no category`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingExpense()))
            val viewModel = createViewModel(editHandle(10L))

            viewModel.setType(TransactionType.TRANSFER)
            viewModel.setToAccountId(2L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = transactionRepository.getTransaction(10L)
            assertThat(saved?.type).isEqualTo(TransactionType.TRANSFER)
            assertThat(saved?.toAccountId).isEqualTo(2L)
            assertThat(saved?.categoryId).isNull()
            assertThat(preferencesRepository.getUserPreferences().lastUsedAccountId).isEqualTo(1L)
        }

    @Test
    fun `changing a transfer into an expense saves with category and no destination`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingTransfer()))
            val viewModel = createViewModel(editHandle(10L))

            viewModel.setType(TransactionType.EXPENSE)
            viewModel.setCategoryId(10L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = transactionRepository.getTransaction(10L)
            assertThat(saved?.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(saved?.categoryId).isEqualTo(10L)
            assertThat(saved?.toAccountId).isNull()
        }

    @Test
    fun `edit mode keeps the transfer destination after process death`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingTransfer()))
            val handle = editHandle(10L)
            createViewModel(handle).setNote("Cena")

            val restored = createViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))

            val state = restored.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.selectedType).isEqualTo(TransactionType.TRANSFER)
            assertThat(state.note).isEqualTo("Cena")
            assertThat(state.amountText).isEqualTo("1500")
            assertThat(state.accountId).isEqualTo(1L)
            assertThat(state.toAccountId).isEqualTo(2L)
        }

    @Test
    fun `creating a transfer persists it and remembers the origin account`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle(accountId = 2L, initialType = TransactionType.TRANSFER))
            viewModel.setAmountText("1000")
            viewModel.setToAccountId(1L)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = transactionRepository.getTransaction(1L)
            assertThat(saved?.type).isEqualTo(TransactionType.TRANSFER)
            assertThat(saved?.accountId).isEqualTo(2L)
            assertThat(saved?.toAccountId).isEqualTo(1L)
            assertThat(saved?.categoryId).isNull()
            assertThat(preferencesRepository.getUserPreferences().lastUsedAccountId).isEqualTo(2L)
        }

    @Test
    fun `delete removes the transaction and navigates back`() =
        runTest(testDispatcher) {
            transactionRepository.setInitialData(listOf(existingExpense()))
            val viewModel = createViewModel(editHandle(10L))

            viewModel.onDeleteClick()
            assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmDelete()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(transactionRepository.getTransaction(10L)).isNull()
        }

    @Test
    fun `a category created from the editor is selected when it returns`() =
        runTest(testDispatcher) {
            val savedStateHandle = creationHandle(accountId = 1L)
            val viewModel = createViewModel(savedStateHandle)

            val newId =
                categoryRepository.saveCategory(
                    Category(id = 0L, name = "Nueva", type = CategoryType.EXPENSE, color = 5, icon = "star"),
                )
            savedStateHandle[NEW_CATEGORY_RESULT_KEY] = newId
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.categoryId).isEqualTo(newId)
            assertThat(state.selectedCategory?.id).isEqualTo(newId)
        }

    @Test
    fun `new account click emits the open account edit event`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(emptyList())
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onNewAccountClick()
                assertThat(awaitItem()).isEqualTo(TransactionEditEvent.OpenAccountEdit)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
