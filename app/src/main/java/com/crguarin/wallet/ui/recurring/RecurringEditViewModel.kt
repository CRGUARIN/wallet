package com.crguarin.wallet.ui.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.formatForInput
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.recurring.DeleteRecurringPaymentUseCase
import com.crguarin.wallet.domain.usecase.recurring.SaveRecurringPaymentUseCase
import com.crguarin.wallet.domain.usecase.recurring.SetRecurringActiveUseCase
import com.crguarin.wallet.ui.navigation.RecurringEdit
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val KEY_TYPE = "recurring_form_type"
private const val KEY_NAME = "recurring_form_name"
private const val KEY_AMOUNT = "recurring_form_amount"
private const val KEY_ACCOUNT = "recurring_form_account"
private const val KEY_CATEGORY = "recurring_form_category"
private const val KEY_FREQUENCY = "recurring_form_frequency"
private const val KEY_INTERVAL = "recurring_form_interval"
private const val KEY_NEXT_DATE = "recurring_form_next_date"
private const val KEY_END_NEVER = "recurring_form_end_never"
private const val KEY_END_DATE = "recurring_form_end_date"
private const val KEY_MODE = "recurring_form_mode"
private const val KEY_REMINDER = "recurring_form_reminder"
private const val KEY_NOTE = "recurring_form_note"

/** Reminder choices offered in the form, in display order. Null means no reminder. */
val RECURRING_REMINDER_OPTIONS: List<Int?> = listOf(null, 1, 2, 3, 5, 7)

const val RECURRING_HISTORY_LIMIT = 6

data class RecurringEditUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val name: String = "",
    val nameErrorRes: Int? = null,
    val amountText: String = "",
    val amountErrorRes: Int? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val accountId: Long? = null,
    val accountErrorRes: Int? = null,
    val categoryNodes: List<CategoryNode> = emptyList(),
    val categoryId: Long? = null,
    val selectedCategory: Category? = null,
    val categoryErrorRes: Int? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalText: String = "1",
    val intervalErrorRes: Int? = null,
    val nextDate: LocalDate? = null,
    val dateErrorRes: Int? = null,
    val endNever: Boolean = true,
    val endDate: LocalDate? = null,
    val endErrorRes: Int? = null,
    val mode: RecurringMode = RecurringMode.AUTO,
    val reminderDaysBefore: Int? = null,
    val note: String = "",
    val noteErrorRes: Int? = null,
    val isActive: Boolean = true,
    val history: List<TransactionDetails> = emptyList(),
    val generalErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showNextDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

sealed interface RecurringEditEvent {
    data class Saved(
        val needsPermissionRequest: Boolean,
    ) : RecurringEditEvent

    data object Deleted : RecurringEditEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurringEditViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val recurringRepository: RecurringRepository,
        private val categoryRepository: CategoryRepository,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val observeCategoryTreeUseCase: ObserveCategoryTreeUseCase,
        private val saveRecurringPaymentUseCase: SaveRecurringPaymentUseCase,
        private val deleteRecurringPaymentUseCase: DeleteRecurringPaymentUseCase,
        private val setRecurringActiveUseCase: SetRecurringActiveUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val route: RecurringEdit = savedStateHandle.toRoute<RecurringEdit>()
        val targetPaymentId: Long? = route.paymentId

        private val selectedTypeFlow: MutableStateFlow<TransactionType>

        private var originalPayment: RecurringPayment? = null

        private val _uiState = MutableStateFlow(RecurringEditUiState())
        val uiState: StateFlow<RecurringEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<RecurringEditEvent>(Channel.BUFFERED)
        val events: Flow<RecurringEditEvent> = _events.receiveAsFlow()

