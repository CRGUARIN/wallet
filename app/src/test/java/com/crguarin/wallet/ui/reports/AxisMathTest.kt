package com.crguarin.wallet.ui.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AxisMathTest {
    @Test
    fun `zero and negative amounts return zero`() {
        assertThat(AxisMath.niceAxisMax(0L)).isEqualTo(0L)
        assertThat(AxisMath.niceAxisMax(-1L)).isEqualTo(0L)
        assertThat(AxisMath.niceAxisMax(-123_456L)).isEqualTo(0L)
    }

    @Test
    fun `rounds up to one two or five times a power of ten`() {
        assertThat(AxisMath.niceAxisMax(1L)).isEqualTo(1L)
        assertThat(AxisMath.niceAxisMax(2L)).isEqualTo(2L)
        assertThat(AxisMath.niceAxisMax(3L)).isEqualTo(5L)
        assertThat(AxisMath.niceAxisMax(5L)).isEqualTo(5L)
        assertThat(AxisMath.niceAxisMax(6L)).isEqualTo(10L)
        assertThat(AxisMath.niceAxisMax(10L)).isEqualTo(10L)
        assertThat(AxisMath.niceAxisMax(11L)).isEqualTo(20L)
        assertThat(AxisMath.niceAxisMax(20L)).isEqualTo(20L)
        assertThat(AxisMath.niceAxisMax(21L)).isEqualTo(50L)
        assertThat(AxisMath.niceAxisMax(50L)).isEqualTo(50L)
        assertThat(AxisMath.niceAxisMax(51L)).isEqualTo(100L)
        assertThat(AxisMath.niceAxisMax(99L)).isEqualTo(100L)
        assertThat(AxisMath.niceAxisMax(100L)).isEqualTo(100L)
        assertThat(AxisMath.niceAxisMax(101L)).isEqualTo(200L)
    }

    @Test
    fun `rounds up real money amounts`() {
        // $ 1.234.567,89 -> $ 2.000.000
        assertThat(AxisMath.niceAxisMax(123_456_789L)).isEqualTo(200_000_000L)
        // $ 15.000.000 -> $ 20.000.000
        assertThat(AxisMath.niceAxisMax(1_500_000_000L)).isEqualTo(2_000_000_000L)
        // $ 999.999.999.999,99 -> $ 1.000.000.000.000
        assertThat(AxisMath.niceAxisMax(99_999_999_999_999L)).isEqualTo(100_000_000_000_000L)
    }

    @Test
    fun `result is never below the input`() {
        val samples = listOf(0L, 1L, 7L, 42L, 999L, 1_000L, 5_432_100L, 99_999_999_999_999L)
        for (sample in samples) {
            assertThat(AxisMath.niceAxisMax(sample)).isAtLeast(sample)
        }
    }

    @Test
    fun `axisScale splits four million pesos into four round millions`() {
        assertThat(AxisMath.axisScale(400_000_000L)).isEqualTo(
            AxisScale(
                400_000_000L,
                listOf(0L, 100_000_000L, 200_000_000L, 300_000_000L, 400_000_000L),
            ),
        )
    }

    @Test
    fun `axisScale splits 150 thousand pesos into steps of 50 thousand`() {
        assertThat(AxisMath.axisScale(15_000_000L)).isEqualTo(
            AxisScale(
                20_000_000L,
                listOf(0L, 5_000_000L, 10_000_000L, 15_000_000L, 20_000_000L),
            ),
        )
    }

    @Test
    fun `axisScale of zero or negative is zero with a single tick`() {
        assertThat(AxisMath.axisScale(0L)).isEqualTo(AxisScale(0L, listOf(0L)))
        assertThat(AxisMath.axisScale(-1L)).isEqualTo(AxisScale(0L, listOf(0L)))
    }

    @Test
    fun `axisScale near a quarter of Long MAX_VALUE does not overflow`() {
        val scale = AxisMath.axisScale(Long.MAX_VALUE / 4L)
        assertThat(scale.maxMinorUnits).isAtLeast(Long.MAX_VALUE / 4L)
        assertThat(scale.ticksMinorUnits).hasSize(5)
        assertThat(scale.ticksMinorUnits).isInOrder()
        assertThat(scale.maxMinorUnits).isEqualTo(scale.ticksMinorUnits.last())
    }

    @Test
    fun `result is always of the form one two or five times a power of ten`() {
        val samples = listOf(1L, 3L, 7L, 11L, 21L, 99L, 101L, 1_234_567L, 88_888_888L, 99_999_999_999_999L)
        val pattern = Regex("[125]0*")
        for (sample in samples) {
            val nice = AxisMath.niceAxisMax(sample).toString()
            assertThat(nice.matches(pattern)).isTrue()
        }
    }
}
