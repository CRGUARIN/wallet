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
 * Y axis range of a chart that can show negative values: the rounded minimum and maximum (minor
 * units) and the tick values, bottom to top.
 */
data class AxisRange(
    val minMinorUnits: Long,
    val maxMinorUnits: Long,
    val ticksMinorUnits: List<Long>,
) {
    /** Position of [minorUnits] in 0..1 (0 = minMinorUnits), clamped. */
    fun fractionOf(minorUnits: Long): Float {
        if (maxMinorUnits == minMinorUnits) return 0f
        return ((minorUnits - minMinorUnits).toFloat() / (maxMinorUnits - minMinorUnits)).coerceIn(0f, 1f)
    }
}

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

    /**
     * Computes the [AxisRange] covering [minMinorUnits]..[maxMinorUnits], with 0 always inside the
     * range and as a tick.
     *
     * Without negatives (`min >= 0`) the result is exactly [axisScale] as a range, so charts
     * without negative values do not change. With negatives the span `max - min` is split into 4
     * "nice" segments (`step = niceAxisMax(ceil(span / 4))`) and both ends are rounded outwards to
     * a multiple of the step (`floor` below, `ceil` above); when that yields more than 6 ticks the
     * step grows to the next "nice" value until at most 6 remain. Every tick is a multiple of the
     * step, so 0 is always one of them.
     *
     * Documented case: `(-3_200_000, 0)` (pesos, i.e. `(-320_000_000, 0)` minor units) gives
     * `-4..0 M` with a 1 M step, not `-3,5..0 M`: `niceAxisMax(ceil(320 M / 4))` is 100 M minor
     * units and `floor(-320 M / 100 M)` is `-4`. A `-3,5..0` range would need a 500 k step, which
     * is below the "nice" minimum for that span.
     *
     * Overflow safety: the span saturates at [Long.MAX_VALUE] and both rounded ends saturate at
     * [Long.MIN_VALUE]/[Long.MAX_VALUE], so inputs near the `Long` extremes never crash and the
     * range still covers the data.
     */
    fun axisRange(
        minMinorUnits: Long,
        maxMinorUnits: Long,
    ): AxisRange {
        if (minMinorUnits >= 0L) {
            val scale = axisScale(maxMinorUnits)
            return AxisRange(0L, scale.maxMinorUnits, scale.ticksMinorUnits)
        }
        val lo = minOf(minMinorUnits, 0L)
        val hi = maxOf(maxMinorUnits, 0L)
        val span =
            try {
                Math.subtractExact(hi, lo)
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        var step = niceAxisMax(ceilDiv4(span))
        var lowerQuotient = Math.floorDiv(lo, step)
        var upperQuotient = ceilDiv(hi, step)
        while (quotientSpan(upperQuotient, lowerQuotient) > 5L) {
            val next = nextNiceStep(step)
            if (next <= step) break
            step = next
            lowerQuotient = Math.floorDiv(lo, step)
            upperQuotient = ceilDiv(hi, step)
        }
        val ticks = (lowerQuotient..upperQuotient).map { quotient -> saturatingMultiply(quotient, step) }
        return AxisRange(ticks.first(), ticks.last(), ticks)
    }

    /** `ceil(span / 4)` for `span >= 0`, matching the quarter [axisScale] uses. */
    private fun ceilDiv4(span: Long): Long = span / 4L + if (span % 4L == 0L) 0L else 1L

    /**
     * `ceil(a / b)` for `b > 0` without overflow. `Math.ceilDiv` is avoided on purpose: it needs
     * API 34 (or core library desugaring) while this project targets API 26 without desugaring.
     */
    private fun ceilDiv(
        a: Long,
        b: Long,
    ): Long = if (a >= 0L) a / b + if (a % b == 0L) 0L else 1L else a / b

    /** `upper - lower` saturated at [Long.MAX_VALUE] (both are tick quotients, `upper >= lower`). */
    private fun quotientSpan(
        upper: Long,
        lower: Long,
    ): Long =
        try {
            Math.subtractExact(upper, lower)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }

    /** Next `1`, `2` or `5` times a power of ten step above [step]. */
    private fun nextNiceStep(step: Long): Long = if (step >= Long.MAX_VALUE) Long.MAX_VALUE else niceAxisMax(step + 1L)

    /** `a * b` saturated at [Long.MIN_VALUE]/[Long.MAX_VALUE] (used with `b > 0`). */
    private fun saturatingMultiply(
        a: Long,
        b: Long,
    ): Long =
        try {
            Math.multiplyExact(a, b)
        } catch (_: ArithmeticException) {
            if (a < 0L) Long.MIN_VALUE else Long.MAX_VALUE
        }
}
