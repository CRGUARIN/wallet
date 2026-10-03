package com.crguarin.wallet.domain.model.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportMathTest {
    @Test
    fun `perMilleShares adds up to one thousand when there are remainders`() {
        val shares = ReportMath.perMilleShares(listOf(Money(1), Money(1), Money(1)))

        assertThat(shares).containsExactly(334, 333, 333).inOrder()
        assertThat(shares.sum()).isEqualTo(1000)
    }

    @Test
    fun `perMilleShares assigns the extra units to the largest remainders`() {
        val shares = ReportMath.perMilleShares(listOf(Money(7), Money(11), Money(13)))

        assertThat(shares).containsExactly(226, 355, 419).inOrder()
        assertThat(shares.sum()).isEqualTo(1000)
    }

    @Test
    fun `perMilleShares returns all zeros when the total is zero`() {
        assertThat(ReportMath.perMilleShares(listOf(Money.ZERO, Money.ZERO, Money.ZERO)))
            .containsExactly(0, 0, 0)
            .inOrder()
        assertThat(ReportMath.perMilleShares(emptyList())).isEmpty()
    }

    @Test
    fun `perMilleShares gives the whole share to a single amount`() {
        assertThat(ReportMath.perMilleShares(listOf(Money(12_345)))).containsExactly(1000)
    }

    @Test
    fun `perMilleShares keeps exact proportions when they divide evenly`() {
        assertThat(ReportMath.perMilleShares(listOf(Money(1), Money(3)))).containsExactly(250, 750).inOrder()
    }

    @Test
    fun `changePercent returns a positive value on an increase`() {
        assertThat(ReportMath.changePercent(Money(150), Money(100))).isEqualTo(50)
    }

    @Test
    fun `changePercent returns a negative value on a decrease`() {
        assertThat(ReportMath.changePercent(Money(75), Money(100))).isEqualTo(-25)
    }

    @Test
    fun `changePercent returns zero when nothing changed`() {
        assertThat(ReportMath.changePercent(Money(100), Money(100))).isEqualTo(0)
    }

    @Test
    fun `changePercent returns null when the previous amount is zero`() {
        assertThat(ReportMath.changePercent(Money(100), Money.ZERO)).isNull()
        assertThat(ReportMath.changePercent(Money.ZERO, Money.ZERO)).isNull()
    }

    @Test
    fun `changePercent rounds ties away from zero`() {
        assertThat(ReportMath.changePercent(Money(205), Money(200))).isEqualTo(3)
        assertThat(ReportMath.changePercent(Money(195), Money(200))).isEqualTo(-3)
    }

    @Test
    fun `changePercent clamps to Int MAX_VALUE on overflow instead of going negative`() {
        assertThat(ReportMath.changePercent(Money(3_000_000_000), Money(100))).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `changePercent rounds to the nearest integer including negatives`() {
        assertThat(ReportMath.changePercent(Money(5), Money(3))).isEqualTo(67)
        assertThat(ReportMath.changePercent(Money(4), Money(3))).isEqualTo(33)
        assertThat(ReportMath.changePercent(Money(1), Money(3))).isEqualTo(-67)
        assertThat(ReportMath.changePercent(Money(2), Money(3))).isEqualTo(-33)
    }

    @Test
    fun `aggregateByMonth zero fills the months without rows`() {
        val rows = listOf(daily(2026, 1, 10, income = 100, expense = 40))

        val result =
            ReportMath.aggregateByMonth(
                rows,
                listOf(YearMonth.of(2026, 1), YearMonth.of(2026, 2)),
            )

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(MonthTotals(YearMonth.of(2026, 1), Money(100), Money(40)))
        assertThat(result[1]).isEqualTo(MonthTotals(YearMonth.of(2026, 2), Money.ZERO, Money.ZERO))
    }

    @Test
    fun `aggregateByMonth crosses the year boundary and respects the requested order`() {
        val rows =
            listOf(
                daily(2026, 12, 31, income = 10, expense = 1),
                daily(2027, 1, 2, income = 20, expense = 2),
            )

        val result =
            ReportMath.aggregateByMonth(
                rows,
                listOf(YearMonth.of(2027, 1), YearMonth.of(2026, 12)),
            )

        assertThat(result.map { it.yearMonth })
            .containsExactly(YearMonth.of(2027, 1), YearMonth.of(2026, 12))
            .inOrder()
        assertThat(result[0]).isEqualTo(MonthTotals(YearMonth.of(2027, 1), Money(20), Money(2)))
        assertThat(result[1]).isEqualTo(MonthTotals(YearMonth.of(2026, 12), Money(10), Money(1)))
    }

    @Test
    fun `aggregateByMonth ignores rows of months that were not requested`() {
        val rows =
            listOf(
                daily(2026, 8, 1, income = 99),
                daily(2026, 9, 1, income = 10),
            )

        val result = ReportMath.aggregateByMonth(rows, listOf(YearMonth.of(2026, 9)))

        assertThat(result).containsExactly(MonthTotals(YearMonth.of(2026, 9), Money(10), Money.ZERO))
    }

    @Test
    fun `cumulative sums the amounts day by day for the given type`() {
        val rows =
            listOf(
                daily(2026, 9, 2, income = 100, expense = 10),
                daily(2026, 9, 4, income = 50, expense = 5),
            )

        val income = ReportMath.cumulative(rows, TransactionType.INCOME, YearMonth.of(2026, 9), 5)
        val expense = ReportMath.cumulative(rows, TransactionType.EXPENSE, YearMonth.of(2026, 9), 4)

        assertThat(income)
            .containsExactly(Money.ZERO, Money(100), Money(100), Money(150), Money(150))
            .inOrder()
        assertThat(expense)
            .containsExactly(Money.ZERO, Money(10), Money(10), Money(15))
            .inOrder()
    }

    @Test
    fun `cumulative ignores other months and clamps lastDay to the month length`() {
        val rows =
            listOf(
                daily(2026, 8, 31, income = 99),
                daily(2026, 9, 1, income = 10),
            )

        val result = ReportMath.cumulative(rows, TransactionType.INCOME, YearMonth.of(2026, 9), 40)

        assertThat(result).hasSize(30)
        assertThat(result.last()).isEqualTo(Money(10))
    }

    @Test
    fun `savingsRatePerMille returns zero when the net is zero`() {
        assertThat(ReportMath.savingsRatePerMille(Money.ZERO, Money(1000))).isEqualTo(0)
    }

    @Test
    fun `savingsRatePerMille returns 1000 when the net equals the income`() {
        assertThat(ReportMath.savingsRatePerMille(Money(1000), Money(1000))).isEqualTo(1000)
    }

    @Test
    fun `savingsRatePerMille computes positive and negative rates`() {
        assertThat(ReportMath.savingsRatePerMille(Money(235), Money(1000))).isEqualTo(235)
        assertThat(ReportMath.savingsRatePerMille(Money(-120), Money(1000))).isEqualTo(-120)
    }

    @Test
    fun `savingsRatePerMille rounds ties away from zero`() {
        assertThat(ReportMath.savingsRatePerMille(Money(5), Money(400))).isEqualTo(13)
        assertThat(ReportMath.savingsRatePerMille(Money(-5), Money(400))).isEqualTo(-13)
    }

    @Test
    fun `savingsRatePerMille rounds to the nearest integer`() {
        assertThat(ReportMath.savingsRatePerMille(Money(1), Money(3))).isEqualTo(333)
        assertThat(ReportMath.savingsRatePerMille(Money(2), Money(3))).isEqualTo(667)
        assertThat(ReportMath.savingsRatePerMille(Money(-2), Money(3))).isEqualTo(-667)
    }

    @Test
    fun `savingsRatePerMille returns null when the income is zero or negative`() {
        assertThat(ReportMath.savingsRatePerMille(Money(100), Money.ZERO)).isNull()
        assertThat(ReportMath.savingsRatePerMille(Money.ZERO, Money.ZERO)).isNull()
        assertThat(ReportMath.savingsRatePerMille(Money(-100), Money(-50))).isNull()
    }

    @Test
    fun `savingsRatePerMille clamps to the Int range on overflow`() {
        val huge = Money(Long.MAX_VALUE / 2000)
        assertThat(ReportMath.savingsRatePerMille(huge, Money(1))).isEqualTo(Int.MAX_VALUE)
        assertThat(ReportMath.savingsRatePerMille(-huge, Money(1))).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun `balancePoints accumulates the opening and the deltas up to each date`() {
        val opening = Money(1000)
        val daily =
            mapOf(
                LocalDate.of(2026, 9, 1) to Money(100),
                LocalDate.of(2026, 9, 3) to Money(-40),
            )
        val dates =
            listOf(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
            )

        assertThat(ReportMath.balancePoints(opening, daily, dates))
            .containsExactly(Money(1100), Money(1100), Money(1060))
            .inOrder()
    }

    @Test
    fun `balancePoints includes deltas of days absent from the requested dates`() {
        val opening = Money(500)
        val daily =
            mapOf(
                LocalDate.of(2026, 9, 10) to Money(100),
                LocalDate.of(2026, 9, 20) to Money(50),
            )
        val dates = listOf(LocalDate.of(2026, 9, 30))

        assertThat(ReportMath.balancePoints(opening, daily, dates))
            .containsExactly(Money(650))
    }

    @Test
    fun `balancePoints ignores deltas after the last date and keeps the opening when empty`() {
        val opening = Money(500)

        assertThat(
            ReportMath.balancePoints(
                opening,
                mapOf(LocalDate.of(2026, 10, 1) to Money(100)),
                listOf(LocalDate.of(2026, 9, 30)),
            ),
        ).containsExactly(Money(500))
        assertThat(
            ReportMath.balancePoints(
                opening,
                emptyMap(),
                listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)),
            ),
        ).containsExactly(Money(500), Money(500)).inOrder()
        assertThat(ReportMath.balancePoints(opening, emptyMap(), emptyList())).isEmpty()
    }

    private fun daily(
        year: Int,
        month: Int,
        day: Int,
        income: Long = 0L,
        expense: Long = 0L,
    ): DailyIncomeExpense =
        DailyIncomeExpense(
            date = LocalDate.of(year, month, day),
            income = Money(income),
            expense = Money(expense),
        )
}
