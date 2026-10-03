package com.crguarin.wallet.ui.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class CompactMoneyFormatterTest {
    private val esLocale = Locale.forLanguageTag("es-CO")
    private val enLocale = Locale.forLanguageTag("en-US")

    // Templates as `stringResource` returns them, without format arguments.
    private val esThousands = "%1\$s mil"
    private val esMillions = "%1\$s M"
    private val esBillions = "%1\$s M"
    private val enThousands = "%1\$sK"
    private val enMillions = "%1\$sM"
    private val enBillions = "%1\$sB"

    private fun formatEs(minorUnits: Long) =
        CompactMoneyFormatter.formatCompact(
            minorUnits = minorUnits,
            locale = esLocale,
            thousandSuffix = esThousands,
            millionSuffix = esMillions,
            billionSuffix = esBillions,
        )

    private fun formatEn(minorUnits: Long) =
        CompactMoneyFormatter.formatCompact(
            minorUnits = minorUnits,
            locale = enLocale,
            thousandSuffix = enThousands,
            millionSuffix = enMillions,
            billionSuffix = enBillions,
        )

    @Test
    fun `formats plain amounts below one thousand without suffix`() {
        assertThat(formatEs(85_000L)).isEqualTo("$ 850")
        assertThat(formatEn(85_000L)).isEqualTo("$850")
        assertThat(formatEs(0L)).isEqualTo("$ 0")
        assertThat(formatEn(0L)).isEqualTo("$0")
        assertThat(formatEs(99_900L)).isEqualTo("$ 999")
        assertThat(formatEn(99_900L)).isEqualTo("$999")
    }

    @Test
    fun `uses the thousands suffix from one thousand`() {
        assertThat(formatEs(100_000L)).isEqualTo("$ 1 mil")
        assertThat(formatEn(100_000L)).isEqualTo("$1K")
        assertThat(formatEs(1_200_000L)).isEqualTo("$ 12 mil")
        assertThat(formatEn(1_200_000L)).isEqualTo("$12K")
    }

    @Test
    fun `formats millions with at most one decimal`() {
        assertThat(formatEs(120_000_000L)).isEqualTo("$ 1,2 M")
        assertThat(formatEn(120_000_000L)).isEqualTo("$1.2M")
        assertThat(formatEs(1_500_000_000L)).isEqualTo("$ 15 M")
        assertThat(formatEn(1_500_000_000L)).isEqualTo("$15M")
    }

    @Test
    fun `rounds half up to one decimal`() {
        // 123.456 thousand -> 123,5 mil
        assertThat(formatEs(12_345_600L)).isEqualTo("$ 123,5 mil")
        assertThat(formatEn(12_345_600L)).isEqualTo("$123.5K")
        // 2.45 million -> 2,5 M
        assertThat(formatEs(245_000_000L)).isEqualTo("$ 2,5 M")
        assertThat(formatEn(245_000_000L)).isEqualTo("$2.5M")
    }

    @Test
    fun `Spanish shows billions as millions and English uses the billions suffix`() {
        // 1,200,000,000.00 COP
        assertThat(formatEs(120_000_000_000L)).isEqualTo("$ 1.200 M")
        assertThat(formatEn(120_000_000_000L)).isEqualTo("$1.2B")
    }

    @Test
    fun `keeps the currency spacing of the locale`() {
        assertThat(formatEs(1_200_000L)).startsWith("$ ")
        assertThat(formatEn(1_200_000L)).startsWith("$1")
    }
}
