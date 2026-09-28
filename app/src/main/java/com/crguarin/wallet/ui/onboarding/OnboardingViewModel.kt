package com.crguarin.wallet.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.usecase.CompleteOnboardingUseCase
import com.crguarin.wallet.ui.theme.WalletPalette
import com.crguarin.wallet.ui.util.AppLanguage
import com.crguarin.wallet.ui.util.LanguageHelper
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
import java.time.Instant
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 1,
    val selectedLanguage: AppLanguage = LanguageHelper.getCurrentLanguage(),
    val accountName: String = "",
    val accountType: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",
    val selectedColor: Int = WalletPalette.colors.first(),
    val selectedIcon: String = "wallet",
    val includeInTotal: Boolean = true,
    val isSaving: Boolean = false,
    val nameErrorRes: Int? = null,
    val amountErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
) {
    val isAccountValid: Boolean
        get() =
            accountName.isNotBlank() &&
                accountName.trim().length <= 40 &&
                nameErrorRes == null &&
                amountErrorRes == null &&
                !isSaving
}

sealed interface OnboardingEvent {
    data object NavigateToHome : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val completeOnboardingUseCase: CompleteOnboardingUseCase,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                OnboardingUiState(
                    step = savedStateHandle.get<Int>(KEY_STEP) ?: 1,
                    selectedLanguage = LanguageHelper.getCurrentLanguage(),
                    accountName = savedStateHandle.get<String>(KEY_ACCOUNT_NAME) ?: "",
                    accountType =
                        savedStateHandle.get<String>(KEY_ACCOUNT_TYPE)?.let { AccountType.valueOf(it) }
                            ?: AccountType.CASH,
                    initialBalanceText = savedStateHandle.get<String>(KEY_INITIAL_BALANCE) ?: "",
                    selectedColor = savedStateHandle.get<Int>(KEY_COLOR) ?: WalletPalette.colors.first(),
                    selectedIcon = savedStateHandle.get<String>(KEY_ICON) ?: "wallet",
                    includeInTotal = savedStateHandle.get<Boolean>(KEY_INCLUDE_IN_TOTAL) ?: true,
                ),
            )
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
        val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

        fun setLanguage(language: AppLanguage) {
            LanguageHelper.setLanguage(language)
            _uiState.update { it.copy(selectedLanguage = language) }
        }

        fun goToStep(step: Int) {
            savedStateHandle[KEY_STEP] = step
            _uiState.update { it.copy(step = step) }
        }

        fun suggestDefaultNameIfEmpty(suggestedName: String) {
            if (_uiState.value.accountName.isEmpty()) {
                setAccountName(suggestedName)
            }
        }

        fun setAccountName(name: String) {
            savedStateHandle[KEY_ACCOUNT_NAME] = name
            val trimmed = name.trim()
            val error =
                when {
                    trimmed.length > 40 -> DomainError.NameTooLong.asStringRes()
                    name.isNotEmpty() && trimmed.isEmpty() -> DomainError.NameEmpty.asStringRes()
                    else -> null
                }
            _uiState.update { it.copy(accountName = name, nameErrorRes = error, generalErrorRes = null) }
        }

        fun setAccountType(type: AccountType) {
            savedStateHandle[KEY_ACCOUNT_TYPE] = type.name
            val currentIcon = _uiState.value.selectedIcon
            val newIcon =
                when {
                    type == AccountType.SAVINGS && currentIcon == "wallet" -> "savings"
                    type == AccountType.CASH && currentIcon == "savings" -> "wallet"
                    else -> currentIcon
                }
            savedStateHandle[KEY_ICON] = newIcon
            _uiState.update { it.copy(accountType = type, selectedIcon = newIcon) }
        }

        fun setInitialBalanceText(text: String) {
            savedStateHandle[KEY_INITIAL_BALANCE] = text
            val parsed = MoneyParser.parseNonNegative(text)
            val error = if (parsed == null) DomainError.AmountInvalid.asStringRes() else null
            _uiState.update { it.copy(initialBalanceText = text, amountErrorRes = error, generalErrorRes = null) }
        }

        fun setColor(color: Int) {
            savedStateHandle[KEY_COLOR] = color
            _uiState.update { it.copy(selectedColor = color) }
        }

        fun setIcon(icon: String) {
            savedStateHandle[KEY_ICON] = icon
            _uiState.update { it.copy(selectedIcon = icon) }
        }

        fun setIncludeInTotal(include: Boolean) {
            savedStateHandle[KEY_INCLUDE_IN_TOTAL] = include
            _uiState.update { it.copy(includeInTotal = include) }
        }

        fun completeOnboarding() {
            val state = _uiState.value
            val trimmedName = state.accountName.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameEmpty.asStringRes()) }
                return
            }
            if (trimmedName.length > 40) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameTooLong.asStringRes()) }
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
                        id = 0L,
                        name = trimmedName,
                        type = state.accountType,
                        currencyCode = "COP",
                        initialBalance = balance,
                        color = state.selectedColor,
                        icon = state.selectedIcon,
                        includeInTotal = state.includeInTotal,
                        isArchived = false,
                        sortOrder = 0,
                        createdAt = Instant.EPOCH,
                    )

                when (val result = completeOnboardingUseCase(account)) {
                    is DomainResult.Success -> {
                        _events.send(OnboardingEvent.NavigateToHome)
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

        companion object {
            private const val KEY_STEP = "onboarding_step"
            private const val KEY_ACCOUNT_NAME = "onboarding_account_name"
            private const val KEY_ACCOUNT_TYPE = "onboarding_account_type"
            private const val KEY_INITIAL_BALANCE = "onboarding_initial_balance"
            private const val KEY_COLOR = "onboarding_color"
            private const val KEY_ICON = "onboarding_icon"
            private const val KEY_INCLUDE_IN_TOTAL = "onboarding_include_in_total"
        }
    }
