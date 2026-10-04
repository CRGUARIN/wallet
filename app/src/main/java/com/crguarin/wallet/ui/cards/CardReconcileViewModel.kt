package com.crguarin.wallet.ui.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.card.CardMath
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.usecase.card.ApplyReconciliationUseCase
import com.crguarin.wallet.domain.usecase.card.PreviewReconciliationUseCase
import com.crguarin.wallet.ui.navigation.CardReconcile
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

private const val KEY_CLOSING_DATE = "card_reconcile_closing_date"
private const val KEY_DUE_DATE = "card_reconcile_due_date"
private const val KEY_STATEMENT_BALANCE = "card_reconcile_statement_balance"
private const val KEY_MINIMUM_PAYMENT = "card_reconcile_minimum_payment"
private const val KEY_INTEREST = "card_reconcile_interest"

data class CardReconcileUiState(
    val isLoading: Boolean = true,
    /** The card no longer exists or is not a credit card. */
    val notFound: Boolean = false,
    val accountId: Long = 0L,
    val accountName: String = "",
    val closingDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val statementBalanceText: String = "",
    val minimumPaymentText: String = "",
    val interestText: String = "",
    val statementBalanceErrorRes: Int? = null,
    val preview: ReconciliationPreview? = null,
    val isCalculating: Boolean = false,
    val isSaving: Boolean = false,
    val generalErrorRes: Int? = null,
    val showClosingDatePicker: Boolean = false,
    val showDueDatePicker: Boolean = false,
) {
    val isCalculateEnabled: Boolean
        get() =
            !isLoading &&
                !notFound &&
                !isCalculating &&
                !isSaving &&
                closingDate != null &&
                dueDate != null &&
                MoneyParser.parse(statementBalanceText) != null

    val isSaveEnabled: Boolean
        get() = preview != null && !isCalculating && !isSaving
}

sealed interface CardReconcileEvent {
    data object ReconciliationSaved : CardReconcileEvent
}

