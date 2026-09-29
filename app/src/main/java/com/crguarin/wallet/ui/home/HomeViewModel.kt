package com.crguarin.wallet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

/** Number of recent movements shown on Home. */
const val HOME_RECENT_LIMIT = 10

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalBalance: Money = Money.ZERO,
    val monthSummary: MonthSummary = MonthSummary(Money.ZERO, Money.ZERO),
    val accounts: List<AccountWithBalance> = emptyList(),
    val recentTransactions: List<TransactionDetails> = emptyList(),
) {
    /** A transfer needs at least two active accounts. */
    val canTransfer: Boolean
        get() = accounts.size >= 2

    val hasNoTransactions: Boolean
        get() = !isLoading && recentTransactions.isEmpty()
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeTotalBalanceUseCase: ObserveTotalBalanceUseCase,
        observeMonthSummaryUseCase: ObserveMonthSummaryUseCase,
        observeAccountsUseCase: ObserveAccountsUseCase,
        transactionRepository: TransactionRepository,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        private val currentMonth: YearMonth = YearMonth.from(timeProvider.today())

        val uiState: StateFlow<HomeUiState> =
            combine(
                observeTotalBalanceUseCase(),
                observeMonthSummaryUseCase(currentMonth),
                observeAccountsUseCase(),
                transactionRepository.observeRecentTransactions(HOME_RECENT_LIMIT),
            ) { totalBalance, monthSummary, accounts, recentTransactions ->
                HomeUiState(
                    isLoading = false,
                    totalBalance = totalBalance,
                    monthSummary = monthSummary,
                    accounts = accounts,
                    recentTransactions = recentTransactions,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState(isLoading = true),
            )
    }
