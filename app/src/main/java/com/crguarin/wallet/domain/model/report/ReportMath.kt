package com.crguarin.wallet.domain.model.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure, integer-only math shared by the report use cases.
 *
 * Monetary amounts are [Money] (Long minor units): floating-point types are never used here, and
 * percentages/per-mille shares are computed with Long arithmetic.
 */
object ReportMath {
    private const val PER_MILLE = 1_000

    /**
     * Splits 1000 per-mille proportionally to [amounts] with the largest-remainder method
     * (Hamilton). The result adds up to exactly 1000, so the UI can never show a total other than
     * 100 %. Returns all zeros when the total is zero. Ties in the remainder are broken by the
     * original position of the amount, which keeps the result stable.
     */
    fun perMilleShares(amounts: List<Money>): List<Int> {
        if (amounts.isEmpty()) return emptyList()
        val total = amounts.fold(0L) { acc, amount -> acc + amount.minorUnits }
        if (total <= 0L) return List(amounts.size) { 0 }

        val exact = amounts.map { it.minorUnits * PER_MILLE }
        val shares = exact.map { (it / total).toInt() }.toMutableList()
        var remainder = PER_MILLE - shares.sum()
        if (remainder > 0) {
            val order =
                amounts.indices.sortedWith(
                    compareByDescending<Int> { exact[it] % total }.thenBy { it },
                )
            for (index in order) {
                if (remainder == 0) break
                shares[index]++
                remainder--
            }
        }
        return shares
    }

    /**
     * Percentage change from [previous] to [current], rounded to the nearest integer with ties
     * [half away from zero][roundedDiv], or null when [previous] is zero. The result is clamped to
     * the [Int] range instead of truncating on overflow.
     *
     * `Money.MAX_MINOR_UNITS * 100` is about 1e17, well below `Long.MAX_VALUE` (about 9.2e18), so
     * the multiplication cannot overflow.
     */
    fun changePercent(
        current: Money,
        previous: Money,
    ): Int? {
        if (previous.isZero) return null
        val numerator = (current.minorUnits - previous.minorUnits) * 100L
        return roundedDiv(numerator, previous.minorUnits)
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            .toInt()
    }

    /**
     * Savings rate in per-mille: `net * 1000 / income`, rounded to the nearest integer with ties
     * [half away from zero][roundedDiv], or null when [income] is zero or negative. It can be
     * negative (more was spent than earned) and it is clamped to the [Int] range instead of
     * truncating on overflow.
     *
     * `Money.MAX_MINOR_UNITS * 1000` is about 1e17, well below `Long.MAX_VALUE` (about 9.2e18), so
     * the multiplication cannot overflow.
     */
    fun savingsRatePerMille(
        net: Money,
        income: Money,
    ): Int? {
        if (income.minorUnits <= 0L) return null
        val numerator = net.minorUnits * PER_MILLE.toLong()
        return roundedDiv(numerator, income.minorUnits)
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            .toInt()
    }

    /**
     * Closing balance of each date in [dates], in ascending order: [opening] plus every delta in
     * [daily] dated on or before each date. Deltas on days absent from [dates] (for example the
     * intermediate days of a month in monthly granularity) are included in the following points.
     */
    fun balancePoints(
        opening: Money,
        daily: Map<LocalDate, Money>,
        dates: List<LocalDate>,
    ): List<Money> {
        val sortedDeltas = daily.entries.sortedBy { it.key }
        val result = ArrayList<Money>(dates.size)
        var running = opening.minorUnits
        var deltaIndex = 0
        for (date in dates.sorted()) {
            while (deltaIndex < sortedDeltas.size && sortedDeltas[deltaIndex].key <= date) {
                running += sortedDeltas[deltaIndex].value.minorUnits
                deltaIndex++
            }
            result += Money(running)
        }
        return result
    }

    /**
     * Sums [rows] per month, following the order of [months]; months without rows are zero-filled.
     * Rows of months not present in [months] are ignored.
     */
    fun aggregateByMonth(
        rows: List<DailyIncomeExpense>,
        months: List<YearMonth>,
    ): List<MonthTotals> {
        val byMonth = rows.groupBy { YearMonth.from(it.date) }
        return months.map { yearMonth ->
            val monthRows = byMonth[yearMonth].orEmpty()
            MonthTotals(
                yearMonth = yearMonth,
                income = Money(monthRows.sumOf { it.income.minorUnits }),
                expense = Money(monthRows.sumOf { it.expense.minorUnits }),
            )
        }
    }

    /**
     * Cumulative income (or expense) of [yearMonth] from day 1 up to [lastDay] (inclusive).
     *
     * Returns one entry per elapsed day, so the size is [lastDay] clamped to the month length. Rows
     * outside [yearMonth] are ignored. TRANSFER has no cumulative series: it is treated as an
     * expense, which is always zero because the source query excludes transfers.
     */
    fun cumulative(
        rows: List<DailyIncomeExpense>,
        type: TransactionType,
        yearMonth: YearMonth,
        lastDay: Int,
    ): List<Money> {
        val byDay =
            rows
                .filter { YearMonth.from(it.date) == yearMonth }
                .associateBy { it.date.dayOfMonth }
        val elapsedDays = lastDay.coerceIn(0, yearMonth.lengthOfMonth())
        val result = ArrayList<Money>(elapsedDays)
        var running = 0L
        for (day in 1..elapsedDays) {
            byDay[day]?.let { row ->
                running += if (type == TransactionType.INCOME) row.income.minorUnits else row.expense.minorUnits
            }
            result += Money(running)
        }
        return result
    }

    /**
     * Integer division rounded to the nearest unit, half away from zero: the magnitude is
     * `(abs(numerator) + abs(denominator) / 2) / abs(denominator)` and the sign follows the
     * quotient, so +2.5 rounds to 3 and −2.5 rounds to −3.
     */
    private fun roundedDiv(
        numerator: Long,
        denominator: Long,
    ): Long {
        val sign = if ((numerator < 0) != (denominator < 0)) -1L else 1L
        val magnitude = (Math.abs(numerator) + Math.abs(denominator) / 2) / Math.abs(denominator)
        return sign * magnitude
    }
}
