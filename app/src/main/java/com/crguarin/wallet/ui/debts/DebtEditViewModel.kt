package com.crguarin.wallet.ui.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.formatForInput
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtDetailUseCase
import com.crguarin.wallet.domain.usecase.debt.SaveDebtUseCase
import com.crguarin.wallet.ui.navigation.DebtEdit
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val KEY_DIRECTION = "debt_form_direction"
private const val KEY_NAME = "debt_form_name"
private const val KEY_AMOUNT = "debt_form_amount"
private const val KEY_DATE = "debt_form_date"
private const val KEY_DUE_DATE = "debt_form_due_date"
private const val KEY_NOTE = "debt_form_note"

data class DebtEditUiState(
    /** True until an existing debt has been loaded; a new debt is never loading. */
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val direction: DebtDirection = DebtDirection.OWED_TO_ME,
    val personName: String = "",
    val personNameErrorRes: Int? = null,
    val amountText: String = "",
    val amountErrorRes: Int? = null,
    val date: LocalDate = LocalDate.now(),
    val dateErrorRes: Int? = null,
    val dueDate: LocalDate? = null,
    val dueDateErrorRes: Int? = null,
    val note: String = "",
    val noteErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showDueDatePicker: Boolean = false,
) {
    /** Save is enabled once the name and the amount are valid and nothing is still loading or saving. */
    val canSave: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                personName.isNotBlank() &&
                personNameErrorRes == null &&
                MoneyParser.parse(amountText) != null
}

sealed interface DebtEditEvent {
    data object Saved : DebtEditEvent

    data object Deleted : DebtEditEvent
}

