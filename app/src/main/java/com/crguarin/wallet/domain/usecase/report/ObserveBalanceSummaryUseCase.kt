package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.report.BalanceSummary
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Income, expense, net, savings rate and net change of [period].
 *
 * A single query covers the current and the previous period (they never overlap). [today] is
 * received so the signature matches the other report use cases; future days hold no data, so no
 * clamping is needed.
 */
class ObserveBalanceSummaryUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            accountId: Long?,
            today: LocalDate,
        ): Flow<BalanceSummary> {
            val previous = period.previous()
            val from = minOf(period.start, previous.start)
            val to = maxOf(period.end, previous.end)
            return reportRepository
                .observeDailyIncomeExpense(from, to, accountId)
                .map { rows ->
                    val income = sum(rows, period.start, period.end) { it.income }
                    val expense = sum(rows, period.start, period.end) { it.expense }
                    val net = income - expense
                    val previousIncome = sum(rows, previous.start, previous.end) { it.income }
                    val previousExpense = sum(rows, previous.start, previous.end) { it.expense }
                    val previousNet =
                        if (previousIncome.isZero && previousExpense.isZero) {
                            null
                        } else {
                            previousIncome - previousExpense
                        }
                    BalanceSummary(
                        income = income,
                        expense = expense,
                        net = net,
                        savingsRatePerMille = ReportMath.savingsRatePerMille(net, income),
                        previousNet = previousNet,
                        netChange = previousNet?.let { net - it },
                    )
                }
        }

        private fun sum(
            rows: List<DailyIncomeExpense>,
            from: LocalDate,
            to: LocalDate,
            pick: (DailyIncomeExpense) -> Money,
        ): Money =
            Money(
                rows
                    .filter { it.date >= from && it.date <= to }
                    .sumOf { pick(it).minorUnits },
            )
    }
