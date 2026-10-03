package com.crguarin.wallet.ui.reports

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test
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
        assertThat(ReportsChartMapper.barAxisMaxMinorUnits(months)).isEqualTo(400_000_000L)
        assertThat(ReportsChartMapper.barAxisMaxMinorUnits(emptyList())).isEqualTo(0L)
    }
}