@HiltViewModel
class DebtEditViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val observeDebtDetailUseCase: ObserveDebtDetailUseCase,
        private val saveDebtUseCase: SaveDebtUseCase,
        private val deleteDebtUseCase: DeleteDebtUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val route: DebtEdit = savedStateHandle.toRoute<DebtEdit>()
        private val debtId: Long? = route.debtId

        private val _uiState = MutableStateFlow(DebtEditUiState())
        val uiState: StateFlow<DebtEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<DebtEditEvent>(Channel.BUFFERED)
        val events: Flow<DebtEditEvent> = _events.receiveAsFlow()

        init {
            val today = timeProvider.today()
            val editingId = debtId
            // After process death the form already holds the user's unsaved edits; only a fresh
            // open copies the stored debt into it.
            val isRestoredForm = savedStateHandle.contains(KEY_NAME)
            _uiState.value =
                DebtEditUiState(
                    isLoading = editingId != null,
                    isEditMode = editingId != null,
                    direction =
                        savedStateHandle.get<String>(KEY_DIRECTION)?.let {
                            runCatching { DebtDirection.valueOf(it) }.getOrNull()
                        } ?: DebtDirection.OWED_TO_ME,
                    personName = savedStateHandle.get<String>(KEY_NAME).orEmpty(),
                    amountText = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
                    date = savedStateHandle.get<Long>(KEY_DATE)?.let { LocalDate.ofEpochDay(it) } ?: today,
                    dueDate = savedStateHandle.get<Long>(KEY_DUE_DATE)?.let { LocalDate.ofEpochDay(it) },
                    note = savedStateHandle.get<String>(KEY_NOTE).orEmpty(),
                )
            if (editingId != null) {
                viewModelScope.launch {
                    loadDebt(editingId, isRestoredForm)
                }
            }
        }

        private suspend fun loadDebt(
            id: Long,
            isRestoredForm: Boolean,
        ) {
            val debt = observeDebtDetailUseCase(id).first()?.summary?.debt
            if (debt == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return
            }
            if (isRestoredForm) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    direction = debt.direction,
                    personName = debt.personName,
                    amountText = formatForInput(debt.amount),
                    date = debt.date,
                    dueDate = debt.dueDate,
                    note = debt.note.orEmpty(),
                )
            }
            persistForm()
        }

        fun setDirection(direction: DebtDirection) {
            updateForm { it.copy(direction = direction, generalErrorRes = null) }
        }

        fun setPersonName(name: String) {
            val error =
                when {
                    name.isBlank() -> DomainError.NameEmpty.asStringRes()
                    name.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
                    else -> null
                }
            updateForm { it.copy(personName = name, personNameErrorRes = error, generalErrorRes = null) }
        }

        fun setAmountText(text: String) {
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    DomainError.AmountInvalid.asStringRes()
                } else {
                    null
                }
            updateForm { it.copy(amountText = text, amountErrorRes = error, generalErrorRes = null) }
        }

        fun openStartDatePicker() {
            _uiState.update { it.copy(showStartDatePicker = true) }
        }

        fun dismissStartDatePicker() {
            _uiState.update { it.copy(showStartDatePicker = false) }
        }

        fun setDate(date: LocalDate) {
            updateForm {
                it.copy(
                    date = date,
                    dateErrorRes = null,
                    dueDateErrorRes = dueDateError(date, it.dueDate),
                    showStartDatePicker = false,
                    generalErrorRes = null,
                )
            }
        }

        fun openDueDatePicker() {
            _uiState.update { it.copy(showDueDatePicker = true) }
        }

        fun dismissDueDatePicker() {
            _uiState.update { it.copy(showDueDatePicker = false) }
        }

        fun setDueDate(dueDate: LocalDate) {
            updateForm {
                it.copy(
                    dueDate = dueDate,
                    dueDateErrorRes = dueDateError(it.date, dueDate),
                    showDueDatePicker = false,
                    generalErrorRes = null,
                )
            }
        }

        fun clearDueDate() {
            updateForm { it.copy(dueDate = null, dueDateErrorRes = null, showDueDatePicker = false, generalErrorRes = null) }
        }

        fun setNote(note: String) {
            val error =
                if (note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            updateForm { it.copy(note = note, noteErrorRes = error, generalErrorRes = null) }
        }

        fun onSaveClick() {
            val state = _uiState.value
            if (state.isSaving || state.isLoading) return
            val nameError =
                when {
                    state.personName.isBlank() -> DomainError.NameEmpty.asStringRes()
                    state.personName.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
                    else -> null
                }
            val amount = MoneyParser.parse(state.amountText)
            val amountError = if (amount == null) DomainError.AmountInvalid.asStringRes() else null
            val noteError =
                if (state.note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            val dueError = dueDateError(state.date, state.dueDate)
            if (nameError != null || amountError != null || noteError != null || dueError != null) {
                _uiState.update {
                    it.copy(
                        personNameErrorRes = nameError,
                        amountErrorRes = amountError,
                        noteErrorRes = noteError,
                        dueDateErrorRes = dueError,
                    )
                }
                return
            }
            checkNotNull(amount)
            val now = timeProvider.now()
            val debt =
                Debt(
                    id = debtId ?: 0L,
                    personName = state.personName,
                    direction = state.direction,
                    amount = amount,
                    date = state.date,
                    dueDate = state.dueDate,
                    note = state.note,
                    createdAt = now,
                    updatedAt = now,
                )
            _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
            viewModelScope.launch {
                when (val result = saveDebtUseCase(debt)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(DebtEditEvent.Saved)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isSaving = false) }
                        applySaveError(result.error)
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
            val id = debtId ?: return
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                when (val result = deleteDebtUseCase(id)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false, showDeleteDialog = false) }
                        _events.send(DebtEditEvent.Deleted)
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

        private fun applySaveError(error: DomainError) {
            when (error) {
                is DomainError.NameEmpty, is DomainError.NameTooLong ->
                    _uiState.update { it.copy(personNameErrorRes = error.asStringRes()) }
                is DomainError.AmountInvalid, is DomainError.DebtAmountBelowPaid ->
                    _uiState.update { it.copy(amountErrorRes = error.asStringRes()) }
                is DomainError.DateOutOfRange ->
                    _uiState.update { it.copy(dateErrorRes = error.asStringRes()) }
                is DomainError.DebtDueBeforeDate ->
                    _uiState.update { it.copy(dueDateErrorRes = error.asStringRes()) }
                is DomainError.NoteTooLong ->
                    _uiState.update { it.copy(noteErrorRes = error.asStringRes()) }
                else ->
                    _uiState.update { it.copy(generalErrorRes = error.asStringRes()) }
            }
        }

        private fun dueDateError(
            date: LocalDate,
            dueDate: LocalDate?,
        ): Int? =
            if (dueDate != null && dueDate.isBefore(date)) {
                DomainError.DebtDueBeforeDate.asStringRes()
            } else {
                null
            }

        /** Applies [transform] to the form and keeps the typed values for process death. */
        private fun updateForm(transform: (DebtEditUiState) -> DebtEditUiState) {
            _uiState.update(transform)
            persistForm()
        }

        private fun persistForm() {
            val state = _uiState.value
            savedStateHandle[KEY_DIRECTION] = state.direction.name
            savedStateHandle[KEY_NAME] = state.personName
            savedStateHandle[KEY_AMOUNT] = state.amountText
            savedStateHandle[KEY_DATE] = state.date.toEpochDay()
            val dueDate = state.dueDate
            if (dueDate == null) {
                savedStateHandle.remove<Long>(KEY_DUE_DATE)
            } else {
                savedStateHandle[KEY_DUE_DATE] = dueDate.toEpochDay()
            }
            savedStateHandle[KEY_NOTE] = state.note
        }
    }
