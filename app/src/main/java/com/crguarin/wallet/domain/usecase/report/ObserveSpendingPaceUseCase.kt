package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Day-by-day cumulative spending (or income) of [yearMonth] against the previous month.
 *
 * "today" is received as a parameter: the current month is cut at today, past months are complete.
 */
class ObserveSpendingPaceUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            yearMonth: YearMonth,
            type: TransactionType,
            accountId: Long?,
            today: LocalDate,
        ): Flow<SpendingPace> {
            val previousMonth = yearMonth.minusMonths(1)
            // A single query covers the previous month and the selected month.
            val from = previousMonth.atDay(1)
            val to = yearMonth.atEndOfMonth()
            return reportRepository
                .observeDailyIncomeExpense(from, to, accountId)
                .map { rows ->
                    val daysInMonth = yearMonth.lengthOfMonth()
                    val isCurrentMonth = yearMonth == YearMonth.from(today)
                    val comparisonDay =
                        if (isCurrentMonth) {
                            today.dayOfMonth.coerceAtMost(daysInMonth)
                        } else {
                            daysInMonth
                        }
                    val current = ReportMath.cumulative(rows, type, yearMonth, comparisonDay)
                    val previous = ReportMath.cumulative(rows, type, previousMonth, previousMonth.lengthOfMonth())
                    val currentAtDay = current.last()
                    // The previous month may be shorter: clamp the comparison to its last day.
                    val previousAtDay = previous[comparisonDay.coerceAtMost(previous.size) - 1]
                    SpendingPace(
                        yearMonth = yearMonth,
                        current = current,
                        previous = previous,
                        daysInMonth = daysInMonth,
                        comparisonDay = comparisonDay,
                        currentAtDay = currentAtDay,
                        previousAtDay = previousAtDay,
                        changePercent = ReportMath.changePercent(currentAtDay, previousAtDay),
                    )
                }
        }
    }
