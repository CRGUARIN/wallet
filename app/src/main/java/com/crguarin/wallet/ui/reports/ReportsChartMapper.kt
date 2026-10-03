package com.crguarin.wallet.ui.reports

import androidx.compose.ui.graphics.Color
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
    /** Number of segments (and therefore labels) drawn on each y axis. */
    const val Y_AXIS_SEGMENTS = 4

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

    /** Rounded-up y axis maximum (minor units) of the bars of [monthlyTotals]. */
    fun barAxisMaxMinorUnits(monthlyTotals: List<MonthTotals>): Long {
        val max = monthlyTotals.maxOfOrNull { maxOf(it.income.minorUnits, it.expense.minorUnits) } ?: 0L
        return AxisMath.niceAxisMax(max)
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
     * caller passes `AxisMath.niceAxisMax` of the maximum of both series, so the y labels line up
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

    /** Y labels in minor units bottom to top: 0, max/segments, ..., max. */
    fun yAxisMinorUnits(
        axisMaxMinorUnits: Long,
        segments: Int = Y_AXIS_SEGMENTS,
    ): List<Long> {
        if (axisMaxMinorUnits <= 0L || segments <= 0) return listOf(0L)
        return (0..segments).map { axisMaxMinorUnits * it / segments }
    }

    private fun fraction(
        minorUnits: Long,
        maxMinorUnits: Long,
    ): Float = if (maxMinorUnits > 0L) (minorUnits.toFloat() / maxMinorUnits.toFloat()).coerceIn(0f, 1f) else 0f
}
