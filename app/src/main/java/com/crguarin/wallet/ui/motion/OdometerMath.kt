package com.crguarin.wallet.ui.motion

/**
 * One animated digit column of the odometer: the digit at this position rolls from [from] to
 * [to] passing through the values in between. `from` is null only when the column is new and
 * callers pass '0' instead (see [OdometerMath.plan]).
 */
data class OdometerSlot(
    val from: Char,
    val to: Char,
)

/**
 * Pure planning of the rolling amount. The animation always works on the already formatted text,
 * never on the money value, and only digits move: "$", signs, dots and spaces stay fixed.
 */
object OdometerMath {
    /**
     * One slot per digit of [to], left to right (so the caller can delay by position). Digits are
     * paired from the right (units with units): a [to] digit with no counterpart in [from] rolls
     * from '0', and extra leading digits of [from] vanish with no animation.
     */
    fun plan(
        from: String,
        to: String,
    ): List<OdometerSlot> {
        val fromDigits = from.filter { it.isDigit() }
        val toDigits = to.filter { it.isDigit() }
        val extra = toDigits.length - fromDigits.length
        return toDigits.mapIndexed { index, digit ->
            val counterpart = fromDigits.getOrNull(index - extra) ?: '0'
            OdometerSlot(from = counterpart, to = digit)
        }
    }

    /** [text] with every digit replaced by '0', same length: rolling "from zero" on entrance. */
    fun zeroed(text: String): String = text.map { if (it.isDigit()) '0' else it }.joinToString("")
}
