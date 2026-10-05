package com.crguarin.wallet.ui.recurring

import androidx.lifecycle.SavedStateHandle
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
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.recurring.DeleteRecurringPaymentUseCase
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import com.crguarin.wallet.domain.usecase.recurring.SaveRecurringPaymentUseCase
import com.crguarin.wallet.domain.usecase.recurring.SetRecurringActiveUseCase
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
class RecurringEditViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveUseCase: SaveRecurringPaymentUseCase
    private lateinit var deleteUseCase: DeleteRecurringPaymentUseCase
    private lateinit var setActiveUseCase: SetRecurringActiveUseCase
    private lateinit var processDueUseCase: ProcessDueRecurringUseCase

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
    private val archivedAccount =
        Account(
            id = 3L,
            name = "Vieja",
            type = AccountType.CASH,
            initialBalance = Money.ZERO,
            color = 3,
            icon = "wallet",
            isArchived = true,
            createdAt = baseInstant,
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
        timeProvider = FakeTimeProvider(baseInstant)
        accountRepository.setInitialData(listOf(savings, card, archivedAccount))
        categoryRepository.setInitialData(listOf(expenseCategory, incomeCategory))
        saveUseCase =
            SaveRecurringPaymentUseCase(
                recurringRepository = recurringRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = timeProvider,
            )
        deleteUseCase = DeleteRecurringPaymentUseCase(recurringRepository)
        setActiveUseCase = SetRecurringActiveUseCase(recurringRepository, timeProvider)
        val transactionRepository = FakeTransactionRepository()
        processDueUseCase =
            ProcessDueRecurringUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                SaveTransactionUseCase(
                    transactionRepository,
                    accountRepository,
                    categoryRepository,
                    timeProvider,
                    CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
                ),
                timeProvider,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun creationHandle(): SavedStateHandle = SavedStateHandle(mapOf("paymentId" to null))

    private fun editHandle(paymentId: Long): SavedStateHandle = SavedStateHandle(mapOf("paymentId" to paymentId))

    private fun createViewModel(savedStateHandle: SavedStateHandle): RecurringEditViewModel =
        RecurringEditViewModel(
            savedStateHandle = savedStateHandle,
            recurringRepository = recurringRepository,
            categoryRepository = categoryRepository,
            observeAccountsUseCase = ObserveAccountsUseCase(accountRepository),
            observeCategoryTreeUseCase = ObserveCategoryTreeUseCase(categoryRepository),
            saveRecurringPaymentUseCase = saveUseCase,
            deleteRecurringPaymentUseCase = deleteUseCase,
            setRecurringActiveUseCase = setActiveUseCase,
            processDueRecurringUseCase = processDueUseCase,
            timeProvider = timeProvider,
        )

    private fun existingPayment(
        id: Long = 5L,
        name: String = "Netflix",
        mode: RecurringMode = RecurringMode.AUTO,
        reminderDaysBefore: Int? = null,
        isActive: Boolean = true,
        startDate: LocalDate = LocalDate.of(2026, 8, 15),
        nextDueDate: LocalDate = LocalDate.of(2026, 9, 15),
    ) = RecurringPayment(
        id = id,
        name = name,
        type = TransactionType.EXPENSE,
        amount = Money(4_590_000L),
        accountId = 1L,
        categoryId = 10L,
        frequency = RecurrenceFrequency.MONTHLY,
        interval = 1,
        startDate = startDate,
        nextDueDate = nextDueDate,
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    /** Fills every required field so a creation save reaches the use case. */
    private fun RecurringEditViewModel.fillValidCreateForm() {
        setName("Netflix")
        setAmountText("45900")
        setCategoryId(10L)
        setNextDate(today.plusDays(5))
    }

    @Test
    fun `create saves the payment with the chosen date as start and next due date`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setMode(RecurringMode.CONFIRM)
            viewModel.setReminderDaysBefore(3)

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem())
                    .isEqualTo(RecurringEditEvent.Saved(needsPermissionRequest = true))
                cancelAndIgnoreRemainingEvents()
            }

            val saved = recurringRepository.getAll().single()
            assertThat(saved.id).isGreaterThan(0L)
            assertThat(saved.name).isEqualTo("Netflix")
            assertThat(saved.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(saved.amount).isEqualTo(Money(4_590_000L))
            assertThat(saved.accountId).isEqualTo(1L)
            assertThat(saved.categoryId).isEqualTo(10L)
            assertThat(saved.frequency).isEqualTo(RecurrenceFrequency.MONTHLY)
            assertThat(saved.interval).isEqualTo(1)
            assertThat(saved.startDate).isEqualTo(today.plusDays(5))
            assertThat(saved.nextDueDate).isEqualTo(today.plusDays(5))
            assertThat(saved.mode).isEqualTo(RecurringMode.CONFIRM)
            assertThat(saved.reminderDaysBefore).isEqualTo(3)
            assertThat(saved.createdAt).isEqualTo(baseInstant)
            assertThat(saved.updatedAt).isEqualTo(baseInstant)
        }

    @Test
    fun `an automatic charge due today is recorded when saving`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setNextDate(today)

            viewModel.onSaveClick()

            assertThat(recurringRepository.completedTransactions.map { it.date }).containsExactly(today)
            assertThat(recurringRepository.getAll().single().nextDueDate).isEqualTo(today.plusMonths(1))
        }

    @Test
    fun `a charge that asks first is not recorded when saving`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setNextDate(today)
            viewModel.setMode(RecurringMode.CONFIRM)

            viewModel.onSaveClick()

            assertThat(recurringRepository.completedTransactions).isEmpty()
            assertThat(recurringRepository.getAll().single().nextDueDate).isEqualTo(today)
        }

    @Test
    fun `editing without schedule changes keeps the pending next due date`() =
        runTest(testDispatcher) {
            // CONFIRM: an AUTO payment would record the overdue charge on save.
            recurringRepository.setInitialData(listOf(existingPayment(mode = RecurringMode.CONFIRM)))
            val viewModel = createViewModel(editHandle(5L))

            assertThat(viewModel.uiState.value.isEditMode).isTrue()
            assertThat(viewModel.uiState.value.nextDate).isEqualTo(LocalDate.of(2026, 9, 15))

            viewModel.setName("Netflix Premium")
            viewModel.setAmountText("50000")
            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(RecurringEditEvent.Saved(needsPermissionRequest = true))
                cancelAndIgnoreRemainingEvents()
            }

            val saved = recurringRepository.get(5L)
            assertThat(saved!!.name).isEqualTo("Netflix Premium")
            assertThat(saved.amount).isEqualTo(Money(5_000_000L))
            assertThat(saved.startDate).isEqualTo(LocalDate.of(2026, 8, 15))
            assertThat(saved.nextDueDate).isEqualTo(LocalDate.of(2026, 9, 15))
            assertThat(saved.createdAt).isEqualTo(baseInstant)
        }

    @Test
    fun `moving the next charge date moves the calendar anchor`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val viewModel = createViewModel(editHandle(5L))

            viewModel.setNextDate(LocalDate.of(2026, 10, 20))
            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(RecurringEditEvent.Saved(needsPermissionRequest = false))
                cancelAndIgnoreRemainingEvents()
            }

            val saved = recurringRepository.get(5L)
            assertThat(saved!!.startDate).isEqualTo(LocalDate.of(2026, 10, 20))
            assertThat(saved.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 20))
        }

    @Test
    fun `editing the frequency recalculates the next due date from today`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val viewModel = createViewModel(editHandle(5L))

            viewModel.setFrequency(RecurrenceFrequency.WEEKLY)
            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(RecurringEditEvent.Saved(needsPermissionRequest = false))
                cancelAndIgnoreRemainingEvents()
            }

            val saved = recurringRepository.get(5L)
            assertThat(saved!!.startDate).isEqualTo(LocalDate.of(2026, 8, 15))
            assertThat(saved.frequency).isEqualTo(RecurrenceFrequency.WEEKLY)
            assertThat(saved.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 3))
        }

    @Test
    fun `editing a reminder payment asks for the notification permission`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(
                listOf(existingPayment(mode = RecurringMode.CONFIRM, reminderDaysBefore = 1)),
            )
            val viewModel = createViewModel(editHandle(5L))

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem())
                    .isEqualTo(RecurringEditEvent.Saved(needsPermissionRequest = true))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a new next charge date in the past is rejected`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val viewModel = createViewModel(editHandle(5L))

            viewModel.setNextDate(LocalDate.of(2026, 9, 1))
            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.dateErrorRes).isEqualTo(R.string.error_recurring_date_past)
            assertThat(viewModel.uiState.value.isSaving).isFalse()
            assertThat(recurringRepository.get(5L)!!.nextDueDate).isEqualTo(LocalDate.of(2026, 9, 15))
        }

    @Test
    fun `name errors are validated before saving`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())

            viewModel.setName("")
            viewModel.onSaveClick()
            assertThat(viewModel.uiState.value.nameErrorRes).isEqualTo(R.string.error_name_empty)

            viewModel.setName("x".repeat(DomainLimits.NAME_MAX_LENGTH + 1))
            assertThat(viewModel.uiState.value.nameErrorRes).isEqualTo(R.string.error_name_too_long)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `invalid amount is reported on the field`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())

            viewModel.setAmountText("abc")
            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.amountErrorRes).isEqualTo(R.string.error_amount_invalid)
            assertThat(viewModel.uiState.value.amountText).isEqualTo("abc")
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `interval out of range is reported on the field`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())

            viewModel.setIntervalText("13")
            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.intervalErrorRes).isEqualTo(R.string.error_recurring_interval)
            assertThat(viewModel.uiState.value.intervalText).isEqualTo("13")
        }

    @Test
    fun `a next charge date in the past is rejected when creating`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setNextDate(today.minusDays(1))

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.dateErrorRes).isEqualTo(R.string.error_recurring_date_past)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `an end date before the next charge is rejected`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setEndNever(false)
            viewModel.setEndDate(today)

            assertThat(viewModel.uiState.value.endDate).isEqualTo(today)
            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.endErrorRes).isEqualTo(R.string.error_recurring_end_before_start)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `a too long note is rejected`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())

            viewModel.setNote("x".repeat(DomainLimits.NOTE_MAX_LENGTH + 1))

            assertThat(viewModel.uiState.value.noteErrorRes).isEqualTo(R.string.error_note_too_long)
        }

    @Test
    fun `a missing account is reported as not found`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(emptyList())
            val viewModel = createViewModel(creationHandle())
            viewModel.setName("Netflix")
            viewModel.setAmountText("45900")
            viewModel.setCategoryId(10L)

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.accountErrorRes).isEqualTo(R.string.error_not_found)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `an archived account from the domain is mapped to its field`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            viewModel.setAccountId(3L)

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.accountErrorRes).isEqualTo(R.string.error_account_archived)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `a category of another type from the domain is mapped to its field`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.fillValidCreateForm()
            // Income category while the expense type is selected; the picker is bypassed on purpose.
            viewModel.setCategoryId(20L)

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.categoryErrorRes).isEqualTo(R.string.error_category_type_mismatch)
            assertThat(recurringRepository.getAll()).isEmpty()
        }

    @Test
    fun `changing the type clears the selected category`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.setCategoryId(10L)
            assertThat(viewModel.uiState.value.categoryId).isEqualTo(10L)

            viewModel.setType(TransactionType.INCOME)

            assertThat(viewModel.uiState.value.categoryId).isNull()
            assertThat(viewModel.uiState.value.selectedCategory).isNull()
            assertThat(viewModel.uiState.value.categoryErrorRes).isNull()
        }

    @Test
    fun `pausing and resuming updates the active flag and recalculates on resume`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val viewModel = createViewModel(editHandle(5L))

            viewModel.setActive(false)
            assertThat(recurringRepository.get(5L)!!.isActive).isFalse()

            viewModel.setActive(true)
            val resumed = recurringRepository.get(5L)!!
            assertThat(resumed.isActive).isTrue()
            assertThat(resumed.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 15))
        }

    @Test
    fun `resuming refreshes the form date so saving afterwards keeps it`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(
                listOf(existingPayment(isActive = false, nextDueDate = LocalDate.of(2026, 9, 15))),
            )
            val viewModel = createViewModel(editHandle(5L))
            assertThat(viewModel.uiState.value.nextDate).isEqualTo(LocalDate.of(2026, 9, 15))

            viewModel.setActive(true)

            // Resuming moves nextDueDate to the first occurrence on or after today (2026-09-28).
            assertThat(recurringRepository.get(5L)!!.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 15))
            assertThat(viewModel.uiState.value.nextDate).isEqualTo(LocalDate.of(2026, 10, 15))

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.dateErrorRes).isNull()
            assertThat(recurringRepository.get(5L)!!.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 15))
        }

    @Test
    fun `delete asks for confirmation and removes the payment`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val viewModel = createViewModel(editHandle(5L))

            viewModel.onDeleteClick()
            assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmDelete()
                assertThat(awaitItem()).isEqualTo(RecurringEditEvent.Deleted)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(recurringRepository.get(5L)).isNull()
        }

    @Test
    fun `an unknown payment id is reported as not found`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(editHandle(99L))

            assertThat(viewModel.uiState.value.notFound).isTrue()
            assertThat(viewModel.uiState.value.isEditMode).isFalse()
        }

    @Test
    fun `form entries survive process death through SavedStateHandle`() =
        runTest(testDispatcher) {
            val handle = creationHandle()
            val first = createViewModel(handle)
            first.setName("Gimnasio")
            first.setAmountText("120000")
            first.setFrequency(RecurrenceFrequency.YEARLY)
            first.setIntervalText("3")
            first.setNote("Anual")
            first.setNextDate(LocalDate.of(2026, 10, 10))
            first.setMode(RecurringMode.CONFIRM)
            first.setReminderDaysBefore(5)

            val restored =
                createViewModel(
                    SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
                )

            val state = restored.uiState.value
            assertThat(state.name).isEqualTo("Gimnasio")
            assertThat(state.amountText).isEqualTo("120000")
            assertThat(state.frequency).isEqualTo(RecurrenceFrequency.YEARLY)
            assertThat(state.intervalText).isEqualTo("3")
            assertThat(state.note).isEqualTo("Anual")
            assertThat(state.nextDate).isEqualTo(LocalDate.of(2026, 10, 10))
            assertThat(state.mode).isEqualTo(RecurringMode.CONFIRM)
            assertThat(state.reminderDaysBefore).isEqualTo(5)
            assertThat(state.isEditMode).isFalse()
        }

    @Test
    fun `restoring an edit keeps the unsaved form values`() =
        runTest(testDispatcher) {
            recurringRepository.setInitialData(listOf(existingPayment()))
            val handle = editHandle(5L)
            val first = createViewModel(handle)
            first.setName("Sin guardar")

            val restored =
                createViewModel(
                    SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
                )

            assertThat(restored.uiState.value.isEditMode).isTrue()
            assertThat(restored.uiState.value.name).isEqualTo("Sin guardar")
            assertThat(restored.uiState.value.nextDate).isEqualTo(LocalDate.of(2026, 9, 15))
        }
}
