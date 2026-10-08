package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.ui.util.asStringRes

private const val ICON_WALLET = "wallet"
private const val ICON_SAVINGS = "savings"
private const val ICON_CREDIT_CARD = "credit_card"
private const val ICON_TRANSIT = "directions_bus"

/** Swaps the default icon of the previous type for the default icon of [newType]; custom icons are kept. */
fun iconAfterTypeChange(
    newType: AccountType,
    currentIcon: String,
): String =
    when {
        newType == AccountType.SAVINGS && currentIcon == ICON_WALLET -> ICON_SAVINGS
        newType == AccountType.CASH && currentIcon == ICON_SAVINGS -> ICON_WALLET
        newType == AccountType.CREDIT_CARD && (currentIcon == ICON_WALLET || currentIcon == ICON_SAVINGS) ->
            ICON_CREDIT_CARD
        newType == AccountType.CASH && currentIcon == ICON_CREDIT_CARD -> ICON_WALLET
        newType == AccountType.SAVINGS && currentIcon == ICON_CREDIT_CARD -> ICON_SAVINGS
        newType == AccountType.TRANSIT_CARD &&
            (currentIcon == ICON_WALLET || currentIcon == ICON_SAVINGS || currentIcon == ICON_CREDIT_CARD) ->
            ICON_TRANSIT
        newType == AccountType.CASH && currentIcon == ICON_TRANSIT -> ICON_WALLET
        newType == AccountType.SAVINGS && currentIcon == ICON_TRANSIT -> ICON_SAVINGS
        newType == AccountType.CREDIT_CARD && currentIcon == ICON_TRANSIT -> ICON_CREDIT_CARD
        else -> currentIcon
    }

/** Parses a statement/payment day (1..31), or null when invalid. */
fun parseCardDay(text: String): Int? {
    val day = text.trim().toIntOrNull() ?: return null
    return day.takeIf { it in 1..31 }
}

/** Error for the account-name text field that does not depend on other accounts, or null when valid. */
@StringRes
fun accountNameInputError(name: String): Int? {
    val trimmed = name.trim()
    return when {
        trimmed.length > DomainLimits.NAME_MAX_LENGTH -> DomainError.NameTooLong.asStringRes()
        name.isNotEmpty() && trimmed.isEmpty() -> DomainError.NameEmpty.asStringRes()
        else -> null
    }
}
