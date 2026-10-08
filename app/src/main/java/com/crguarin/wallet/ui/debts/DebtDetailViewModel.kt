package com.crguarin.wallet.ui.debts

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
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.debt.DebtDetail
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.usecase.debt.AddDebtPaymentUseCase
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtPaymentUseCase
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtDetailUseCase
import com.crguarin.wallet.domain.usecase.debt.SettleDebtUseCase
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.crguarin.wallet.ui.navigation.DebtDetail as DebtDetailNav

/**
 * Debt detail state. [detail] is null when the debt does not exist (or was deleted). The dialogs
 * keep their own input and inline errors, so a rejected payment does not close them.
 */
data class DebtDetailUiState(
    val isLoading: Boolean = true,
    val detail: DebtDetail? = null,
    val today: LocalDate = LocalDate.now(),
    val isProcessing: Boolean = false,
    val showPaymentDialog: Boolean = false,
    val paymentAmountText: String = "",
    val paymentAmountErrorRes: Int? = null,
    val paymentDate: LocalDate = LocalDate.now(),
    val paymentDateErrorRes: Int? = null,
    val showPaymentDatePicker: Boolean = false,
    val paymentNote: String = "",
    val paymentNoteErrorRes: Int? = null,
    /** Errors of the payment dialog that do not belong to one of its fields. */
    val dialogErrorRes: Int? = null,
    val showSettleDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val paymentToDelete: DebtPayment? = null,
)

sealed interface DebtDetailEvent {
    data class ShowMessage(
        val messageRes: Int,
    ) : DebtDetailEvent

    data object Deleted : DebtDetailEvent
}

