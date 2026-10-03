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
     * Computes the [AxisScale] for a data maximum (minor units). The axis always has 4 segments
     * and every tick is a "nice" `1`, `2` or `5` times a power of ten number, so labels never fall
     * on values like 1,25 M: `step = niceAxisMax(ceil(max / 4))`, `max = 4 * step` and
     * ticks `0, step, 2 * step, 3 * step, 4 * step`.
     *
     * Returns `AxisScale(0, [0])` for zero or negative input. Products that would overflow
     * saturate at [Long.MAX_VALUE].
     */
    fun axisScale(maxMinorUnits: Long): AxisScale {
        if (maxMinorUnits <= 0L) return AxisScale(0L, listOf(0L))
        val quarter = maxMinorUnits / 4L + if (maxMinorUnits % 4L == 0L) 0L else 1L
        val step = niceAxisMax(quarter)
        val ticks =
            (0L..4L).map { index ->
                try {
                    Math.multiplyExact(step, index)
                } catch (_: ArithmeticException) {
                    Long.MAX_VALUE
                }
            }
        return AxisScale(ticks.last(), ticks)
    }

    /**
     * Rounds [maxMinorUnits] up to the next "nice" value of the form `1`, `2` or `5` times a power
     * of ten, so the mapped y labels of a chart are easy to read and the axis can be split into 4
     * even segments.
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
