package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionDaySeparators
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.transit.RegisterTripUseCase
import com.crguarin.wallet.ui.navigation.AccountDetail
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Number of recent statements shown in the card detail. */
const val CARD_DETAIL_STATEMENTS_LIMIT = 6

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val accountWithBalance: AccountWithBalance? = null,
    val monthSummary: MonthSummary = MonthSummary(Money.ZERO, Money.ZERO),
    val showDeleteConfirmDialog: Boolean = false,
    val showOfferArchiveDialog: Boolean = false,
    /** True when there is another active account to transfer to. */
    val canTransfer: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    /** Net per day from this account's perspective, shown in the day headers of the list. */
    val dailyTotals: Map<LocalDate, Money> = emptyMap(),
    /** Null unless the account is a credit card. */
    val cardOverview: CardOverview? = null,
    /** Newest statements first, limited to [CARD_DETAIL_STATEMENTS_LIMIT]. */
    val statements: List<CardStatement> = emptyList(),
    /** Adjustment movement amount per statement id, for statements that have one. */
    val adjustmentAmounts: Map<Long, Money> = emptyMap(),
    /** Default source account for "pay card": the last used non-card account, or the first one. */
    val payFromAccountId: Long? = null,
    /** Pending statement amount in minor units, or null when there is nothing to pay. */
    val payAmountMinor: Long? = null,
    /** Default source account for topping up this transit card, or null when there is none. */
    val rechargeFromAccountId: Long? = null,
) {
    val isTransitCard: Boolean
        get() = accountWithBalance?.account?.type == AccountType.TRANSIT_CARD

    /** Whole trips the balance still pays; 0 without a fare. */
    val tripsLeft: Int
        get() = accountWithBalance?.let { TransitMath.tripsLeft(it.balance, it.account.fare) } ?: 0

    /** True when another trip is allowed: a transit card whose balance is not negative. */
    val canRegisterTrip: Boolean
        get() = accountWithBalance?.let { TransitMath.canTravel(it.balance) } ?: false

    /** True when the next trip goes through on credit: the balance is below one fare but not negative. */
    val isCreditTrip: Boolean
        get() = accountWithBalance?.let { TransitMath.isCreditTrip(it.balance, it.account.fare) } ?: false

    /** True while the credit trip is owed: the balance is negative until the next top-up. */
    val isCreditUsed: Boolean
        get() = accountWithBalance?.let { TransitMath.isCreditUsed(it.balance) } ?: false
}

sealed interface AccountDetailEvent {
    data object NavigateBack : AccountDetailEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : AccountDetailEvent

    /** A trip was registered; the screen offers to undo it with [transactionId]. */
    data class TripRegistered(
        val transactionId: Long,
    ) : AccountDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        accountRepository: AccountRepository,
        observeMonthSummaryUseCase: ObserveMonthSummaryUseCase,
        observeTransactionsUseCase: ObserveTransactionsUseCase,
        observeDailyTotalsUseCase: ObserveDailyTotalsUseCase,
        private val archiveAccountUseCase: ArchiveAccountUseCase,
        private val unarchiveAccountUseCase: UnarchiveAccountUseCase,
        private val deleteAccountUseCase: DeleteAccountUseCase,
        private val registerTripUseCase: RegisterTripUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        timeProvider: TimeProvider,
        observeCardOverviewUseCase: ObserveCardOverviewUseCase,
        cardStatementRepository: CardStatementRepository,
        preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        val accountId: Long = savedStateHandle.toRoute<AccountDetail>().accountId

        private val dialogStateFlow =
            MutableStateFlow(
                DialogState(
                    showDelete = false,
                    showOfferArchive = false,
                ),
            )

        private data class CardDetail(
            val overview: CardOverview?,
            val statements: List<CardStatement>,
            val adjustmentAmounts: Map<Long, Money>,
            val lastUsedAccountId: Long?,
        )

        private data class DialogState(
            val showDelete: Boolean,
            val showOfferArchive: Boolean,
        )

