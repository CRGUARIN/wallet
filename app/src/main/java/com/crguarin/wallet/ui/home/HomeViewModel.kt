package com.crguarin.wallet.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtsOverviewUseCase
import com.crguarin.wallet.domain.usecase.recurring.ObserveUpcomingPaymentsUseCase
import com.crguarin.wallet.domain.usecase.transit.RegisterTripUseCase
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Number of recent movements shown on Home. */
const val HOME_RECENT_LIMIT = 10

/** Days covered by the "upcoming payments" card. */
private const val UPCOMING_DAYS = 7

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalBalance: Money = Money.ZERO,
    val monthSummary: MonthSummary = MonthSummary(Money.ZERO, Money.ZERO),
    val accounts: List<AccountWithBalance> = emptyList(),
    val recentTransactions: List<TransactionDetails> = emptyList(),
    val cardOverviews: Map<Long, CardOverview> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    /** Debts overview; null until it is loaded. The debts card shows only when there are active debts. */
    val debts: DebtsOverview? = null,
    /** The "choose the card" sheet of the trip shortcut is open (only offered with several transit cards). */
    val showTripChooser: Boolean = false,
) {
    /** A transfer needs at least two active accounts. */
    val canTransfer: Boolean
        get() = accounts.size >= 2

    /** Active transit cards: the trip shortcut shows up when there is at least one. */
    val transitCards: List<AccountWithBalance>
        get() = accounts.filter { it.account.type == AccountType.TRANSIT_CARD && !it.account.isArchived }

    val canRegisterTrip: Boolean
        get() = transitCards.isNotEmpty()

    val hasNoTransactions: Boolean
        get() = !isLoading && recentTransactions.isEmpty()
}

sealed interface HomeEvent {
    /** A trip was registered; the screen offers to undo it with [transactionId]. */
    data class TripRegistered(
        val transactionId: Long,
    ) : HomeEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeTotalBalanceUseCase: ObserveTotalBalanceUseCase,
        observeMonthSummaryUseCase: ObserveMonthSummaryUseCase,
        observeAccountsUseCase: ObserveAccountsUseCase,
        transactionRepository: TransactionRepository,
        timeProvider: TimeProvider,
        observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
        observeUpcomingPaymentsUseCase: ObserveUpcomingPaymentsUseCase,
        observeDebtsOverviewUseCase: ObserveDebtsOverviewUseCase,
        private val registerTripUseCase: RegisterTripUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
    ) : ViewModel() {
        private val tripChooserFlow = MutableStateFlow(false)
        private var tripJob: Job? = null

        private val _events = Channel<HomeEvent>(Channel.BUFFERED)
        val events: Flow<HomeEvent> = _events.receiveAsFlow()

        // Rebuilt when the day changes, so the month summary follows the calendar while the app stays open.
        val uiState: StateFlow<HomeUiState> =
            timeProvider
                .todayFlow()
                .flatMapLatest { today ->
                    combine(
                        combine(
                            observeTotalBalanceUseCase(),
                            observeMonthSummaryUseCase(YearMonth.from(today)),
                            observeAccountsUseCase(),
                            transactionRepository.observeRecentTransactions(HOME_RECENT_LIMIT),
                            observeCardOverviewsUseCase(),
                        ) { totalBalance, monthSummary, accounts, recentTransactions, cardOverviews ->
                            HomeUiState(
                                isLoading = false,
                                totalBalance = totalBalance,
                                monthSummary = monthSummary,
                                accounts = accounts,
                                recentTransactions = recentTransactions,
                                cardOverviews = cardOverviews,
                                today = today,
                            )
                        },
                        observeUpcomingPaymentsUseCase(UPCOMING_DAYS),
                        observeDebtsOverviewUseCase(),
                    ) { base, upcoming, debts ->
                        base.copy(upcomingPayments = upcoming, debts = debts)
                    }
                }.combine(tripChooserFlow) { state, chooserRequested ->
                    // The chooser only exists while there is a real choice to make.
                    state.copy(showTripChooser = chooserRequested && state.transitCards.size > 1)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HomeUiState(isLoading = true),
                )

        /**
         * One-tap trip. With one transit card the trip is registered right away; with several, the
         * screen asks which card first ([onTripCardSelected] continues from there).
         */
        fun onTripClick() {
            val cards = uiState.value.transitCards
            when {
                cards.isEmpty() -> Unit
                cards.size == 1 -> registerTrip(cards.single().account.id)
                else -> tripChooserFlow.value = true
            }
        }

        fun onTripCardSelected(accountId: Long) {
            tripChooserFlow.value = false
            registerTrip(accountId)
        }

        fun dismissTripChooser() {
            tripChooserFlow.value = false
        }

        /** Removes the trip that was just registered. A trip that is already gone is not an error. */
        fun undoTrip(transactionId: Long) {
            viewModelScope.launch {
                when (val result = deleteTransactionUseCase(transactionId)) {
                    is DomainResult.Success -> Unit
                    is DomainResult.Failure -> {
                        if (result.error != DomainError.NotFound) {
                            _events.send(HomeEvent.ShowSnackbar(result.error.asStringRes()))
                        }
                    }
                }
            }
        }

        private fun registerTrip(accountId: Long) {
            // Two quick taps would both read the balance before the first trip is saved and use the credit twice.
            if (tripJob?.isActive == true) return
            tripJob =
                viewModelScope.launch {
                    when (val result = registerTripUseCase(accountId)) {
                        is DomainResult.Success -> _events.send(HomeEvent.TripRegistered(result.value.transactionId))
                        is DomainResult.Failure -> _events.send(HomeEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
        }
    }
