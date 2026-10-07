package com.crguarin.wallet.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import com.crguarin.wallet.domain.usecase.ReorderAccountsUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AccountsUiState(
    val isLoading: Boolean = false,
    val totalBalance: Money = Money.ZERO,
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val archivedAccounts: List<AccountWithBalance> = emptyList(),
    val isArchivedExpanded: Boolean = false,
    val isReordering: Boolean = false,
    val cardOverviews: Map<Long, CardOverview> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
) {
    val isEmpty: Boolean
        get() = !isLoading && activeAccounts.isEmpty() && archivedAccounts.isEmpty()
}

@HiltViewModel
class AccountsViewModel
    @Inject
    constructor(
        observeAccountsUseCase: ObserveAccountsUseCase,
        observeTotalBalanceUseCase: ObserveTotalBalanceUseCase,
        private val reorderAccountsUseCase: ReorderAccountsUseCase,
        observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
        timeProvider: TimeProvider,
    ) : ViewModel() {
        private val isArchivedExpandedFlow = MutableStateFlow(false)
        private val isReorderingFlow = MutableStateFlow(false)

        val uiState: StateFlow<AccountsUiState> =
            combine(
                observeAccountsUseCase(includeArchived = true),
                observeTotalBalanceUseCase(),
                isArchivedExpandedFlow,
                isReorderingFlow,
                combine(observeCardOverviewsUseCase(), timeProvider.todayFlow(), ::Pair),
            ) { accounts, totalBalance, isArchivedExpanded, isReordering, (cardOverviews, today) ->
                val active = accounts.filter { !it.account.isArchived }
                val archived = accounts.filter { it.account.isArchived }
                AccountsUiState(
                    isLoading = false,
                    totalBalance = totalBalance,
                    activeAccounts = active,
                    archivedAccounts = archived,
                    isArchivedExpanded = isArchivedExpanded,
                    isReordering = isReordering,
                    cardOverviews = cardOverviews,
                    today = today,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AccountsUiState(isLoading = true),
            )

        fun toggleArchivedExpanded() {
            isArchivedExpandedFlow.update { !it }
        }

        fun toggleReordering() {
            isReorderingFlow.update { !it }
        }

        fun moveAccountUp(accountId: Long) {
            val active = uiState.value.activeAccounts
            val index = active.indexOfFirst { it.account.id == accountId }
            if (index > 0) {
                val mutable = active.map { it.account.id }.toMutableList()
                val temp = mutable[index]
                mutable[index] = mutable[index - 1]
                mutable[index - 1] = temp
                viewModelScope.launch {
                    reorderAccountsUseCase(mutable)
                }
            }
        }

        fun moveAccountDown(accountId: Long) {
            val active = uiState.value.activeAccounts
            val index = active.indexOfFirst { it.account.id == accountId }
            if (index in 0 until active.size - 1) {
                val mutable = active.map { it.account.id }.toMutableList()
                val temp = mutable[index]
                mutable[index] = mutable[index + 1]
                mutable[index + 1] = temp
                viewModelScope.launch {
                    reorderAccountsUseCase(mutable)
                }
            }
        }
    }