        private var tripJob: Job? = null

        private val _events = Channel<AccountDetailEvent>(Channel.BUFFERED)
        val events: Flow<AccountDetailEvent> = _events.receiveAsFlow()

        private val accountFilter = TransactionFilter(accountId = accountId)

        /**
         * Paginated transactions of this account, grouped by day. The day totals live in
         * [AccountDetailUiState.dailyTotals] so the same `PagingData` is never collected twice.
         */
        val pagingData: Flow<PagingData<TransactionListEntry>> =
            observeTransactionsUseCase(accountFilter)
                .map { paging ->
                    paging
                        .map { TransactionListEntry.Item(it) }
                        .insertSeparators<TransactionListEntry.Item, TransactionListEntry> { before, after ->
                            TransactionDaySeparators.headerFor(before, after)
                        }
                }.cachedIn(viewModelScope)

        /** Today with this month's summary, re-queried when the day changes. */
        private val todaySummaryFlow: Flow<Pair<LocalDate, MonthSummary>> =
            timeProvider.todayFlow().flatMapLatest { today ->
                observeMonthSummaryUseCase(YearMonth.from(today), accountId).map { summary -> today to summary }
            }

        private val headerFlow =
            combine(
                accountRepository.observeAccountWithBalance(accountId),
                todaySummaryFlow,
                dialogStateFlow,
            ) { account, todaySummary, dialogState -> Triple(account, todaySummary, dialogState) }

        private val listsFlow =
            combine(
                accountRepository.observeAccountsWithBalance(includeArchived = false),
                observeDailyTotalsUseCase(accountFilter),
                combine(
                    observeCardOverviewUseCase(accountId),
                    cardStatementRepository.observeStatements(accountId),
                    cardStatementRepository.observeAdjustmentAmounts(accountId),
                    preferencesRepository.observeUserPreferences(),
                ) { overview, statements, adjustmentAmounts, preferences ->
                    CardDetail(overview, statements, adjustmentAmounts, preferences.lastUsedAccountId)
                },
            ) { activeAccounts, dailyTotals, cardDetail -> Triple(activeAccounts, dailyTotals, cardDetail) }

