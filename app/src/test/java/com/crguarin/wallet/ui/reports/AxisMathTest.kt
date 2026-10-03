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
    fun `axisRange without negatives splits four million pesos like axisScale`() {
        assertThat(AxisMath.axisRange(0L, 400_000_000L)).isEqualTo(
            AxisRange(
                0L,
                400_000_000L,
                listOf(0L, 100_000_000L, 200_000_000L, 300_000_000L, 400_000_000L),
            ),
        )
    }

    @Test
    fun `axisRange from minus one to three million keeps both ends`() {
        assertThat(AxisMath.axisRange(-100_000_000L, 300_000_000L)).isEqualTo(
            AxisRange(
                -100_000_000L,
                300_000_000L,
                listOf(-100_000_000L, 0L, 100_000_000L, 200_000_000L, 300_000_000L),
            ),
        )
    }

    @Test
    fun `axisRange of minus 3_2 million to zero rounds down to minus four million`() {
        // step = nice(ceil(320 M / 4)) = 100 M minor units, floor(-320 M / 100 M) = -4: the range
        // is -4..0 M, never -3,5..0 M, which would need a 500 k step below the nice minimum.
        assertThat(AxisMath.axisRange(-320_000_000L, 0L)).isEqualTo(
            AxisRange(
                -400_000_000L,
                0L,
                listOf(-400_000_000L, -300_000_000L, -200_000_000L, -100_000_000L, 0L),
            ),
        )
    }

    @Test
    fun `axisRange of minus and plus 150 thousand pesos rounds to 200 thousand`() {
        assertThat(AxisMath.axisRange(-15_000_000L, 15_000_000L)).isEqualTo(
            AxisRange(
                -20_000_000L,
                20_000_000L,
                listOf(-20_000_000L, -10_000_000L, 0L, 10_000_000L, 20_000_000L),
            ),
        )
    }

    @Test
    fun `axisRange of zero to zero is a single zero tick`() {
        assertThat(AxisMath.axisRange(0L, 0L)).isEqualTo(AxisRange(0L, 0L, listOf(0L)))
    }

    @Test
    fun `axisRange with min above zero matches axisScale exactly`() {
        val maxima = listOf(0L, 1L, 9L, 100L, 15_000_000L, 400_000_000L, Long.MAX_VALUE / 4L)
        for (max in maxima) {
            val scale = AxisMath.axisScale(max)
            val expected = AxisRange(0L, scale.maxMinorUnits, scale.ticksMinorUnits)
            assertThat(AxisMath.axisRange(0L, max)).isEqualTo(expected)
            // A positive minimum is dropped: without negatives the range always starts at 0.
            assertThat(AxisMath.axisRange(max / 2L, max)).isEqualTo(expected)
        }
    }

    @Test
    fun `axisRange near an eighth of Long MAX_VALUE covers the data with few ticks`() {
        val eighth = Long.MAX_VALUE / 8L
        for ((min, max) in listOf(-eighth to eighth, -eighth to 0L, -1L to eighth)) {
            val range = AxisMath.axisRange(min, max)
            assertThat(range.minMinorUnits).isAtMost(min)
            assertThat(range.maxMinorUnits).isAtLeast(max)
            assertThat(range.ticksMinorUnits).contains(0L)
            assertThat(range.ticksMinorUnits).isInOrder()
            assertThat(range.ticksMinorUnits.size).isAtMost(6)
            assertThat(range.ticksMinorUnits.first()).isEqualTo(range.minMinorUnits)
            assertThat(range.ticksMinorUnits.last()).isEqualTo(range.maxMinorUnits)
        }
    }

    @Test
    fun `axisRange of the full Long span saturates instead of overflowing`() {
        val range = AxisMath.axisRange(Long.MIN_VALUE, Long.MAX_VALUE)
        assertThat(range.minMinorUnits).isEqualTo(Long.MIN_VALUE)
        assertThat(range.maxMinorUnits).isEqualTo(Long.MAX_VALUE)
        assertThat(range.ticksMinorUnits).contains(0L)
        assertThat(range.ticksMinorUnits).isInOrder()
        assertThat(range.ticksMinorUnits.size).isAtMost(6)
    }

    @Test
    fun `axisRange always covers the data with zero as a tick`() {
        val samples =
            listOf(
                -1L to 0L,
                -1L to 1L,
                -7L to 3L,
                -123_456_789L to 987_654L,
                -320_000_000L to -100_000_000L,
                -9L to 9L,
                -400_000_000L to 400_000_000L,
            )
        for ((min, max) in samples) {
            val range = AxisMath.axisRange(min, max)
            assertThat(range.minMinorUnits).isAtMost(minOf(min, 0L))
            assertThat(range.maxMinorUnits).isAtLeast(maxOf(max, 0L))
            assertThat(range.ticksMinorUnits).contains(0L)
            assertThat(range.ticksMinorUnits).isInOrder()
            assertThat(range.ticksMinorUnits.size).isInRange(1, 6)
            assertThat(range.ticksMinorUnits.first()).isEqualTo(range.minMinorUnits)
            assertThat(range.ticksMinorUnits.last()).isEqualTo(range.maxMinorUnits)
        }
    }

    @Test
    fun `fractionOf maps the ends to zero and one and clamps outside`() {
        val range = AxisRange(-100_000_000L, 300_000_000L, listOf(-100_000_000L, 0L, 100_000_000L, 200_000_000L, 300_000_000L))
        assertThat(range.fractionOf(-100_000_000L)).isEqualTo(0f)
        assertThat(range.fractionOf(300_000_000L)).isEqualTo(1f)
        assertThat(range.fractionOf(0L)).isWithin(1e-6f).of(0.25f)
        assertThat(range.fractionOf(-999_999_999L)).isEqualTo(0f)
        assertThat(range.fractionOf(999_999_999L)).isEqualTo(1f)
    }

    @Test
    fun `fractionOf of a degenerate range is zero`() {
        assertThat(AxisRange(0L, 0L, listOf(0L)).fractionOf(5L)).isEqualTo(0f)
        assertThat(AxisRange(0L, 0L, listOf(0L)).fractionOf(0L)).isEqualTo(0f)
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
