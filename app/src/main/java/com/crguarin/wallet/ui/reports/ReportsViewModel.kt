package com.crguarin.wallet.ui.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveCategoryBreakdownUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveMonthlyTotalsUseCase
import com.crguarin.wallet.domain.usecase.report.ObservePeriodSummaryUseCase
import com.crguarin.wallet.domain.usecase.report.ObserveSpendingPaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel of the Reports screen. Selection persists in [SavedStateHandle] with `reports_` keys.
 *
 * Public API frozen for the screen: [uiState] plus the action methods below.
 */
@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val observeCategoryBreakdownUseCase: ObserveCategoryBreakdownUseCase,
        private val observeMonthlyTotalsUseCase: ObserveMonthlyTotalsUseCase,
        private val observePeriodSummaryUseCase: ObservePeriodSummaryUseCase,
        private val observeSpendingPaceUseCase: ObserveSpendingPaceUseCase,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        val uiState: StateFlow<ReportsUiState> = TODO("Implemented by subagent D1")

        fun setMode(mode: ReportMode): Unit = TODO()

        fun previousPeriod(): Unit = TODO()

        fun nextPeriod(): Unit = TODO()

        fun setType(type: TransactionType): Unit = TODO()

        fun setAccount(accountId: Long?): Unit = TODO()

        fun openAccountSheet(): Unit = TODO()

        fun dismissAccountSheet(): Unit = TODO()

        /** Selects [key]; selecting the already selected slice deselects it. */
        fun selectSlice(key: SliceKey): Unit = TODO()

        fun drillInto(parentId: Long): Unit = TODO()

        fun drillUp(): Unit = TODO()

        /** Called from the bar chart: switches to MONTH mode and selects [yearMonth]. */
        fun selectMonth(yearMonth: YearMonth): Unit = TODO()

        /** Retries after a flow error: resets the selection to its initial values. */
        fun retry(): Unit = TODO()
    }