        val uiState: StateFlow<AccountDetailUiState> =
            combine(headerFlow, listsFlow) { header, lists ->
                val (account, todaySummary, dialogState) = header
                val (today, monthSummary) = todaySummary
                val (activeAccounts, dailyTotals, cardDetail) = lists
                val (cardOverview, statements, allAdjustmentAmounts, lastUsedAccountId) = cardDetail
                val recentStatements = statements.take(CARD_DETAIL_STATEMENTS_LIMIT)
                val recentIds = recentStatements.map { it.id }.toSet()
                val adjustmentAmounts = allAdjustmentAmounts.filterKeys { it in recentIds }
                val pending = cardOverview?.pendingStatementAmount
                AccountDetailUiState(
                    isLoading = false,
                    today = today,
                    accountWithBalance = account,
                    monthSummary = monthSummary,
                    showDeleteConfirmDialog = dialogState.showDelete,
                    showOfferArchiveDialog = dialogState.showOfferArchive,
                    canTransfer = activeAccounts.any { it.account.id != accountId },
                    dailyTotals = dailyTotals,
                    cardOverview = cardOverview,
                    statements = recentStatements,
                    adjustmentAmounts = adjustmentAmounts,
                    payFromAccountId = payFromAccountIdOf(activeAccounts, lastUsedAccountId),
                    payAmountMinor = pending?.takeIf { it > Money.ZERO }?.minorUnits,
                    rechargeFromAccountId = rechargeFromAccountIdOf(activeAccounts, lastUsedAccountId, accountId),
                )
            }.stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(5_000),
                initialValue = AccountDetailUiState(isLoading = true),
            )

        fun showDeleteDialog() {
            dialogStateFlow.update { it.copy(showDelete = true, showOfferArchive = false) }
        }

        fun dismissDialogs() {
            dialogStateFlow.update { it.copy(showDelete = false, showOfferArchive = false) }
        }

        /** Registers a trip; the use case refuses it while the credit trip is still owed. */
        fun onRegisterTripClick() {
            registerTrip()
        }

        fun undoTrip(transactionId: Long) {
            viewModelScope.launch {
                when (val result = deleteTransactionUseCase(transactionId)) {
                    is DomainResult.Success -> Unit
                    is DomainResult.Failure -> {
                        if (result.error != DomainError.NotFound) {
                            _events.send(AccountDetailEvent.ShowSnackbar(result.error.asStringRes()))
                        }
                    }
                }
            }
        }

        private fun registerTrip() {
            // Two quick taps would both read the balance before the first trip is saved and use the credit twice.
            if (tripJob?.isActive == true) return
            tripJob =
                viewModelScope.launch {
                    when (val result = registerTripUseCase(accountId)) {
                        is DomainResult.Success -> {
                            _events.send(AccountDetailEvent.TripRegistered(result.value.transactionId))
                        }
                        is DomainResult.Failure -> {
                            _events.send(AccountDetailEvent.ShowSnackbar(result.error.asStringRes()))
                        }
                    }
                }
        }

        fun deleteAccount() {
            dismissDialogs()
            viewModelScope.launch {
                when (val result = deleteAccountUseCase(accountId)) {
                    is DomainResult.Success -> {
                        _events.send(AccountDetailEvent.NavigateBack)
                    }
                    is DomainResult.Failure -> {
                        if (result.error is DomainError.AccountHasTransactions) {
                            dialogStateFlow.update { it.copy(showDelete = false, showOfferArchive = true) }
                        } else {
                            _events.send(AccountDetailEvent.ShowSnackbar(result.error.asStringRes()))
                        }
                    }
                }
            }
        }

        fun confirmArchiveInsteadOfDelete() {
            dismissDialogs()
            archiveAccount()
        }

        fun archiveAccount() {
            viewModelScope.launch {
                when (val result = archiveAccountUseCase(accountId)) {
                    is DomainResult.Success -> {
                        _events.send(AccountDetailEvent.ShowSnackbar(R.string.account_detail_archived_success))
                    }
                    is DomainResult.Failure -> {
                        _events.send(AccountDetailEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
            }
        }

        fun unarchiveAccount() {
            viewModelScope.launch {
                when (val result = unarchiveAccountUseCase(accountId)) {
                    is DomainResult.Success -> {
                        _events.send(AccountDetailEvent.ShowSnackbar(R.string.account_detail_unarchived_success))
                    }
                    is DomainResult.Failure -> {
                        _events.send(AccountDetailEvent.ShowSnackbar(result.error.asStringRes()))
                    }
                }
            }
        }
    }

/**
 * Default source account for paying a card: the last used account when it is an active
 * non-card account, otherwise the first active non-card account in the user's order.
 * Transit cards cannot pay a card either.
 */
private fun payFromAccountIdOf(
    activeAccounts: List<AccountWithBalance>,
    lastUsedAccountId: Long?,
): Long? {
    val candidates =
        activeAccounts
            .map { it.account }
            .filter { it.type != AccountType.CREDIT_CARD && it.type != AccountType.TRANSIT_CARD }
    return candidates.firstOrNull { it.id == lastUsedAccountId }?.id
        ?: candidates.minByOrNull { it.sortOrder }?.id
}

/**
 * Default source account for topping up a transit card: the last used cash or savings account
 * other than [targetAccountId], otherwise the first one in the user's order.
 */
private fun rechargeFromAccountIdOf(
    activeAccounts: List<AccountWithBalance>,
    lastUsedAccountId: Long?,
    targetAccountId: Long,
): Long? {
    val candidates =
        activeAccounts
            .map { it.account }
            .filter { it.id != targetAccountId && (it.type == AccountType.CASH || it.type == AccountType.SAVINGS) }
    return candidates.firstOrNull { it.id == lastUsedAccountId }?.id
        ?: candidates.minByOrNull { it.sortOrder }?.id
}