@HiltViewModel
class DebtDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observeDebtDetailUseCase: ObserveDebtDetailUseCase,
        private val addDebtPaymentUseCase: AddDebtPaymentUseCase,
        private val settleDebtUseCase: SettleDebtUseCase,
        private val deleteDebtPaymentUseCase: DeleteDebtPaymentUseCase,
        private val deleteDebtUseCase: DeleteDebtUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val debtId: Long = savedStateHandle.toRoute<DebtDetailNav>().debtId

        private val _uiState = MutableStateFlow(DebtDetailUiState(today = timeProvider.today()))
        val uiState: StateFlow<DebtDetailUiState> = _uiState.asStateFlow()

        private val _events = Channel<DebtDetailEvent>(Channel.BUFFERED)
        val events: Flow<DebtDetailEvent> = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                observeDebtDetailUseCase(debtId).collect { detail ->
                    _uiState.update { it.copy(isLoading = false, detail = detail) }
                }
            }
            viewModelScope.launch {
                timeProvider.todayFlow().collect { today -> _uiState.update { it.copy(today = today) } }
            }
        }

        fun openPaymentDialog() {
            val summary = _uiState.value.detail?.summary ?: return
            if (summary.isSettled) return
            _uiState.update {
                it.copy(
                    showPaymentDialog = true,
                    paymentAmountText = formatForInput(summary.remaining),
                    paymentAmountErrorRes = null,
                    paymentDate = it.today,
                    paymentDateErrorRes = null,
                    paymentNote = "",
                    paymentNoteErrorRes = null,
                    dialogErrorRes = null,
                )
            }
        }

        fun dismissPaymentDialog() {
            _uiState.update {
                it.copy(
                    showPaymentDialog = false,
                    showPaymentDatePicker = false,
                    dialogErrorRes = null,
                )
            }
        }

        fun setPaymentAmountText(text: String) {
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    DomainError.AmountInvalid.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(paymentAmountText = text, paymentAmountErrorRes = error, dialogErrorRes = null) }
        }

        fun openPaymentDatePicker() {
            _uiState.update { it.copy(showPaymentDatePicker = true) }
        }

        fun dismissPaymentDatePicker() {
            _uiState.update { it.copy(showPaymentDatePicker = false) }
        }

        fun setPaymentDate(date: LocalDate) {
            _uiState.update {
                it.copy(paymentDate = date, paymentDateErrorRes = null, showPaymentDatePicker = false)
            }
        }

        fun setPaymentNote(note: String) {
            val error =
                if (note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(paymentNote = note, paymentNoteErrorRes = error, dialogErrorRes = null) }
        }

        fun confirmPayment() {
            val state = _uiState.value
            if (state.isProcessing) return
            val amount = MoneyParser.parse(state.paymentAmountText)
            if (amount == null) {
                _uiState.update { it.copy(paymentAmountErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                when (
                    val result =
                        addDebtPaymentUseCase(
                            debtId = debtId,
                            amount = amount,
                            date = state.paymentDate,
                            note = state.paymentNote,
                        )
                ) {
                    is DomainResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                showPaymentDialog = false,
                                showPaymentDatePicker = false,
                                dialogErrorRes = null,
                            )
                        }
                        _events.send(DebtDetailEvent.ShowMessage(R.string.debts_payment_recorded))
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isProcessing = false) }
                        applyPaymentError(result.error)
                    }
                }
            }
        }

        fun openSettleDialog() {
            _uiState.update { it.copy(showSettleDialog = true) }
        }

        fun dismissSettleDialog() {
            _uiState.update { it.copy(showSettleDialog = false) }
        }

        fun confirmSettle() {
            if (_uiState.value.isProcessing) return
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                val messageRes =
                    when (val result = settleDebtUseCase(debtId)) {
                        is DomainResult.Success -> R.string.debts_settled_message
                        is DomainResult.Failure -> result.error.asStringRes()
                    }
                _uiState.update { it.copy(isProcessing = false, showSettleDialog = false) }
                _events.send(DebtDetailEvent.ShowMessage(messageRes))
            }
        }

        fun openDeletePayment(payment: DebtPayment) {
            _uiState.update { it.copy(paymentToDelete = payment) }
        }

        fun dismissDeletePayment() {
            _uiState.update { it.copy(paymentToDelete = null) }
        }

        fun confirmDeletePayment() {
            val payment = _uiState.value.paymentToDelete ?: return
            if (_uiState.value.isProcessing) return
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                val messageRes =
                    when (val result = deleteDebtPaymentUseCase(payment.id)) {
                        is DomainResult.Success -> R.string.debts_payment_deleted
                        is DomainResult.Failure -> result.error.asStringRes()
                    }
                _uiState.update { it.copy(isProcessing = false, paymentToDelete = null) }
                _events.send(DebtDetailEvent.ShowMessage(messageRes))
            }
        }

        fun openDeleteDebt() {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }

        fun dismissDeleteDebt() {
            _uiState.update { it.copy(showDeleteDialog = false) }
        }

        fun confirmDeleteDebt() {
            if (_uiState.value.isProcessing) return
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                when (val result = deleteDebtUseCase(debtId)) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isProcessing = false, showDeleteDialog = false) }
                        _events.send(DebtDetailEvent.Deleted)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isProcessing = false, showDeleteDialog = false) }
                        _events.send(DebtDetailEvent.ShowMessage(result.error.asStringRes()))
                    }
                }
            }
        }

        /** Amount problems are shown on the amount field, date and note on theirs, the rest in the dialog. */
        private fun applyPaymentError(error: DomainError) {
            when (error) {
                is DomainError.AmountInvalid, is DomainError.DebtPaymentExceedsRemaining ->
                    _uiState.update { it.copy(paymentAmountErrorRes = error.asStringRes()) }
                is DomainError.DateOutOfRange ->
                    _uiState.update { it.copy(paymentDateErrorRes = error.asStringRes()) }
                is DomainError.NoteTooLong ->
                    _uiState.update { it.copy(paymentNoteErrorRes = error.asStringRes()) }
                else ->
                    _uiState.update { it.copy(dialogErrorRes = error.asStringRes()) }
            }
        }
    }
