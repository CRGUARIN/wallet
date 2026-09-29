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
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionDaySeparators
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveAccountUseCase
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

        val uiState: StateFlow<AccountDetailUiState> =
            combine(
                accountRepository.observeAccountWithBalance(accountId),
                observeMonthSummaryUseCase(currentYearMonth, accountId),
                dialogStateFlow,
                accountRepository.observeAccountsWithBalance(includeArchived = false),
                observeDailyTotalsUseCase(accountFilter),
            ) { account, monthSummary, dialogState, activeAccounts, dailyTotals ->
                AccountDetailUiState(
                    isLoading = false,
                    today = today,
                    accountWithBalance = account,
                    monthSummary = monthSummary,
                    showDeleteConfirmDialog = dialogState.showDelete,
                    showOfferArchiveDialog = dialogState.showOfferArchive,
                    canTransfer = activeAccounts.any { it.account.id != accountId },
                    dailyTotals = dailyTotals,
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