@HiltViewModel
class CardReconcileViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val accountRepository: AccountRepository,
        private val previewReconciliationUseCase: PreviewReconciliationUseCase,
        private val applyReconciliationUseCase: ApplyReconciliationUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val route: CardReconcile = savedStateHandle.toRoute<CardReconcile>()
        val accountId: Long = route.accountId

        private val _uiState = MutableStateFlow(CardReconcileUiState(accountId = accountId))
        val uiState: StateFlow<CardReconcileUiState> = _uiState.asStateFlow()

        private val _events = Channel<CardReconcileEvent>(Channel.BUFFERED)
        val events: Flow<CardReconcileEvent> = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                loadCard()
            }
        }

        private suspend fun loadCard() {
            val account = accountRepository.getAccount(accountId)
            if (account == null || account.type != AccountType.CREDIT_CARD) {
                _uiState.update {
                    it.copy(isLoading = false, notFound = true, generalErrorRes = DomainError.NotFound.asStringRes())
                }
                return
            }
            val today = timeProvider.today()
            val statementDay = account.statementDay ?: today.dayOfMonth
            val defaultClosing = CardMath.lastClosingDate(today, statementDay)
            val defaultDue = CardMath.dueDate(defaultClosing, account.paymentDueDay ?: defaultClosing.dayOfMonth)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    accountName = account.name,
                    closingDate =
                        savedStateHandle.get<Long>(KEY_CLOSING_DATE)?.let { epoch ->
                            LocalDate.ofEpochDay(epoch)
                        } ?: defaultClosing,
                    dueDate =
                        savedStateHandle.get<Long>(KEY_DUE_DATE)?.let { epoch ->
                            LocalDate.ofEpochDay(epoch)
                        } ?: defaultDue,
                    statementBalanceText = savedStateHandle.get<String>(KEY_STATEMENT_BALANCE).orEmpty(),
                    minimumPaymentText = savedStateHandle.get<String>(KEY_MINIMUM_PAYMENT).orEmpty(),
                    interestText = savedStateHandle.get<String>(KEY_INTEREST).orEmpty(),
                )
            }
        }

        fun setClosingDate(date: LocalDate) {
            savedStateHandle[KEY_CLOSING_DATE] = date.toEpochDay()
            _uiState.update {
                it.copy(closingDate = date, showClosingDatePicker = false, preview = null, generalErrorRes = null)
            }
        }

        fun setDueDate(date: LocalDate) {
            savedStateHandle[KEY_DUE_DATE] = date.toEpochDay()
            _uiState.update {
                it.copy(dueDate = date, showDueDatePicker = false, generalErrorRes = null)
            }
        }

        fun setStatementBalanceText(text: String) {
            savedStateHandle[KEY_STATEMENT_BALANCE] = text
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    DomainError.AmountInvalid.asStringRes()
                } else {
                    null
                }
            _uiState.update {
                it.copy(
                    statementBalanceText = text,
                    statementBalanceErrorRes = error,
                    preview = null,
                    generalErrorRes = null,
                )
            }
        }

        fun setMinimumPaymentText(text: String) {
            savedStateHandle[KEY_MINIMUM_PAYMENT] = text
            _uiState.update { it.copy(minimumPaymentText = text, preview = null, generalErrorRes = null) }
        }

        fun setInterestText(text: String) {
            savedStateHandle[KEY_INTEREST] = text
            _uiState.update { it.copy(interestText = text, preview = null, generalErrorRes = null) }
        }

        fun openClosingDatePicker() {
            _uiState.update { it.copy(showClosingDatePicker = true) }
        }

        fun dismissClosingDatePicker() {
            _uiState.update { it.copy(showClosingDatePicker = false) }
        }

        fun openDueDatePicker() {
            _uiState.update { it.copy(showDueDatePicker = true) }
        }

        fun dismissDueDatePicker() {
            _uiState.update { it.copy(showDueDatePicker = false) }
        }

        fun calculatePreview() {
            val state = _uiState.value
            if (state.isCalculating || state.closingDate == null) return
            val statementBalance = MoneyParser.parse(state.statementBalanceText)
            if (statementBalance == null) {
                _uiState.update { it.copy(statementBalanceErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }
            val interest = state.interestText.ifBlank { null }?.let { MoneyParser.parse(it) }
            if (state.interestText.isNotBlank() && interest == null) {
                _uiState.update { it.copy(generalErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }
            _uiState.update { it.copy(isCalculating = true, generalErrorRes = null) }
            viewModelScope.launch {
                when (
                    val result =
                        previewReconciliationUseCase(
                            accountId = accountId,
                            closingDate = state.closingDate,
                            statementBalance = statementBalance,
                            statedInterestAndFees = interest,
                        )
                ) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isCalculating = false, preview = result.value) }
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            it.copy(isCalculating = false, preview = null, generalErrorRes = result.error.asStringRes())
                        }
                    }
                }
            }
        }

        /**
         * Saves the statement and its adjustment, if any.
         *
         * @param adjustmentNote localized note for the adjustment transaction
         * (e.g. `stringResource(card_reconcile_note, date)`), resolved by the UI.
         * @param forceNegativeAdjustment true when the user accepts adjusting a negative difference.
         */
        fun save(
            adjustmentNote: String,
            forceNegativeAdjustment: Boolean,
        ) {
            val state = _uiState.value
            val preview = state.preview ?: return
            val dueDate = state.dueDate ?: return
            if (state.isSaving) return
            val minimumPayment = state.minimumPaymentText.ifBlank { null }?.let { MoneyParser.parse(it) }
            if (state.minimumPaymentText.isNotBlank() && minimumPayment == null) {
                _uiState.update { it.copy(generalErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }
            val interest = state.interestText.ifBlank { null }?.let { MoneyParser.parse(it) }
            if (state.interestText.isNotBlank() && interest == null) {
                _uiState.update { it.copy(generalErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }
            _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
            viewModelScope.launch {
                // The cycle may have changed since the preview (e.g. after "review transactions"):
                // recompute it and never save an adjustment based on a stale difference.
                val current =
                    when (
                        val refreshed =
                            previewReconciliationUseCase(
                                accountId = accountId,
                                closingDate = preview.closingDate,
                                statementBalance = preview.statementBalance,
                                statedInterestAndFees = interest,
                            )
                    ) {
                        is DomainResult.Success -> refreshed.value
                        is DomainResult.Failure -> {
                            _uiState.update {
                                it.copy(isSaving = false, generalErrorRes = refreshed.error.asStringRes())
                            }
                            return@launch
                        }
                    }
                if (current != preview) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            preview = current,
                            generalErrorRes = R.string.card_reconcile_recalculated,
                        )
                    }
                    return@launch
                }
                when (
                    val result =
                        applyReconciliationUseCase(
                            preview = preview,
                            dueDate = dueDate,
                            minimumPayment = minimumPayment,
                            statedInterestAndFees = interest,
                            adjustmentNote = adjustmentNote,
                            forceNegativeAdjustment = forceNegativeAdjustment,
                        )
                ) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(CardReconcileEvent.ReconciliationSaved)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            it.copy(isSaving = false, generalErrorRes = result.error.asStringRes())
                        }
                    }
                }
            }
        }
    }
