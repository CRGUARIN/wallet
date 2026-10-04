package com.crguarin.wallet.domain.model.recurring

import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import java.time.LocalDate

/* Shared presentation helpers for recurring payments, used by the observe and
 * notification use cases so they all apply the same rules. */

/** Archived account or category blocks recording; the account takes precedence. */
fun RecurringPayment.blockReason(
    account: Account?,
    category: Category?,
): RecurringBlockReason? =
    when {
        account == null || account.isArchived -> RecurringBlockReason.ACCOUNT_ARCHIVED
        category == null || category.isArchived -> RecurringBlockReason.CATEGORY_ARCHIVED
        else -> null
    }

/**
 * CONFIRM occurrences due up to [today] (oldest-first count). Zero for AUTO
 * payments and when nothing is due yet.
 */
fun RecurringPayment.pendingCount(today: LocalDate): Int {
    if (mode != RecurringMode.CONFIRM) return 0
    if (nextDueDate > today) return 0
    return RecurrenceMath.occurrencesBetween(this, nextDueDate, today).size
}

fun RecurringPayment.toItem(
    account: Account?,
    category: Category?,
    today: LocalDate,
): RecurringItem =
    RecurringItem(
        payment = this,
        accountName = account?.name ?: "",
        accountType = account?.type ?: AccountType.CASH,
        category = category,
        pendingCount = pendingCount(today),
        blockReason = blockReason(account, category),
        monthlyEquivalent = RecurrenceMath.monthlyEquivalent(this),
    )