        init {
            val today = timeProvider.today()
            val restoredType =
                savedStateHandle.get<String>(KEY_TYPE)?.let {
                    runCatching { TransactionType.valueOf(it) }.getOrNull()
                } ?: TransactionType.EXPENSE
            selectedTypeFlow = MutableStateFlow(restoredType)
            _uiState.value =
                RecurringEditUiState(
                    today = today,
                    type = restoredType,
                    name = savedStateHandle.get<String>(KEY_NAME).orEmpty(),
                    amountText = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
                    accountId = savedStateHandle.get<Long>(KEY_ACCOUNT),
                    categoryId = savedStateHandle.get<Long>(KEY_CATEGORY),
                    frequency =
                        savedStateHandle.get<String>(KEY_FREQUENCY)?.let {
                            runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull()
                        } ?: RecurrenceFrequency.MONTHLY,
                    intervalText = savedStateHandle.get<String>(KEY_INTERVAL) ?: "1",
                    nextDate =
                        savedStateHandle.get<Long>(KEY_NEXT_DATE)?.let { LocalDate.ofEpochDay(it) }
                            ?: today,
                    endNever = savedStateHandle.get<Boolean>(KEY_END_NEVER) ?: true,
                    endDate = savedStateHandle.get<Long>(KEY_END_DATE)?.let { LocalDate.ofEpochDay(it) },
                    mode =
                        savedStateHandle.get<String>(KEY_MODE)?.let {
                            runCatching { RecurringMode.valueOf(it) }.getOrNull()
                        } ?: RecurringMode.AUTO,
                    reminderDaysBefore = savedStateHandle.get<Int>(KEY_REMINDER),
                    note = savedStateHandle.get<String>(KEY_NOTE).orEmpty(),
                )
            viewModelScope.launch {
                loadInitialData(today)
            }
            viewModelScope.launch {
                observeAccountsUseCase(false).collect { accounts ->
                    _uiState.update { state ->
                        // New forms default to the first active account, like the transaction editor.
                        val defaultAccountId =
                            if (!state.isEditMode && state.accountId == null) {
                                accounts.firstOrNull()?.account?.id
                            } else {
                                state.accountId
                            }
                        if (defaultAccountId != null && defaultAccountId != state.accountId) {
                            savedStateHandle[KEY_ACCOUNT] = defaultAccountId
                        }
                        state.copy(
                            accounts = accounts,
                            accountId = defaultAccountId ?: state.accountId,
                        )
                    }
                }
            }
            viewModelScope.launch {
                selectedTypeFlow
                    .flatMapLatest { type ->
                        observeCategoryTreeUseCase(type.toCategoryType())
                    }.collect { nodes ->
                        _uiState.update { it.copy(categoryNodes = nodes) }
                    }
            }
            val editingId = targetPaymentId
            if (editingId != null && editingId > 0L) {
                viewModelScope.launch {
                    recurringRepository
                        .observeGeneratedTransactions(editingId, RECURRING_HISTORY_LIMIT)
                        .collect { history ->
                            _uiState.update { it.copy(history = history) }
                        }
                }
            } else {
                viewModelScope.launch {
                    flowOf(emptyList<TransactionDetails>()).collect { history ->
                        _uiState.update { it.copy(history = history) }
                    }
                }
            }
        }

