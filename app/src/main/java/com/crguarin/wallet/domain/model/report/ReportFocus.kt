package com.crguarin.wallet.domain.model.report

import com.crguarin.wallet.domain.model.TransactionType

/**
 * What the reports screen shows: expenses, income, or the balance (result) view.
 *
 * EXPENSE and INCOME behave as before; BALANCE shows the net, the savings rate and the balance
 * evolution instead of the category donut and the month pace.
 */
enum class ReportFocus {
    EXPENSE,
    INCOME,
    BALANCE;

    /** Transaction type shown by this focus; null for BALANCE. */
    val transactionType: TransactionType?
        get() =
            when (this) {
                EXPENSE -> TransactionType.EXPENSE
                INCOME -> TransactionType.INCOME
                BALANCE -> null
            }
}
