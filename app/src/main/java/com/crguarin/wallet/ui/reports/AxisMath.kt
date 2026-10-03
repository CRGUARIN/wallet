package com.crguarin.wallet.ui.reports

/**
 * Y axis scale of a chart: the rounded-up maximum and the tick values (minor units), bottom to
 * top, used both to normalize the drawn series and to format the axis labels.
 */
data class AxisScale(
    val maxMinorUnits: Long,
    val ticksMinorUnits: List<Long>,
)

/**
 * Pure helpers for chart axes.
 *
 * Lives in the UI layer on purpose: the domain's `ReportMath` must not duplicate [niceAxisMax];
 * axis ticks are a presentation concern.
 */
object AxisMath {
    /**
     * Computes the [AxisScale] for a data maximum. Temporary implementation: the previous
     * behavior of `niceAxisMax(max)` plus `ReportsChartMapper.yAxisMinorUnits` of that maximum.
     */
    fun axisScale(maxMinorUnits: Long): AxisScale {
        val max = niceAxisMax(maxMinorUnits)
        return AxisScale(max, ReportsChartMapper.yAxisMinorUnits(max))
    }

    /**
     * Rounds [maxMinorUnits] up to the next "nice" value of the form `1`, `2` or `5` times a power
     * of ten, so the mapped y labels of a chart are easy to read and the axis can be split into 3
     * or 4 even segments.
     *
     * Returns 0 for zero or negative input. Inputs are in minor units (cents), so for values of at
     * least 100 the result is always a whole number of pesos.
     */
    fun niceAxisMax(maxMinorUnits: Long): Long {
        if (maxMinorUnits <= 0L) return 0L
        var magnitude = 1L
        while (magnitude <= maxMinorUnits / 10L) {
            magnitude *= 10L
        }
        return when {
            magnitude >= maxMinorUnits -> magnitude
            2L * magnitude >= maxMinorUnits -> 2L * magnitude
            5L * magnitude >= maxMinorUnits -> 5L * magnitude
            magnitude <= Long.MAX_VALUE / 10L -> 10L * magnitude
            else -> Long.MAX_VALUE
        }
    }
}
