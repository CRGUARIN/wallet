package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.formatForInput
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.ColorRules
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.normalizedName
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
import java.time.Instant
import javax.inject.Inject

data class AccountEditUiState(
    val isLoading: Boolean = false,
    /** Edit mode for an id that no longer exists. */
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",
    val creditLimitText: String = "",
    val statementDayText: String = "",
    val paymentDueDayText: String = "",
    val fareText: String = "",
    /** Transit cards only: the amount typed is a negative balance (a credit trip still owed). */
    val transitBalanceNegative: Boolean = false,
    val color: Int = WalletPalette.colors.first(),
    /** Colors of the other active accounts; the picker shows them crossed out. */
    val usedColors: Set<Int> = emptySet(),
    val icon: String = "wallet",
    val includeInTotal: Boolean = true,
    val originalInitialBalance: Money = Money.ZERO,
    /** True in edit mode when the account already has transactions: the type cannot change. */
    val hasTransactions: Boolean = false,
    val isSaving: Boolean = false,
    val nameErrorRes: Int? = null,
    val amountErrorRes: Int? = null,
    val creditLimitErrorRes: Int? = null,
    val statementDayErrorRes: Int? = null,
    val paymentDueDayErrorRes: Int? = null,
    val fareErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
) {
    val isCreditCard: Boolean
        get() = type == AccountType.CREDIT_CARD

    val isTransitCard: Boolean
        get() = type == AccountType.TRANSIT_CARD

    /** The type selector is locked in edit mode when the account has transactions. */
    val isTypeLocked: Boolean
        get() = isEditMode && hasTransactions

    val showInitialBalanceWarning: Boolean
        get() {
            if (!isEditMode) return false
            val parsed = MoneyParser.parseNonNegative(initialBalanceText) ?: return false
            val effective =
                when {
                    isCreditCard -> cardInitialBalanceToSave(parsed, originalInitialBalance)
                    isTransitCard -> transitInitialBalanceToSave(parsed, transitBalanceNegative)
                    else -> parsed
                }
            return effective != originalInitialBalance
        }

    val isSaveEnabled: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                name.isNotBlank() &&
                name.trim().length <= DomainLimits.NAME_MAX_LENGTH &&
                nameErrorRes == null &&
                amountErrorRes == null &&
                creditLimitErrorRes == null &&
                statementDayErrorRes == null &&
                paymentDueDayErrorRes == null &&
                fareErrorRes == null
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

        private val _uiState =
            MutableStateFlow(
                AccountEditUiState(
                    isLoading = targetAccountId != null && targetAccountId > 0,
                    isEditMode = targetAccountId != null && targetAccountId > 0,
                ),
            )
        val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<AccountEditEvent>(Channel.BUFFERED)
        val events: Flow<AccountEditEvent> = _events.receiveAsFlow()

        private var existingAccounts: List<Account> = emptyList()

        init {
            viewModelScope.launch {
                existingAccounts = accountRepository.getAllAccounts()
                val usedColors = ColorRules.usedAccountColors(existingAccounts, targetAccountId?.takeIf { it > 0 })
                _uiState.update { it.copy(usedColors = usedColors) }
                if (targetAccountId == null || targetAccountId <= 0) {
                    _uiState.update {
                        it.copy(color = ColorRules.firstFreeColor(WalletPalette.colors, usedColors))
                    }
                }
                if (targetAccountId != null && targetAccountId > 0) {
                    _uiState.update { it.copy(isLoading = true, isEditMode = true, accountId = targetAccountId) }
                    val account = accountRepository.getAccount(targetAccountId)
                    if (account != null) {
                        val hasTransactions = accountRepository.countTransactions(targetAccountId) > 0
                        // A card stores -debt as initialBalance; the form asks for the debt (>= 0).
                        val debtText =
                            if (account.type == AccountType.CREDIT_CARD) {
                                val debt = -account.initialBalance
                                formatForInput(debt.takeIf { it > Money.ZERO } ?: Money.ZERO)
                            } else {
                                formatForInput(account.initialBalance)
                            }
                        // A transit card with a negative balance is edited as its absolute value plus the toggle.
                        val transitText =
                            if (account.type == AccountType.TRANSIT_CARD) {
                                formatForInput(if (account.initialBalance < Money.ZERO) -account.initialBalance else account.initialBalance)
                            } else {
                                debtText
                            }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                name = account.name,
                                type = account.type,
                                initialBalanceText = transitText,
                                creditLimitText =
                                    account.creditLimit?.let { limit ->
                                        formatForInput(limit)
                                    } ?: "",
                                statementDayText = account.statementDay?.toString() ?: "",
                                paymentDueDayText = account.paymentDueDay?.toString() ?: "",
                                fareText = account.fare?.let { formatForInput(it) } ?: "",
                                transitBalanceNegative = account.type == AccountType.TRANSIT_CARD && account.initialBalance < Money.ZERO,
                                color = account.color,
                                icon = account.icon,
                                includeInTotal = account.includeInTotal,
                                originalInitialBalance = account.initialBalance,
                                hasTransactions = hasTransactions,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                    }
                }
            }
        }

        fun setName(name: String) {
            val trimmed = name.trim()
            val error =
                accountNameInputError(name)
                    ?: if (trimmed.isNotEmpty() && isDuplicateName(trimmed)) {
                        DomainError.NameDuplicated.asStringRes()
                    } else {
                        null
                    }
            _uiState.update { it.copy(name = name, nameErrorRes = error, generalErrorRes = null) }
        }

        private fun isDuplicateName(candidate: String): Boolean {
            val normalized = candidate.normalizedName()
            val currentId = _uiState.value.accountId
            return existingAccounts.any {
                !it.isArchived && it.id != currentId && it.name.normalizedName() == normalized
            }
        }

        fun setType(type: AccountType) {
            if (_uiState.value.isTypeLocked) return
            _uiState.update {
                // Only a transit card can owe the credit trip. Leaving the type clears that debt instead
                // of saving the typed amount as a positive balance.
                val dropsCredit = it.transitBalanceNegative && type != AccountType.TRANSIT_CARD
                val next =
                    it.copy(
                        type = type,
                        icon = iconAfterTypeChange(type, it.icon),
                        // The fare error only concerns transit cards; saving checks the fare again.
                        fareErrorRes = if (type == AccountType.TRANSIT_CARD) it.fareErrorRes else null,
                        transitBalanceNegative = it.transitBalanceNegative && !dropsCredit,
                        initialBalanceText = if (dropsCredit) formatForInput(Money.ZERO) else it.initialBalanceText,
                        amountErrorRes = if (dropsCredit) null else it.amountErrorRes,
                    )
                next.withTransitCreditCheck()
            }
        }

        fun setInitialBalanceText(text: String) {
            _uiState.update {
                val next = it.copy(initialBalanceText = text, generalErrorRes = null)
                val invalid = if (MoneyParser.parseNonNegative(text) == null) DomainError.AmountInvalid.asStringRes() else null
                next.copy(amountErrorRes = invalid ?: transitCreditError(next))
            }
        }

        fun setTransitBalanceNegative(negative: Boolean) {
            _uiState.update { it.copy(transitBalanceNegative = negative, generalErrorRes = null).withTransitCreditCheck() }
        }

        fun setCreditLimitText(text: String) {
            _uiState.update {
                it.copy(creditLimitText = text, creditLimitErrorRes = creditLimitError(text), generalErrorRes = null)
            }
        }

        fun setStatementDayText(text: String) {
            _uiState.update {
                it.copy(
                    statementDayText = text,
                    statementDayErrorRes = cardDayError(text),
                    generalErrorRes = null,
                )
            }
        }

        fun setPaymentDueDayText(text: String) {
            _uiState.update {
                it.copy(
                    paymentDueDayText = text,
                    paymentDueDayErrorRes = cardDayError(text),
                    generalErrorRes = null,
                )
            }
        }

        fun setFareText(text: String) {
            _uiState.update {
                it
                    .copy(
                        fareText = text,
                        fareErrorRes = if (parseFare(text) == null) R.string.error_fare_invalid else null,
                        generalErrorRes = null,
                    ).withTransitCreditCheck()
            }
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
            if (_uiState.value.isSaving) return
            val state = _uiState.value
            val trimmedName = state.name.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(nameErrorRes = DomainError.NameEmpty.asStringRes()) }
                return
            }
            if (trimmedName.length > DomainLimits.NAME_MAX_LENGTH) {
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

            val isCard = state.type == AccountType.CREDIT_CARD
            val creditLimit = if (isCard) MoneyParser.parseNonNegative(state.creditLimitText) else null
            val statementDay = if (isCard) parseCardDay(state.statementDayText) else null
            val paymentDueDay = if (isCard) parseCardDay(state.paymentDueDayText) else null
            if (isCard) {
                val limitError = creditLimitError(state.creditLimitText)
                val statementError = cardDayError(state.statementDayText)
                val dueError = cardDayError(state.paymentDueDayText)
                if (limitError != null || statementError != null || dueError != null || creditLimit == null) {
                    _uiState.update {
                        it.copy(
                            creditLimitErrorRes = limitError,
                            statementDayErrorRes = statementError,
                            paymentDueDayErrorRes = dueError,
                        )
                    }
                    return
                }
            }

            val isTransit = state.type == AccountType.TRANSIT_CARD
            val fare = if (isTransit) parseFare(state.fareText) else null
            if (isTransit && fare == null) {
                _uiState.update { it.copy(fareErrorRes = R.string.error_fare_invalid) }
                return
            }
            val creditError = transitCreditError(state)
            if (creditError != null) {
                _uiState.update { it.copy(amountErrorRes = creditError) }
                return
            }

            _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
            viewModelScope.launch {
                val account =
                    Account(
                        id = state.accountId ?: 0L,
                        name = trimmedName,
                        type = state.type,
                        currencyCode = APP_CURRENCY,
                        // Cards ask for the current debt (>= 0) and store it negated.
                        initialBalance =
                            when {
                                isCard -> cardInitialBalanceToSave(balance, state.originalInitialBalance)
                                isTransit -> transitInitialBalanceToSave(balance, state.transitBalanceNegative)
                                else -> balance
                            },
                        color = state.color,
                        icon = state.icon,
                        includeInTotal = state.includeInTotal,
                        isArchived = false,
                        sortOrder = 0,
                        createdAt = Instant.EPOCH,
                        creditLimit = creditLimit,
                        statementDay = statementDay,
                        paymentDueDay = paymentDueDay,
                        fare = fare,
                    )

                when (val result = saveAccountUseCase(account)) {
                    is DomainResult.Success -> {
                        _events.send(AccountEditEvent.NavigateBack)
                    }
                    is DomainResult.Failure -> {
                        _uiState.update {
                            if (result.error is DomainError.TransitCreditTooLarge) {
                                it.copy(isSaving = false, amountErrorRes = result.error.asStringRes())
                            } else {
                                it.copy(isSaving = false, generalErrorRes = result.error.asStringRes())
                            }
                        }
                    }
                }
            }
        }
    }

