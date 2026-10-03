package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import javax.inject.Inject

/**
 * Total, previous-period total, change percentage and average for [period].
 *
 * The average is per elapsed day in MONTH mode and per elapsed month in YEAR mode; "today" is
 * received as a parameter so the use case stays deterministic.
 */
class ObservePeriodSummaryUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            type: TransactionType,
            accountId: Long?,
            today: LocalDate,
        ): Flow<PeriodSummary> {
            val previous = period.previous()
            // A single query covers the current and the previous period (they never overlap).
            val from = minOf(period.start, previous.start)
            val to = maxOf(period.end, previous.end)
            return reportRepository
                .observeDailyIncomeExpense(from, to, accountId)
                .map { rows ->
                    val total = sumType(rows, type, period.start, period.end)
                    val previousTotal = sumType(rows, type, previous.start, previous.end)
                    PeriodSummary(
                        total = total,
                        previousTotal = previousTotal,
                        changePercent = ReportMath.changePercent(total, previousTotal),
                        average = Money(Math.floorDiv(total.minorUnits, averageDivisor(period, today).toLong())),
                    )
                }
        }

        private fun sumType(
            rows: List<DailyIncomeExpense>,
            type: TransactionType,
            from: LocalDate,
            to: LocalDate,
        ): Money =
            Money(
                rows
                    .filter { it.date >= from && it.date <= to }
                    .sumOf { if (type == TransactionType.INCOME) it.income.minorUnits else it.expense.minorUnits },
            )

        private fun averageDivisor(
            period: ReportPeriod,
            today: LocalDate,
        ): Int =
            when (period) {
                is ReportPeriod.Month ->
                    if (period.yearMonth == YearMonth.from(today)) {
                        today.dayOfMonth
                    } else {
                        period.yearMonth.lengthOfMonth()
                    }
                is ReportPeriod.Year ->
                    if (period.year == Year.from(today)) {
                        today.monthValue
                    } else {
                        12
                    }
            }
    }
