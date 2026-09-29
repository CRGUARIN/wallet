package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.ui.util.asStringRes

private const val ICON_WALLET = "wallet"
private const val ICON_SAVINGS = "savings"

/** Swaps the default icon of the previous type for the default icon of [newType]; custom icons are kept. */
fun iconAfterTypeChange(
    newType: AccountType,
    currentIcon: String,
): String =
    when {
        newType == AccountType.SAVINGS && currentIcon == ICON_WALLET -> ICON_SAVINGS
        newType == AccountType.CASH && currentIcon == ICON_SAVINGS -> ICON_WALLET
        else -> currentIcon
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