/**
 * Initial balance to save for a card from the entered [debt]: the debt negated, except an
 * existing credit in favor (positive initial) is preserved while the debt stays zero, since the
 * form has no field to enter it. For new cards the original is zero, so it always stores `-debt`.
 */
private fun cardInitialBalanceToSave(
    debt: Money,
    originalInitialBalance: Money,
): Money =
    if (debt.isZero && originalInitialBalance.isPositive) {
        originalInitialBalance
    } else {
        -debt
    }

/**
 * Initial balance to save for a transit card: the amount negated while the balance owes the
 * credit trip, otherwise the amount as typed.
 */
private fun transitInitialBalanceToSave(
    amount: Money,
    negative: Boolean,
): Money = if (negative) -amount else amount

/**
 * Error for a negative transit balance larger than one fare, or null. Only applies when the type is a
 * transit card, the toggle is on, and both the amount and the fare parse.
 */
private fun transitCreditError(state: AccountEditUiState): Int? {
    if (!state.isTransitCard || !state.transitBalanceNegative) return null
    val amount = MoneyParser.parseNonNegative(state.initialBalanceText) ?: return null
    val fare = parseFare(state.fareText) ?: return null
    return if (amount > fare) R.string.error_transit_credit_too_large else null
}

/** Re-validates the credit-too-large error, leaving other amount errors untouched. */
private fun AccountEditUiState.withTransitCreditCheck(): AccountEditUiState {
    if (amountErrorRes != null && amountErrorRes != R.string.error_transit_credit_too_large) return this
    return copy(amountErrorRes = transitCreditError(this))
}

private fun creditLimitError(text: String): Int? {
    val parsed = MoneyParser.parseNonNegative(text)
    return if (parsed == null || parsed <= Money.ZERO) R.string.card_error_limit_required else null
}

private fun cardDayError(text: String): Int? {
    if (parseCardDay(text) == null) return R.string.card_error_day_invalid
    return null
}

/** Fare per trip from the form: a positive amount, or null when it is missing or not greater than zero. */
private fun parseFare(text: String): Money? = MoneyParser.parseNonNegative(text)?.takeIf { it > Money.ZERO }
