package com.crguarin.wallet.ui.reports

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceGranularity
import com.crguarin.wallet.domain.model.report.BalancePoint
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportsChartMapperTest {
    @Test
    fun `bar axis maximum comes from axisScale`() {
        val months =
            listOf(
                MonthTotals(
                    yearMonth = YearMonth.of(2026, 10),
                    income = Money(100_000_000L),
                    expense = Money(400_000_000L),
                ),
            )
        // 4.000.000 $ no longer rounds up to 5.000.000: the axis ends at 4 M with 4 segments.
        assertThat(evolutionAxisScale(months).maxMinorUnits).isEqualTo(400_000_000L)
        assertThat(evolutionAxisScale(emptyList()).maxMinorUnits).isEqualTo(0L)
    }

    private fun month(
        month: Int,
        income: Long,
        expense: Long,
    ) = MonthTotals(YearMonth.of(2026, month), Money(income), Money(expense))

    @Test
    fun `evolutionAxisRange without deficits matches axisScale`() {
        val months = listOf(month(9, 400_000_000L, 100_000_000L), month(10, 200_000_000L, 100_000_000L))

        val range = ReportsChartMapper.evolutionAxisRange(months)
        val scale = AxisMath.axisScale(400_000_000L)

        assertThat(range.minMinorUnits).isEqualTo(0L)
        assertThat(range.maxMinorUnits).isEqualTo(scale.maxMinorUnits)
        assertThat(range.ticksMinorUnits).isEqualTo(scale.ticksMinorUnits)
    }

    @Test
    fun `evolutionAxisRange covers the most negative net`() {
        val months = listOf(month(9, 100_000_000L, 400_000_000L), month(10, 200_000_000L, 100_000_000L))

        val range = ReportsChartMapper.evolutionAxisRange(months)

        assertThat(range.minMinorUnits).isAtMost(-300_000_000L)
        assertThat(range.maxMinorUnits).isAtLeast(400_000_000L)
        assertThat(range.ticksMinorUnits).contains(0L)
    }

    @Test
    fun `evolutionAxisRange of no months is degenerate`() {
        assertThat(ReportsChartMapper.evolutionAxisRange(emptyList()))
            .isEqualTo(AxisRange(0L, 0L, listOf(0L)))
    }

    @Test
    fun `barPositions maps income and expense through the range`() {
        val months = listOf(month(9, 100_000_000L, 400_000_000L))
        val range = AxisMath.axisRange(-300_000_000L, 400_000_000L)

        assertThat(ReportsChartMapper.barPositions(months, range)).containsExactly(
            listOf(range.fractionOf(100_000_000L), range.fractionOf(400_000_000L)),
        )
    }

    @Test
    fun `netLinePositions maps nets and breaks on future months`() {
        val months =
            listOf(
                month(9, 300_000_000L, 100_000_000L),
                month(10, 100_000_000L, 400_000_000L),
                month(11, 0L, 0L),
            )
        val range = ReportsChartMapper.evolutionAxisRange(months)

        val positions = ReportsChartMapper.netLinePositions(months, range, YearMonth.of(2026, 10))

        assertThat(positions).hasSize(3)
        assertThat(positions[0]).isEqualTo(range.fractionOf(200_000_000L))
        assertThat(positions[1]).isEqualTo(range.fractionOf(-300_000_000L))
        assertThat(positions[2]).isNull()
    }

    @Test
    fun `netLinePositions with null last month maps everything to null`() {
        val months = listOf(month(9, 300_000_000L, 100_000_000L))
        val range = ReportsChartMapper.evolutionAxisRange(months)

        assertThat(ReportsChartMapper.netLinePositions(months, range, null)).containsExactly(null)
        assertThat(ReportsChartMapper.netLinePositions(emptyList(), range, null)).isEmpty()
    }

    @Test
    fun `averageExpensePosition averages past months through the range`() {
        val months =
            listOf(
                month(9, 300_000_000L, 100_000_000L),
                month(10, 100_000_000L, 300_000_000L),
                month(11, 0L, 0L),
            )
        val range = ReportsChartMapper.evolutionAxisRange(months)

        val position = ReportsChartMapper.averageExpensePosition(months, range, YearMonth.of(2026, 10))

        assertThat(position).isEqualTo(range.fractionOf(200_000_000L))
    }

    @Test
    fun `averageExpensePosition is null without relevant months or with a degenerate range`() {
        val months = listOf(month(9, 300_000_000L, 100_000_000L))
        val range = ReportsChartMapper.evolutionAxisRange(months)

        assertThat(ReportsChartMapper.averageExpensePosition(months, range, YearMonth.of(2026, 8))).isNull()
        assertThat(ReportsChartMapper.averageExpensePosition(emptyList(), range, YearMonth.of(2026, 9))).isNull()
        val degenerate = AxisRange(0L, 0L, listOf(0L))
        assertThat(ReportsChartMapper.averageExpensePosition(months, degenerate, YearMonth.of(2026, 9))).isNull()
    }

    private fun evolution(
        opening: Long,
        vararg balances: Long,
    ) = BalanceEvolution(
        granularity = BalanceGranularity.DAILY,
        opening = Money(opening),
        points =
            balances.mapIndexed { index, balance ->
                BalancePoint(LocalDate.of(2026, 10, index + 1), Money(balance))
            },
    )

    @Test
    fun `balanceAxisRange covers opening and points`() {
        val range = ReportsChartMapper.balanceAxisRange(evolution(50_000_000L, 100_000_000L, -200_000_000L, 0L))

        assertThat(range.minMinorUnits).isAtMost(-200_000_000L)
        assertThat(range.maxMinorUnits).isAtLeast(100_000_000L)
        assertThat(range.ticksMinorUnits).contains(0L)
    }

    @Test
    fun `balanceAxisRange without points covers the opening`() {
        val range = ReportsChartMapper.balanceAxisRange(evolution(-100_000_000L))

        assertThat(range.minMinorUnits).isAtMost(-100_000_000L)
        assertThat(range.maxMinorUnits).isAtLeast(0L)
    }

    @Test
    fun `balancePositions maps balances in order`() {
        val points =
            listOf(
                BalancePoint(LocalDate.of(2026, 10, 1), Money(100_000_000L)),
                BalancePoint(LocalDate.of(2026, 10, 2), Money(-100_000_000L)),
            )
        val range = AxisMath.axisRange(-100_000_000L, 100_000_000L)

        assertThat(ReportsChartMapper.balancePositions(points, range))
            .containsExactly(
                range.fractionOf(100_000_000L),
                range.fractionOf(-100_000_000L),
            ).inOrder()
    }
}
