package com.crguarin.wallet.core.money

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MoneyParserTest {
    private val esLocale = Locale.forLanguageTag("es-CO")
    private val enLocale = Locale.forLanguageTag("en-US")

    @Test
    fun `parse valid integers in Spanish`() {
        assertThat(MoneyParser.parse("15000", esLocale)).isEqualTo(Money(1_500_000L))
        assertThat(MoneyParser.parse("1", esLocale)).isEqualTo(Money(100L))
        assertThat(MoneyParser.parse("100", esLocale)).isEqualTo(Money(10_000L))
    }

    @Test
    fun `parse valid decimals in Spanish with comma`() {
        assertThat(MoneyParser.parse("15000,50", esLocale)).isEqualTo(Money(1_500_050L))
        assertThat(MoneyParser.parse("15000,5", esLocale)).isEqualTo(Money(1_500_050L))
        assertThat(MoneyParser.parse("15000,05", esLocale)).isEqualTo(Money(1_500_005L))
        assertThat(MoneyParser.parse("0,50", esLocale)).isEqualTo(Money(50L))
        assertThat(MoneyParser.parse(",50", esLocale)).isEqualTo(Money(50L))
        assertThat(MoneyParser.parse("0,01", esLocale)).isEqualTo(Money(1L))
        assertThat(MoneyParser.parse("15000,", esLocale)).isEqualTo(Money(1_500_000L))
    }

    @Test
    fun `parse limit value in Spanish`() {
        // 999_999_999_999,99 -> 999_999_999_999_99 minor units
        val parsed = MoneyParser.parse("999999999999,99", esLocale)
        assertThat(parsed).isEqualTo(Money(Money.MAX_MINOR_UNITS))
    }

    @Test
    fun `parse trims surrounding whitespace in Spanish`() {
        assertThat(MoneyParser.parse("  15000,50  ", esLocale)).isEqualTo(Money(1_500_050L))
    }

    @Test
    fun `parse valid integers in English`() {
        assertThat(MoneyParser.parse("15000", enLocale)).isEqualTo(Money(1_500_000L))
        assertThat(MoneyParser.parse("1", enLocale)).isEqualTo(Money(100L))
        assertThat(MoneyParser.parse("100", enLocale)).isEqualTo(Money(10_000L))
    }

    @Test
    fun `parse valid decimals in English with dot`() {
        assertThat(MoneyParser.parse("15000.50", enLocale)).isEqualTo(Money(1_500_050L))
        assertThat(MoneyParser.parse("15000.5", enLocale)).isEqualTo(Money(1_500_050L))
        assertThat(MoneyParser.parse("15000.05", enLocale)).isEqualTo(Money(1_500_005L))
        assertThat(MoneyParser.parse("0.50", enLocale)).isEqualTo(Money(50L))
        assertThat(MoneyParser.parse(".50", enLocale)).isEqualTo(Money(50L))
        assertThat(MoneyParser.parse("0.01", enLocale)).isEqualTo(Money(1L))
        assertThat(MoneyParser.parse("15000.", enLocale)).isEqualTo(Money(1_500_000L))
    }

    @Test
    fun `parse limit value in English`() {
        val parsed = MoneyParser.parse("999999999999.99", enLocale)
        assertThat(parsed).isEqualTo(Money(Money.MAX_MINOR_UNITS))
    }

    @Test
    fun `parse trims surrounding whitespace in English`() {
        assertThat(MoneyParser.parse("  15000.50  ", enLocale)).isEqualTo(Money(1_500_050L))
    }

    @Test
    fun `reject wrong decimal separator for locale`() {
        // Dot in Spanish is rejected (dot is not the decimal separator)
        assertThat(MoneyParser.parse("15000.50", esLocale)).isNull()
        // Comma in English is rejected (comma is not the decimal separator)
        assertThat(MoneyParser.parse("15000,50", enLocale)).isNull()
    }

    @Test
    fun `reject thousands grouping separator`() {
        assertThat(MoneyParser.parse("15.000", esLocale)).isNull()
        assertThat(MoneyParser.parse("15.000,00", esLocale)).isNull()
        assertThat(MoneyParser.parse("15,000", enLocale)).isNull()
        assertThat(MoneyParser.parse("15,000.00", enLocale)).isNull()
    }

    @Test
    fun `reject empty and blank strings`() {
        assertThat(MoneyParser.parse("", esLocale)).isNull()
        assertThat(MoneyParser.parse("   ", esLocale)).isNull()
    }

    @Test
    fun `reject zero values`() {
        assertThat(MoneyParser.parse("0", esLocale)).isNull()
        assertThat(MoneyParser.parse("0,0", esLocale)).isNull()
        assertThat(MoneyParser.parse("0,00", esLocale)).isNull()
        assertThat(MoneyParser.parse(",0", esLocale)).isNull()
        assertThat(MoneyParser.parse(",00", esLocale)).isNull()
        assertThat(MoneyParser.parse("000", esLocale)).isNull()
        assertThat(MoneyParser.parse("0.00", enLocale)).isNull()
        assertThat(MoneyParser.parse(".00", enLocale)).isNull()
    }

    @Test
    fun `reject negative values`() {
        assertThat(MoneyParser.parse("-15000", esLocale)).isNull()
        assertThat(MoneyParser.parse("-15000,50", esLocale)).isNull()
        assertThat(MoneyParser.parse("-0,01", esLocale)).isNull()
        assertThat(MoneyParser.parse("-15000.50", enLocale)).isNull()
    }

    @Test
    fun `reject more than two decimal places`() {
        assertThat(MoneyParser.parse("15000,500", esLocale)).isNull()
        assertThat(MoneyParser.parse("15000,123", esLocale)).isNull()
        assertThat(MoneyParser.parse("0,001", esLocale)).isNull()
        assertThat(MoneyParser.parse("15000.123", enLocale)).isNull()
    }

    @Test
    fun `reject multiple decimal separators`() {
        assertThat(MoneyParser.parse("15,000,00", esLocale)).isNull()
        assertThat(MoneyParser.parse("15.000.00", enLocale)).isNull()
    }

    @Test
    fun `reject input consisting only of decimal separator`() {
        assertThat(MoneyParser.parse(",", esLocale)).isNull()
        assertThat(MoneyParser.parse(".", enLocale)).isNull()
    }

    @Test
    fun `reject non-numeric characters and symbols`() {
        assertThat(MoneyParser.parse("abc", esLocale)).isNull()
        assertThat(MoneyParser.parse("15a", esLocale)).isNull()
        assertThat(MoneyParser.parse("$15000", esLocale)).isNull()
        assertThat(MoneyParser.parse("15000$", esLocale)).isNull()
        assertThat(MoneyParser.parse("+15000", esLocale)).isNull()
        assertThat(MoneyParser.parse("15 000", esLocale)).isNull()
    }

    @Test
    fun `reject overflow values`() {
        // 1 trillion major units (exceeds 999_999_999_999,99)
        assertThat(MoneyParser.parse("1000000000000", esLocale)).isNull()
        assertThat(MoneyParser.parse("1000000000000,00", esLocale)).isNull()
        // Absurdly large string that would overflow Long
        assertThat(MoneyParser.parse("99999999999999999999999999", esLocale)).isNull()
    }

    @Test
    fun `parseToMinorUnits returns exact Long`() {
        assertThat(MoneyParser.parseToMinorUnits("15000,50", esLocale)).isEqualTo(1_500_050L)
        assertThat(MoneyParser.parseToMinorUnits("invalid", esLocale)).isNull()
    }

    @Test
    fun `parseResult returns Success on valid input`() {
        val result = MoneyParser.parseResult("15000,50", esLocale)
        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        assertThat((result as DomainResult.Success).value).isEqualTo(Money(1_500_050L))
    }

    @Test
    fun `parseResult returns Failure with AmountInvalid on invalid input`() {
        val result = MoneyParser.parseResult("invalid", esLocale)
        assertThat(result).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((result as DomainResult.Failure).error).isEqualTo(DomainError.AmountInvalid)
    }

    @Test
    fun `string extension toMoneyOrNull parses correctly`() {
        assertThat("15000,50".toMoneyOrNull(esLocale)).isEqualTo(Money(1_500_050L))
        assertThat("invalid".toMoneyOrNull(esLocale)).isNull()
    }
}
