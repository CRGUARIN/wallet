package com.crguarin.wallet.core.money

/**
 * Monetary amount stored as an integer of minor units (cents).
 *
 * COP (Colombian Peso) has 2 ISO fraction digits (`Currency.getInstance("COP").defaultFractionDigits == 2`),
 * so $15.000 is stored as 1_500_000 minor units.
 *
 * Floating-point types (Float, Double) must never be used for monetary values.
 */
@JvmInline
value class Money(
    val minorUnits: Long,
) : Comparable<Money> {
    val isZero: Boolean
        get() = minorUnits == 0L

    val isNegative: Boolean
        get() = minorUnits < 0L

    val isPositive: Boolean
        get() = minorUnits > 0L

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    operator fun unaryMinus(): Money = Money(-minorUnits)

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    companion object {
        /**
         * Upper limit for amounts in minor units (999_999_999_999_99L, roughly 1 trillion COP minus 1 cent)
         * to prevent arithmetic overflow.
         */
        const val MAX_MINOR_UNITS: Long = 999_999_999_999_99L

        val ZERO: Money = Money(0L)
        val MAX: Money = Money(MAX_MINOR_UNITS)

        fun fromMajor(majorUnits: Long): Money = Money(majorUnits * 100L)
    }
}

/**
 * Default currency code for Wallet v1.
 */
const val APP_CURRENCY: String = "COP"
