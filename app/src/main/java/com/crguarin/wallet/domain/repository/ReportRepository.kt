package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Aggregated, read-only queries for the reports screen.
 *
 * Transfers are never returned: [observeCategoryTotals] only accepts INCOME or EXPENSE and
 * [observeDailyIncomeExpense] filters them out in SQL.
 */
interface ReportRepository {
    /**
     * Totals per category (leaf, or root with direct movements) in the inclusive date range.
     * Throws [IllegalArgumentException] when [type] is TRANSFER: transfers are neither income nor
     * expense.
     */
    fun observeCategoryTotals(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
        accountId: Long?,
    ): Flow<List<CategoryTotal>>

    /**
     * Income and expense totals per day in the inclusive date range, ordered by date.
     * Days without movements are not returned; callers fill the gaps in domain code.
     */
    fun observeDailyIncomeExpense(
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): Flow<List<DailyIncomeExpense>>
}
