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
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.ui.navigation.AccountDetail
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    /** Default source account for "pay card": the last active non-card account, if any. */
    val payFromAccountId: Long? = null,
    /** Pending statement amount in minor units, or null when there is nothing to pay. */
    val payAmountMinor: Long? = null,
)

sealed interface AccountDetailEvent {
    data object NavigateBack : AccountDetailEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : AccountDetailEvent
}

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
        timeProvider: TimeProvider,
        observeCardOverviewUseCase: ObserveCardOverviewUseCase,
        cardStatementRepository: CardStatementRepository,
        private val transactionRepository: TransactionRepository,
    ) : ViewModel() {
        val accountId: Long = savedStateHandle.toRoute<AccountDetail>().accountId

        private val today: LocalDate = timeProvider.today()

        private val dialogStateFlow =
            MutableStateFlow(
                DialogState(
                    showDelete = false,
                    showOfferArchive = false,
                ),
            )

        private data class DialogState(
            val showDelete: Boolean,
            val showOfferArchive: Boolean,
        )

        private val _events = Channel<AccountDetailEvent>(Channel.BUFFERED)
        val events: Flow<AccountDetailEvent> = _events.receiveAsFlow()

        private val currentYearMonth = YearMonth.from(timeProvider.today())

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

        private val headerFlow =
            combine(
                accountRepository.observeAccountWithBalance(accountId),
                observeMonthSummaryUseCase(currentYearMonth, accountId),
                dialogStateFlow,
            ) { account, monthSummary, dialogState -> Triple(account, monthSummary, dialogState) }

        private val listsFlow =
            combine(
                accountRepository.observeAccountsWithBalance(includeArchived = false),
                observeDailyTotalsUseCase(accountFilter),
                combine(
                    observeCardOverviewUseCase(accountId),
                    cardStatementRepository.observeStatements(accountId),
                ) { overview, statements -> overview to statements },
            ) { activeAccounts, dailyTotals, cardDetail -> Triple(activeAccounts, dailyTotals, cardDetail) }

        val uiState: StateFlow<AccountDetailUiState> =
            combine(headerFlow, listsFlow) { header, lists ->
                val (account, monthSummary, dialogState) = header
                val (activeAccounts, dailyTotals, cardDetail) = lists
                val (cardOverview, statements) = cardDetail
                val recentStatements = statements.take(CARD_DETAIL_STATEMENTS_LIMIT)
                val adjustmentAmounts =
                    recentStatements
                        .mapNotNull { statement ->
                            val adjustmentId = statement.adjustmentTransactionId ?: return@mapNotNull null
                            val amount = transactionRepository.getTransaction(adjustmentId)?.amount
                            if (amount != null) statement.id to amount else null
                        }.toMap()
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
                    payFromAccountId =
                        activeAccounts
                            .filter { it.account.type != AccountType.CREDIT_CARD }
                            .lastOrNull()
                            ?.account
                            ?.id,
                    payAmountMinor = pending?.takeIf { it > Money.ZERO }?.minorUnits,
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
