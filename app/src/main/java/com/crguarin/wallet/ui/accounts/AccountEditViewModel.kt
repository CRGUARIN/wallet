package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.usecase.SaveAccountUseCase
import com.crguarin.wallet.ui.navigation.AccountEdit
import com.crguarin.wallet.ui.theme.WalletPalette
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
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

data class AccountEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",
    val color: Int = WalletPalette.colors.first(),
    val icon: String = "wallet",
    val includeInTotal: Boolean = true,
    val originalInitialBalance: Money = Money.ZERO,
    val isSaving: Boolean = false,
    val nameErrorRes: Int? = null,
    val amountErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
) {
    val showInitialBalanceWarning: Boolean
        get() {
            if (!isEditMode) return false
            val parsed = MoneyParser.parseNonNegative(initialBalanceText) ?: return false
            return parsed != originalInitialBalance
        }

    val isSaveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                name.isNotBlank() &&
                name.trim().length <= 40 &&
                nameErrorRes == null &&
                amountErrorRes == null
}

sealed interface AccountEditEvent {
    data object NavigateBack : AccountEditEvent

    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : AccountEditEvent
}

@HiltViewModel
class AccountEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val accountRepository: AccountRepository,
        private val saveAccountUseCase: SaveAccountUseCase,
    ) : ViewModel() {
        val targetAccountId: Long? = savedStateHandle.toRoute<AccountEdit>().accountId

        private val _uiState = MutableStateFlow(AccountEditUiState())
        val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<AccountEditEvent>(Channel.BUFFERED)
        val events: Flow<AccountEditEvent> = _events.receiveAsFlow()

        private var existingAccounts: List<Account> = emptyList()

        init {
            viewModelScope.launch {
                existingAccounts = accountRepository.getAllAccounts()
                if (targetAccountId != null && targetAccountId > 0) {
                    _uiState.update { it.copy(isLoading = true, isEditMode = true, accountId = targetAccountId) }
                    val account = accountRepository.getAccount(targetAccountId)
                    if (account != null) {
                        val formattedBalance = formatInitialBalanceForInput(account.initialBalance)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                name = account.name,
                                type = account.type,
                                initialBalanceText = formattedBalance,
                                color = account.color,
                                icon = account.icon,
                                includeInTotal = account.includeInTotal,
                                originalInitialBalance = account.initialBalance,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }

        private fun formatInitialBalanceForInput(balance: Money): String =
            if (balance.minorUnits == 0L) {
                ""
            } else if (balance.minorUnits % 100L == 0L) {
                (balance.minorUnits / 100L).toString()
            } else {
                val major = balance.minorUnits / 100L
                val fraction = balance.minorUnits % 100L
                val separator = DecimalFormatSymbols.getInstance().decimalSeparator
                String.format(Locale.ROOT, "%d%c%02d", major, separator, fraction)
            }

        fun setName(name: String) {
            val trimmed = name.trim()
            val error =
                when {
                    trimmed.length > 40 -> DomainError.NameTooLong.asStringRes()
                    name.isNotEmpty() && trimmed.isEmpty() -> DomainError.NameEmpty.asStringRes()
                    trimmed.isNotEmpty() && isDuplicateName(trimmed) -> DomainError.NameDuplicated.asStringRes()
                    else -> null
                }
            _uiState.update { it.copy(name = name, nameErrorRes = error, generalErrorRes = null) }
        }

        private fun isDuplicateName(candidate: String): Boolean {
            val normalized = candidate.lowercase(Locale.ROOT)
            val currentId = _uiState.value.accountId
            return existingAccounts.any {
                !it.isArchived && it.id != currentId && it.name.trim().lowercase(Locale.ROOT) == normalized
            }
        }

        fun setType(type: AccountType) {
            val currentIcon = _uiState.value.icon
            val newIcon =
                when {
                    type == AccountType.SAVINGS && currentIcon == "wallet" -> "savings"
                    type == AccountType.CASH && currentIcon == "savings" -> "wallet"
                    else -> currentIcon
                }
            _uiState.update { it.copy(type = type, icon = newIcon) }
        }

        fun setInitialBalanceText(text: String) {
            val parsed = MoneyParser.parseNonNegative(text)
            val error = if (parsed == null) DomainError.AmountInvalid.asStringRes() else null
            _uiState.update { it.copy(initialBalanceText = text, amountErrorRes = error, generalErrorRes = null) }
        }

        fun setColor(color: Int) {
            _uiState.update { it.copy(color = color) }
        }

        fun setIcon(icon: String) {
            _uiState.update { it.copy(icon = icon) }
        }

        fun setIncludeInTotal(include: Boolean) {
            _uiState.update { it.copy(includeInTotal = include) }
        }

        fun saveAccount() {
            val state = _uiState.value
            val trimmedName = state.name.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameEmpty.asStringRes()) }
                return
            }
            if (trimmedName.length > 40) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameTooLong.asStringRes()) }
                return
            }
            if (isDuplicateName(trimmedName)) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameDuplicated.asStringRes()) }
                return
            }

            val balance = MoneyParser.parseNonNegative(state.initialBalanceText)
            if (balance == null) {
                _uiState.update { it.copy(amountErrorRes = DomainError.AmountInvalid.asStringRes()) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
                val account =
                    Account(
                        id = state.accountId ?: 0L,
                        name = trimmedName,
                        type = state.type,
                        currencyCode = "COP",
                        initialBalance = balance,
                        color = state.color,
                        icon = state.icon,
                        includeInTotal = state.includeInTotal,
                        isArchived = false,
                        sortOrder = 0,
                        createdAt = Instant.EPOCH,
                    )

                when (val result = saveAccountUseCase(account)) {
                    is DomainResult.Success -> {
                        _events.send(AccountEditEvent.NavigateBack)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                generalErrorRes = result.error.asStringRes(),
                            )
                        }
                    }
                }
            }
        }
    }
