package com.crguarin.wallet.core.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {
    @Test
    fun `constructor sets minorUnits correctly`() {
        val money = Money(1_500_000L)
        assertThat(money.minorUnits).isEqualTo(1_500_000L)
    }

    @Test
    fun `fromMajor multiplies by 100`() {
        val money = Money.fromMajor(15_000L)
        assertThat(money.minorUnits).isEqualTo(1_500_000L)
    }

    @Test
    fun `zero constant has zero minorUnits`() {
        assertThat(Money.ZERO.minorUnits).isEqualTo(0L)
        assertThat(Money.ZERO.isZero).isTrue()
        assertThat(Money.ZERO.isNegative).isFalse()
        assertThat(Money.ZERO.isPositive).isFalse()
    }

    @Test
    fun `max constant matches max minor units`() {
        assertThat(Money.MAX.minorUnits).isEqualTo(Money.MAX_MINOR_UNITS)
        assertThat(Money.MAX_MINOR_UNITS).isEqualTo(999_999_999_999_99L)
    }

    @Test
    fun `app currency is COP`() {
        assertThat(APP_CURRENCY).isEqualTo("COP")
    }

    @Test
    fun `isZero returns true only for zero`() {
        assertThat(Money(0L).isZero).isTrue()
        assertThat(Money(1L).isZero).isFalse()
        assertThat(Money(-1L).isZero).isFalse()
    }

    @Test
    fun `isNegative returns true only for negative values`() {
        assertThat(Money(-500L).isNegative).isTrue()
        assertThat(Money(0L).isNegative).isFalse()
        assertThat(Money(500L).isNegative).isFalse()
    }

    @Test
    fun `isPositive returns true only for positive values`() {
        assertThat(Money(500L).isPositive).isTrue()
        assertThat(Money(0L).isPositive).isFalse()
        assertThat(Money(-500L).isPositive).isFalse()
    }

    @Test
    fun `plus adds minor units`() {
        val a = Money(1_000_000L)
        val b = Money(500_000L)
        val result = a + b
        assertThat(result).isEqualTo(Money(1_500_000L))
    }

    @Test
    fun `plus with negative numbers`() {
        val a = Money(1_000_000L)
        val b = Money(-1_500_000L)
        val result = a + b
        assertThat(result).isEqualTo(Money(-500_000L))
    }

    @Test
    fun `minus subtracts minor units`() {
        val a = Money(1_500_000L)
        val b = Money(500_000L)
        val result = a - b
        assertThat(result).isEqualTo(Money(1_000_000L))
    }

    @Test
    fun `minus resulting in negative`() {
        val a = Money(500_000L)
        val b = Money(1_500_000L)
        val result = a - b
        assertThat(result).isEqualTo(Money(-1_000_000L))
        assertThat(result.isNegative).isTrue()
    }

    @Test
    fun `unaryMinus negates minor units`() {
        assertThat(-Money(500L)).isEqualTo(Money(-500L))
        assertThat(-Money(-500L)).isEqualTo(Money(500L))
        assertThat(-Money(0L)).isEqualTo(Money(0L))
    }

    @Test
    fun `compareTo orders amounts properly`() {
        val negative = Money(-100L)
        val zero = Money(0L)
        val small = Money(50L)
        val large = Money(100L)

        assertThat(negative).isLessThan(zero)
        assertThat(zero).isLessThan(small)
        assertThat(small).isLessThan(large)
        assertThat(large).isGreaterThan(small)
        assertThat(large).isEquivalentAccordingToCompareTo(Money(100L))
    }
}
