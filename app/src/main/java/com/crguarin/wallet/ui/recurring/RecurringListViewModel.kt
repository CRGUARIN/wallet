package com.crguarin.wallet.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.formatForInput
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.model.recurring.RecurringOverview
import com.crguarin.wallet.domain.usecase.recurring.ConfirmRecurringOccurrenceUseCase
import com.crguarin.wallet.domain.usecase.recurring.ObserveRecurringOverviewUseCase
import com.crguarin.wallet.domain.usecase.recurring.SkipRecurringOccurrenceUseCase
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

data class RecurringListUiState(
    val isLoading: Boolean = true,
    val overview: RecurringOverview? = null,
    val confirmItem: RecurringItem? = null,
    val confirmAmountText: String = "",
    val confirmAmountErrorRes: Int? = null,
    val confirmDate: LocalDate? = null,
    val showConfirmDatePicker: Boolean = false,
    val skipItem: RecurringItem? = null,
    val isProcessing: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

sealed interface RecurringListEvent {
    data class ShowMessage(
        val messageRes: Int,
    ) : RecurringListEvent
}

@HiltViewModel
class RecurringListViewModel
    @Inject
    constructor(
        observeRecurringOverviewUseCase: ObserveRecurringOverviewUseCase,
        private val confirmRecurringOccurrenceUseCase: ConfirmRecurringOccurrenceUseCase,
        private val skipRecurringOccurrenceUseCase: SkipRecurringOccurrenceUseCase,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecurringListUiState(today = timeProvider.today()))
        val uiState: StateFlow<RecurringListUiState> = _uiState.asStateFlow()

        private val _events = Channel<RecurringListEvent>(Channel.BUFFERED)
        val events: Flow<RecurringListEvent> = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                observeRecurringOverviewUseCase().collect { overview ->
                    _uiState.update { it.copy(isLoading = false, overview = overview) }
                }
            }
        }

        fun openConfirm(item: RecurringItem) {
            _uiState.update {
                it.copy(
                    confirmItem = item,
                    confirmAmountText = formatForInput(item.payment.amount),
                    confirmAmountErrorRes = null,
                    confirmDate = item.payment.nextDueDate,
                    showConfirmDatePicker = false,
                    skipItem = null,
                )
            }
        }

        fun setConfirmAmountText(text: String) {
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    R.string.error_amount_invalid
                } else {
                    null
                }
            _uiState.update { it.copy(confirmAmountText = text, confirmAmountErrorRes = error) }
        }

        fun openConfirmDatePicker() {
            _uiState.update { it.copy(showConfirmDatePicker = true) }
        }

        fun dismissConfirmDatePicker() {
            _uiState.update { it.copy(showConfirmDatePicker = false) }
        }

        fun setConfirmDate(date: LocalDate) {
            _uiState.update { it.copy(confirmDate = date, showConfirmDatePicker = false) }
        }

        fun dismissConfirm() {
            _uiState.update {
                it.copy(
                    confirmItem = null,
                    confirmAmountErrorRes = null,
                    showConfirmDatePicker = false,
                )
            }
        }

        fun confirmRecord() {
            val state = _uiState.value
            val item = state.confirmItem ?: return
            val amount = MoneyParser.parse(state.confirmAmountText)
            if (amount == null) {
                _uiState.update { it.copy(confirmAmountErrorRes = R.string.error_amount_invalid) }
                return
            }
            if (state.isProcessing) return
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                when (
                    val result =
                        confirmRecurringOccurrenceUseCase(
                            id = item.payment.id,
                            amount = amount,
                            date = state.confirmDate ?: item.payment.nextDueDate,
                        )
                ) {
                    is DomainResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                confirmItem = null,
                                showConfirmDatePicker = false,
                            )
                        }
                        _events.send(RecurringListEvent.ShowMessage(R.string.recurring_recorded))
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isProcessing = false) }
                        _events.send(RecurringListEvent.ShowMessage(result.error.asStringRes()))
                    }
                }
            }
        }

        fun openSkip(item: RecurringItem) {
            _uiState.update {
                it.copy(
                    skipItem = item,
                    confirmItem = null,
                    showConfirmDatePicker = false,
                )
            }
        }

        fun dismissSkip() {
            _uiState.update { it.copy(skipItem = null) }
        }

        fun confirmSkip() {
            val item = _uiState.value.skipItem ?: return
            if (_uiState.value.isProcessing) return
            _uiState.update { it.copy(isProcessing = true) }
            viewModelScope.launch {
                when (
                    val result = skipRecurringOccurrenceUseCase(id = item.payment.id)
                ) {
                    is DomainResult.Success -> {
                        _uiState.update { it.copy(isProcessing = false, skipItem = null) }
                        _events.send(RecurringListEvent.ShowMessage(R.string.recurring_recorded))
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(isProcessing = false) }
                        _events.send(RecurringListEvent.ShowMessage(result.error.asStringRes()))
                    }
                }
            }
        }
    }
