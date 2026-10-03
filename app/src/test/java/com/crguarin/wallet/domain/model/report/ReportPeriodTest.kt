package com.crguarin.wallet.domain.model.report

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Year
import java.time.YearMonth

class ReportPeriodTest {
    @Test
    fun `month period covers the first to the last day of the month`() {
        val period = ReportPeriod.Month(YearMonth.of(2026, 10))

        assertThat(period.start).isEqualTo(java.time.LocalDate.of(2026, 10, 1))
        assertThat(period.end).isEqualTo(java.time.LocalDate.of(2026, 10, 31))
    }

    @Test
    fun `month period handles february and leap years`() {
        assertThat(ReportPeriod.Month(YearMonth.of(2027, 2)).end)
            .isEqualTo(java.time.LocalDate.of(2027, 2, 28))
        assertThat(ReportPeriod.Month(YearMonth.of(2028, 2)).end)
            .isEqualTo(java.time.LocalDate.of(2028, 2, 29))
    }

    @Test
    fun `month previous and next shift by one month across year limits`() {
        assertThat(ReportPeriod.Month(YearMonth.of(2026, 1)).previous())
            .isEqualTo(ReportPeriod.Month(YearMonth.of(2025, 12)))
        assertThat(ReportPeriod.Month(YearMonth.of(2026, 12)).next())
            .isEqualTo(ReportPeriod.Month(YearMonth.of(2027, 1)))
    }

    @Test
    fun `year period covers january first to december thirty first`() {
        val period = ReportPeriod.Year(Year.of(2026))

        assertThat(period.start).isEqualTo(java.time.LocalDate.of(2026, 1, 1))
        assertThat(period.end).isEqualTo(java.time.LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `year previous and next shift by one year`() {
        val period = ReportPeriod.Year(Year.of(2026))

        assertThat(period.previous()).isEqualTo(ReportPeriod.Year(Year.of(2025)))
        assertThat(period.next()).isEqualTo(ReportPeriod.Year(Year.of(2027)))
    }
}