        private suspend fun loadInitialData(today: LocalDate) {
            val editingId = targetPaymentId
            if (editingId != null && editingId > 0L) {
                val existing = recurringRepository.get(editingId)
                if (existing == null) {
                    _uiState.update {
                        it.copy(isLoading = false, notFound = true)
                    }
                    return
                }
                originalPayment = existing
                // After process death the form already holds the user's unsaved edits; only
                // the first load copies the stored payment into it.
                val isRestoredForm = savedStateHandle.contains(KEY_NAME)
                if (isRestoredForm) {
                    selectedTypeFlow.value = _uiState.value.type
                    _uiState.update { it.copy(isLoading = false, isEditMode = true, isActive = existing.isActive) }
                } else {
                    selectedTypeFlow.value = existing.type
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            type = existing.type,
                            name = existing.name,
                            amountText = formatForInput(existing.amount),
                            accountId = existing.accountId,
                            categoryId = existing.categoryId,
                            frequency = existing.frequency,
                            intervalText = existing.interval.toString(),
                            nextDate = existing.nextDueDate,
                            endNever = existing.endDate == null,
                            endDate = existing.endDate,
                            mode = existing.mode,
                            reminderDaysBefore = existing.reminderDaysBefore,
                            note = existing.note.orEmpty(),
                            isActive = existing.isActive,
                        )
                    }
                    persistForm()
                }
                _uiState.value.categoryId?.let { resolveSelectedCategory(it) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _uiState.value.categoryId?.let { resolveSelectedCategory(it) }
            }
        }

        private suspend fun resolveSelectedCategory(categoryId: Long) {
            val category = categoryRepository.getCategory(categoryId)
            if (category == null) {
                _uiState.update { it.copy(categoryId = null, selectedCategory = null) }
                return
            }
            _uiState.update {
                it.copy(
                    categoryId = category.id,
                    selectedCategory = category,
                )
            }
        }

        private fun persistForm() {
            val state = _uiState.value
            savedStateHandle[KEY_TYPE] = state.type.name
            savedStateHandle[KEY_NAME] = state.name
            savedStateHandle[KEY_AMOUNT] = state.amountText
            state.accountId?.let { savedStateHandle[KEY_ACCOUNT] = it }
            state.categoryId?.let { savedStateHandle[KEY_CATEGORY] = it }
            savedStateHandle[KEY_FREQUENCY] = state.frequency.name
            savedStateHandle[KEY_INTERVAL] = state.intervalText
            state.nextDate?.let { savedStateHandle[KEY_NEXT_DATE] = it.toEpochDay() }
            savedStateHandle[KEY_END_NEVER] = state.endNever
            state.endDate?.let { savedStateHandle[KEY_END_DATE] = it.toEpochDay() }
            savedStateHandle[KEY_MODE] = state.mode.name
            state.reminderDaysBefore?.let { savedStateHandle[KEY_REMINDER] = it }
            savedStateHandle[KEY_NOTE] = state.note
        }

        fun setType(type: TransactionType) {
            if (type == _uiState.value.type || type == TransactionType.TRANSFER) return
            selectedTypeFlow.value = type
            savedStateHandle[KEY_TYPE] = type.name
            savedStateHandle.remove<Long>(KEY_CATEGORY)
            _uiState.update {
                it.copy(
                    type = type,
                    categoryId = null,
                    selectedCategory = null,
                    categoryErrorRes = null,
                    generalErrorRes = null,
                )
            }
        }

        fun setName(name: String) {
            savedStateHandle[KEY_NAME] = name
            val error =
                when {
                    name.isBlank() -> DomainError.NameEmpty.asStringRes()
                    name.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
                    else -> null
                }
            _uiState.update { it.copy(name = name, nameErrorRes = error, generalErrorRes = null) }
        }

        fun setAmountText(text: String) {
            savedStateHandle[KEY_AMOUNT] = text
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    DomainError.AmountInvalid.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(amountText = text, amountErrorRes = error, generalErrorRes = null) }
        }

        fun setAccountId(accountId: Long) {
            savedStateHandle[KEY_ACCOUNT] = accountId
            _uiState.update {
                it.copy(
                    accountId = accountId,
                    accountErrorRes = null,
                    showAccountSheet = false,
                    generalErrorRes = null,
                )
            }
        }

        fun setCategoryId(categoryId: Long) {
            viewModelScope.launch {
                val category = categoryRepository.getCategory(categoryId)
                if (category == null) return@launch
                savedStateHandle[KEY_CATEGORY] = category.id
                _uiState.update {
                    it.copy(
                        categoryId = category.id,
                        selectedCategory = category,
                        categoryErrorRes = null,
                        showCategorySheet = false,
                        generalErrorRes = null,
                    )
                }
            }
        }

        fun setFrequency(frequency: RecurrenceFrequency) {
            savedStateHandle[KEY_FREQUENCY] = frequency.name
            _uiState.update { it.copy(frequency = frequency, generalErrorRes = null) }
        }

        fun setIntervalText(text: String) {
            val filtered = text.filter { it.isDigit() }.take(2)
            savedStateHandle[KEY_INTERVAL] = filtered
            val error =
                if (filtered.toIntOrNull() !in 1..12) {
                    DomainError.RecurringIntervalInvalid.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(intervalText = filtered, intervalErrorRes = error, generalErrorRes = null) }
        }

        fun setNextDate(date: LocalDate) {
            savedStateHandle[KEY_NEXT_DATE] = date.toEpochDay()
            _uiState.update {
                it.copy(nextDate = date, dateErrorRes = null, showNextDatePicker = false, generalErrorRes = null)
            }
        }

        fun setEndNever(endNever: Boolean) {
            savedStateHandle[KEY_END_NEVER] = endNever
            _uiState.update { it.copy(endNever = endNever, endErrorRes = null, generalErrorRes = null) }
        }

        fun setEndDate(date: LocalDate) {
            savedStateHandle[KEY_END_DATE] = date.toEpochDay()
            _uiState.update {
                it.copy(endDate = date, endNever = false, endErrorRes = null, showEndDatePicker = false, generalErrorRes = null)
            }
        }

        fun setMode(mode: RecurringMode) {
            savedStateHandle[KEY_MODE] = mode.name
            _uiState.update { it.copy(mode = mode, generalErrorRes = null) }
        }

        fun setReminderDaysBefore(days: Int?) {
            if (days == null) {
                savedStateHandle.remove<Int>(KEY_REMINDER)
            } else {
                savedStateHandle[KEY_REMINDER] = days
            }
            _uiState.update { it.copy(reminderDaysBefore = days, generalErrorRes = null) }
        }

        fun setNote(note: String) {
            savedStateHandle[KEY_NOTE] = note
            val error =
                if (note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(note = note, noteErrorRes = error, generalErrorRes = null) }
        }

        fun openAccountSheet() {
            _uiState.update { it.copy(showAccountSheet = true) }
        }

        fun dismissAccountSheet() {
            _uiState.update { it.copy(showAccountSheet = false) }
        }

        fun openCategorySheet() {
            _uiState.update { it.copy(showCategorySheet = true) }
        }

        fun dismissCategorySheet() {
            _uiState.update { it.copy(showCategorySheet = false) }
        }

        fun openNextDatePicker() {
            _uiState.update { it.copy(showNextDatePicker = true) }
        }

        fun dismissNextDatePicker() {
            _uiState.update { it.copy(showNextDatePicker = false) }
        }

        fun openEndDatePicker() {
            _uiState.update { it.copy(showEndDatePicker = true) }
        }

        fun dismissEndDatePicker() {
            _uiState.update { it.copy(showEndDatePicker = false) }
        }

        fun setActive(active: Boolean) {
            val editingId = targetPaymentId
            if (!_uiState.value.isEditMode || editingId == null || editingId <= 0L) {
                _uiState.update { it.copy(isActive = active) }
                return
            }
            if (active == _uiState.value.isActive) return
            viewModelScope.launch {
                when (
                    val result = setRecurringActiveUseCase(id = editingId, active = active)
                ) {
                    is DomainResult.Success -> {
                        val refreshed = recurringRepository.get(editingId)
                        if (refreshed != null) {
                            originalPayment = refreshed
                            // Resuming recalculates nextDueDate; keep the form in sync so saving
                            // afterwards does not treat the stale date as a new calendar anchor.
                            val syncedNextDate =
                                if (active) {
                                    refreshed.nextDueDate
                                } else {
                                    _uiState.value.nextDate
                                }
                            _uiState.update {
                                it.copy(
                                    isActive = active,
                                    nextDate = syncedNextDate,
                                    dateErrorRes = if (active) null else it.dateErrorRes,
                                    generalErrorRes = null,
                                )
                            }
                            if (active) persistForm()
                        } else {
                            _uiState.update { it.copy(isActive = active, generalErrorRes = null) }
                        }
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(generalErrorRes = result.error.asStringRes()) }
                    }
                }
            }
        }

        fun onDeleteClick() {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }

        fun dismissDeleteDialog() {
            _uiState.update { it.copy(showDeleteDialog = false) }
        }

        fun confirmDelete() {
            val editingId = targetPaymentId ?: return
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                when (
                    val result = deleteRecurringPaymentUseCase(id = editingId)
                ) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false, showDeleteDialog = false) }
                        _events.send(RecurringEditEvent.Deleted)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                showDeleteDialog = false,
                                generalErrorRes = result.error.asStringRes(),
                            )
                        }
                    }
                }
            }
        }

        fun onSaveClick() {
            val state = _uiState.value
            if (state.isSaving || state.isLoading) return
            val nameError =
                when {
                    state.name.isBlank() -> DomainError.NameEmpty.asStringRes()
                    state.name.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
                    else -> null
                }
            val amount = MoneyParser.parse(state.amountText)
            val amountError = if (amount == null) DomainError.AmountInvalid.asStringRes() else null
            val accountError = if (state.accountId == null) DomainError.NotFound.asStringRes() else null
            val categoryError = if (state.categoryId == null) R.string.error_category_required else null
            val interval = state.intervalText.toIntOrNull()
            val intervalError =
                if (interval !in 1..12) DomainError.RecurringIntervalInvalid.asStringRes() else null
            val original = originalPayment
            val nextDate = state.nextDate
            // On edition the date field shows `nextDueDate`; picking a different date moves the
            // calendar anchor, and the use case recalculates the next occurrence from today.
            val dateIsNew = original == null || nextDate != original.nextDueDate
            val dateError =
                when {
                    nextDate == null -> DomainError.RecurringDateInPast.asStringRes()
                    dateIsNew && nextDate < state.today -> DomainError.RecurringDateInPast.asStringRes()
                    else -> null
                }
            val endDate = if (state.endNever) null else state.endDate
            val endError =
                if (!state.endNever && (endDate == null || (nextDate != null && endDate < nextDate))) {
                    DomainError.RecurringEndBeforeStart.asStringRes()
                } else {
                    null
                }
            val noteError =
                if (state.note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            if (listOf(nameError, amountError, accountError, categoryError, intervalError, dateError, endError, noteError)
                    .any { it != null }
            ) {
                _uiState.update {
                    it.copy(
                        nameErrorRes = nameError,
                        amountErrorRes = amountError,
                        accountErrorRes = accountError,
                        categoryErrorRes = categoryError,
                        intervalErrorRes = intervalError,
                        dateErrorRes = dateError,
                        endErrorRes = endError,
                        noteErrorRes = noteError,
                    )
                }
                return
            }
            checkNotNull(amount)
            checkNotNull(nextDate)
            checkNotNull(interval)
            val now = timeProvider.now()
            val payment =
                RecurringPayment(
                    id = original?.id ?: 0L,
                    name = state.name.trim(),
                    type = state.type,
                    amount = amount,
                    accountId = requireNotNull(state.accountId),
                    categoryId = requireNotNull(state.categoryId),
                    note = state.note.ifBlank { null },
                    frequency = state.frequency,
                    interval = interval,
                    startDate = if (dateIsNew) nextDate else requireNotNull(original).startDate,
                    endDate = endDate,
                    nextDueDate = if (dateIsNew) nextDate else requireNotNull(original).nextDueDate,
                    mode = state.mode,
                    reminderDaysBefore = state.reminderDaysBefore,
                    isActive = state.isActive,
                    createdAt = original?.createdAt ?: now,
                    updatedAt = now,
                )
            _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
            viewModelScope.launch {
                when (
                    val result = saveRecurringPaymentUseCase(payment = payment)
                ) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        val needsPermission = state.reminderDaysBefore != null || state.mode == RecurringMode.CONFIRM
                        _events.send(RecurringEditEvent.Saved(needsPermissionRequest = needsPermission))
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isSaving = false) }
                        applySaveError(result.error)
                    }
                }
            }
        }

        private fun applySaveError(error: DomainError) {
            when (error) {
                is DomainError.NameEmpty, is DomainError.NameTooLong ->
                    _uiState.update { it.copy(nameErrorRes = error.asStringRes()) }
                is DomainError.AmountInvalid ->
                    _uiState.update { it.copy(amountErrorRes = error.asStringRes()) }
                is DomainError.AccountArchived, is DomainError.NotFound ->
                    _uiState.update { it.copy(accountErrorRes = error.asStringRes()) }
                is DomainError.CategoryArchived, is DomainError.CategoryTypeMismatch, is DomainError.CategoryRequired ->
                    _uiState.update { it.copy(categoryErrorRes = error.asStringRes()) }
                is DomainError.RecurringIntervalInvalid ->
                    _uiState.update { it.copy(intervalErrorRes = error.asStringRes()) }
                is DomainError.RecurringDateInPast, is DomainError.DateOutOfRange ->
                    _uiState.update { it.copy(dateErrorRes = error.asStringRes()) }
                is DomainError.RecurringEndBeforeStart ->
                    _uiState.update { it.copy(endErrorRes = error.asStringRes()) }
                is DomainError.NoteTooLong ->
                    _uiState.update { it.copy(noteErrorRes = error.asStringRes()) }
                else ->
                    _uiState.update { it.copy(generalErrorRes = error.asStringRes()) }
            }
        }

        private fun TransactionType.toCategoryType(): CategoryType =
            when (this) {
                TransactionType.EXPENSE -> CategoryType.EXPENSE
                TransactionType.INCOME -> CategoryType.INCOME
                TransactionType.TRANSFER -> CategoryType.EXPENSE
            }
    }
