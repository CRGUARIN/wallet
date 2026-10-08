package com.crguarin.wallet.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtsOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Debts list state: the overview, today (to flag overdue debts) and whether the settled
 * section is open. The settled section starts collapsed.
 */
data class DebtListUiState(
    val isLoading: Boolean = true,
    val overview: DebtsOverview? = null,
    val settledExpanded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

@HiltViewModel
class DebtListViewModel
    @Inject
    constructor(
        observeDebtsOverviewUseCase: ObserveDebtsOverviewUseCase,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DebtListUiState(today = timeProvider.today()))
        val uiState: StateFlow<DebtListUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observeDebtsOverviewUseCase().collect { overview ->
                    _uiState.update { it.copy(isLoading = false, overview = overview) }
                }
            }
            viewModelScope.launch {
                timeProvider.todayFlow().collect { today -> _uiState.update { it.copy(today = today) } }
            }
        }

        fun toggleSettledExpanded() {
            _uiState.update { it.copy(settledExpanded = !it.settledExpanded) }
        }
    }
