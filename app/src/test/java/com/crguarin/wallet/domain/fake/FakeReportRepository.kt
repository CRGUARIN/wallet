package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory [ReportRepository] for domain and ViewModel tests.
 *
 * Category totals carry neither a transaction type, a date nor an account in the domain model, so
 * they are stored per type (see [setInitialData]) and returned as-is: the requested range and
 * account are ignored. Daily rows are filtered by the requested inclusive range, which is enough to
 * test the monthly aggregation, the cumulative sums and the ranges asked by the use cases.
 */
class FakeReportRepository : ReportRepository {
    private val dailyIncomeExpense = MutableStateFlow<List<DailyIncomeExpense>>(emptyList())
    private val categoryTotalsByType = MutableStateFlow<Map<TransactionType, List<CategoryTotal>>>(emptyMap())

    /** Inclusive date ranges received by [observeDailyIncomeExpense], in call order. */
    val dailyRangeRequests = mutableListOf<Pair<LocalDate, LocalDate>>()

    /** Account ids received by [observeDailyIncomeExpense], in call order. */
    val dailyAccountRequests = mutableListOf<Long?>()

    /** Inclusive date ranges received by [observeCategoryTotals], in call order. */
    val categoryRangeRequests = mutableListOf<Pair<LocalDate, LocalDate>>()

    /** Transaction types received by [observeCategoryTotals], in call order. */
    val categoryTypeRequests = mutableListOf<TransactionType>()

    /** Account ids received by [observeCategoryTotals], in call order. */
    val categoryAccountRequests = mutableListOf<Long?>()

    /** Replaces the whole in-memory dataset; both arguments default to empty. */
    fun setInitialData(
        categoryTotalsByType: Map<TransactionType, List<CategoryTotal>> = emptyMap(),
        dailyIncomeExpense: List<DailyIncomeExpense> = emptyList(),
    ) {
        this.categoryTotalsByType.value = categoryTotalsByType
        this.dailyIncomeExpense.value = dailyIncomeExpense.sortedBy { it.date }
    }

    /** Convenience for tests that use the same category rows for INCOME and EXPENSE. */
    fun setInitialData(
        categoryTotals: List<CategoryTotal>,
        dailyIncomeExpense: List<DailyIncomeExpense> = emptyList(),
    ) {
        setInitialData(
            categoryTotalsByType =
                mapOf(
                    TransactionType.INCOME to categoryTotals,
                    TransactionType.EXPENSE to categoryTotals,
                ),
            dailyIncomeExpense = dailyIncomeExpense,
        )
    }

    override fun observeCategoryTotals(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
        accountId: Long?,
    ): Flow<List<CategoryTotal>> {
        require(type != TransactionType.TRANSFER) { "Transfers have no category totals" }
        categoryRangeRequests += from to to
        categoryTypeRequests += type
        categoryAccountRequests += accountId
        return categoryTotalsByType.map { totals -> totals[type].orEmpty() }
    }

    override fun observeDailyIncomeExpense(
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): Flow<List<DailyIncomeExpense>> {
        dailyRangeRequests += from to to
        dailyAccountRequests += accountId
        return dailyIncomeExpense.map { rows ->
            rows.filter { it.date >= from && it.date <= to }
        }
    }
}
