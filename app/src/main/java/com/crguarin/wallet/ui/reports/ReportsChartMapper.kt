package com.crguarin.wallet.ui.reports

import androidx.compose.ui.graphics.Color
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalancePoint
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.ui.reports.charts.DonutSlice
import java.time.YearMonth

/**
 * Converts the `Money` values of the domain report models into the `Float` fractions the charts
 * need. This is the only place where report amounts become floating point, and only to draw: the
 * percentages shown to the user are computed with integer math in the domain.
 */
object ReportsChartMapper {
    /** Donut slices of [breakdown], with the colors resolved from the palette or the theme. */
    fun donutSlices(
        breakdown: CategoryBreakdown,
        othersColor: Color,
        uncategorizedColor: Color,
    ): List<DonutSlice<SliceKey>> {
        val total = breakdown.total.minorUnits
        return breakdown.slices.map { slice ->
            val fraction = if (total > 0L) slice.amount.minorUnits.toFloat() / total.toFloat() else 0f
            DonutSlice(
                key = slice.key,
                fraction = fraction,
                color = sliceColor(slice, othersColor, uncategorizedColor),
            )
        }
    }

    /** Draw color of one slice: its category color, the "Others" color or the uncategorized one. */
    fun sliceColor(
        slice: CategorySlice,
        othersColor: Color,
        uncategorizedColor: Color,
    ): Color =
        when (val key = slice.key) {
            SliceKey.Others -> othersColor
            SliceKey.Uncategorized -> uncategorizedColor
            is SliceKey.Category, is SliceKey.ParentDirect -> slice.color?.let { Color(it) } ?: othersColor
        }

    /** Fractions `0..1` of each bar: one `[income, expense]` pair per month. */
    fun barFractions(
        monthlyTotals: List<MonthTotals>,
        axisMaxMinorUnits: Long,
    ): List<List<Float>> =
        monthlyTotals.map { month ->
            listOf(
                fraction(month.income.minorUnits, axisMaxMinorUnits),
                fraction(month.expense.minorUnits, axisMaxMinorUnits),
            )
        }

    /**
     * Fraction of the dashed average-expense line: the average expense of the months up to
     * [lastRelevantMonth] (future months are not counted). Null when there is nothing to average.
     */
    fun averageExpenseFraction(
        monthlyTotals: List<MonthTotals>,
        axisMaxMinorUnits: Long,
        lastRelevantMonth: YearMonth,
    ): Float? {
        if (axisMaxMinorUnits <= 0L) return null
        val relevant = monthlyTotals.filter { it.yearMonth <= lastRelevantMonth }
        if (relevant.isEmpty()) return null
        val average = relevant.sumOf { it.expense.minorUnits } / relevant.size
        return fraction(average, axisMaxMinorUnits)
    }

    /**
     * Normalizes [SpendingPace.current] and [SpendingPace.previous] over [axisMaxMinorUnits] (the
     * caller passes `AxisMath.axisScale(...).maxMinorUnits` of the maximum of both series, so the y labels line up
     * with the drawn lines). Returns `current to previous`.
     */
    fun cumulativeFractions(
        pace: SpendingPace,
        axisMaxMinorUnits: Long,
    ): Pair<List<Float>, List<Float>> {
        val current = pace.current.map { fraction(it.minorUnits, axisMaxMinorUnits) }
        val previous = pace.previous.map { fraction(it.minorUnits, axisMaxMinorUnits) }
        return current to previous
    }

    /**
     * Axis range of the evolution bars, covering the income, the expense and the net
     * (`income - expense`) of every month. Without deficits it matches [AxisMath.axisScale] as a
     * range.
     */
    fun evolutionAxisRange(monthlyTotals: List<MonthTotals>): AxisRange {
        var lo = 0L
        var hi = 0L
        for (month in monthlyTotals) {
            val income = month.income.minorUnits
            val expense = month.expense.minorUnits
            val net = income - expense
            lo = minOf(lo, net)
            hi = maxOf(hi, income, expense, net)
        }
        return AxisMath.axisRange(lo, hi)
    }

    /** Bar positions inside [range]: one `[income, expense]` pair per month, `0..1`. */
    fun barPositions(
        monthlyTotals: List<MonthTotals>,
        range: AxisRange,
    ): List<List<Float>> =
        monthlyTotals.map { month ->
            listOf(
                range.fractionOf(month.income.minorUnits),
                range.fractionOf(month.expense.minorUnits),
            )
        }

    /**
     * Net-line positions inside [range], one per month. Months after [lastRelevantMonth] (future
     * months) map to null, so the line breaks there; a null [lastRelevantMonth] maps every month
     * to null.
     */
    fun netLinePositions(
        monthlyTotals: List<MonthTotals>,
        range: AxisRange,
        lastRelevantMonth: YearMonth?,
    ): List<Float?> =
        monthlyTotals.map { month ->
            if (lastRelevantMonth != null && month.yearMonth <= lastRelevantMonth) {
                range.fractionOf(month.income.minorUnits - month.expense.minorUnits)
            } else {
                null
            }
        }

    /**
     * Position of the dashed average-expense line inside [range]: the average expense of the
     * months up to [lastRelevantMonth] (future months are not counted). Null when there is
     * nothing to average or the range is degenerate.
     */
    fun averageExpensePosition(
        monthlyTotals: List<MonthTotals>,
        range: AxisRange,
        lastRelevantMonth: YearMonth,
    ): Float? {
        val relevant = monthlyTotals.filter { it.yearMonth <= lastRelevantMonth }
        if (relevant.isEmpty()) return null
        if (range.maxMinorUnits == range.minMinorUnits) return null
        val average = relevant.sumOf { it.expense.minorUnits } / relevant.size
        return range.fractionOf(average)
    }

    /** Axis range of the balance line, covering the opening and every point of [evolution]. */
    fun balanceAxisRange(evolution: BalanceEvolution): AxisRange {
        var lo = evolution.opening.minorUnits
        var hi = evolution.opening.minorUnits
        for (point in evolution.points) {
            lo = minOf(lo, point.balance.minorUnits)
            hi = maxOf(hi, point.balance.minorUnits)
        }
        return AxisMath.axisRange(lo, hi)
    }

    /** Balance-line positions inside [range], one per point, in order. */
    fun balancePositions(
        points: List<BalancePoint>,
        range: AxisRange,
    ): List<Float> = points.map { range.fractionOf(it.balance.minorUnits) }

    private fun fraction(
        minorUnits: Long,
        maxMinorUnits: Long,
    ): Float = if (maxMinorUnits > 0L) (minorUnits.toFloat() / maxMinorUnits.toFloat()).coerceIn(0f, 1f) else 0f
}
