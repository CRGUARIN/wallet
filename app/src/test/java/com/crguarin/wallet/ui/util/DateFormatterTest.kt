package com.crguarin.wallet.ui.util

import com.crguarin.wallet.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class DateFormatterTest {
    private val today = LocalDate.of(2026, 9, 28)

    @Test
    fun `today resolves to the today label`() {
        assertThat(relativeDayLabelRes(date = today, today = today))
            .isEqualTo(R.string.transactions_day_today)
    }

    @Test
    fun `yesterday resolves to the yesterday label`() {
        assertThat(relativeDayLabelRes(date = today.minusDays(1), today = today))
            .isEqualTo(R.string.transactions_day_yesterday)
    }

    @Test
    fun `older dates have no relative label`() {
        assertThat(relativeDayLabelRes(date = today.minusDays(2), today = today)).isNull()
    }

    @Test
    fun `formats month and year in Spanish`() {
        val formatted = YearMonth.of(2026, 9).formatMonthYear(Locale.forLanguageTag("es"))

        assertThat(formatted).isEqualTo("Septiembre 2026")
    }

    @Test
    fun `formats month and year in English`() {
        val formatted = YearMonth.of(2026, 9).formatMonthYear(Locale.forLanguageTag("en"))

        assertThat(formatted).isEqualTo("September 2026")
    }
}
