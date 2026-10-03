package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/**
 * Income and expense totals for each of [months], in the same order and with zero-filled months
 * without movements. Runs a single query covering the whole range.
 */
class ObserveMonthlyTotalsUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            months: List<YearMonth>,
            accountId: Long?,
        ): Flow<List<MonthTotals>> {
            if (months.isEmpty()) return flowOf(emptyList())
            val from = months.first().atDay(1)
            val to = months.last().atEndOfMonth()
            return reportRepository
                .observeDailyIncomeExpense(from, to, accountId)
                .map { rows -> ReportMath.aggregateByMonth(rows, months) }
        }
    }
