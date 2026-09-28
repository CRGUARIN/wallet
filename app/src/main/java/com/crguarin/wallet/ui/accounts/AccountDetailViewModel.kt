package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val accountWithBalance: AccountWithBalance? = null,
    val monthSummary: MonthSummary = MonthSummary(Money.ZERO, Money.ZERO),
    val showDeleteConfirmDialog: Boolean = false,
    val showOfferArchiveDialog: Boolean = false,
    val errorMessageRes: Int? = null,
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
        private val archiveAccountUseCase: ArchiveAccountUseCase,
        private val unarchiveAccountUseCase: UnarchiveAccountUseCase,
        private val deleteAccountUseCase: DeleteAccountUseCase,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        val accountId: Long = savedStateHandle.toRoute<AccountDetail>().accountId

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

        val uiState: StateFlow<AccountDetailUiState> =
            combine(
                accountRepository.observeAccountWithBalance(accountId),
                observeMonthSummaryUseCase(currentYearMonth, accountId),
                dialogStateFlow,
            ) { account, monthSummary, dialogState ->
                AccountDetailUiState(
                    isLoading = false,
                    accountWithBalance = account,
                    monthSummary = monthSummary,
                    showDeleteConfirmDialog = dialogState.showDelete,
                    showOfferArchiveDialog = dialogState.showOfferArchive,
                )
            }.stateIn(
                scope = viewModelScope,
                started =
                    kotlinx.coroutines.flow.SharingStarted
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
