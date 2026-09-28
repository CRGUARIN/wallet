package com.crguarin.wallet.core.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {
    private val esLocale = Locale.forLanguageTag("es-CO")
    private val enLocale = Locale.forLanguageTag("en-US")

    private fun String.normalizeSpaces(): String =
        replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .trim()

    @Test
    fun `format COP in Spanish without cents displays zero decimals and dot for thousands`() {
        val money = Money(1_500_000L) // $ 15.000
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 15.000")
    }

    @Test
    fun `format COP in Spanish with cents displays two decimals and comma for decimal separator`() {
        val money = Money(1_500_050L) // $ 15.000,50
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 15.000,50")
    }

    @Test
    fun `format COP in Spanish with single cent displays two decimals`() {
        val money = Money(1L) // $ 0,01
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 0,01")
    }

    @Test
    fun `format COP in Spanish with zero displays zero decimals`() {
        val money = Money.ZERO
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 0")
    }

    @Test
    fun `format COP in Spanish with negative amount without cents`() {
        val money = Money(-1_500_000L)
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("-$ 15.000")
    }

    @Test
    fun `format COP in Spanish with negative amount with cents`() {
        val money = Money(-1_500_050L)
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("-$ 15.000,50")
    }

    @Test
    fun `format COP in Spanish with max limit amount`() {
        val money = Money.MAX // 999_999_999_999_99L -> $ 999.999.999.999,99
        val formatted = MoneyFormatter.format(money, locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 999.999.999.999,99")
    }

    @Test
    fun `format COP in English without cents displays zero decimals and comma for thousands`() {
        val money = Money(1_500_000L)
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$15,000")
    }

    @Test
    fun `format COP in English with cents displays two decimals and dot for decimal separator`() {
        val money = Money(1_500_050L)
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$15,000.50")
    }

    @Test
    fun `format COP in English with single cent displays two decimals`() {
        val money = Money(1L)
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$0.01")
    }

    @Test
    fun `format COP in English with zero displays zero decimals`() {
        val money = Money.ZERO
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$0")
    }

    @Test
    fun `format COP in English with negative amount without cents`() {
        val money = Money(-1_500_000L)
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("-$15,000")
    }

    @Test
    fun `format COP in English with negative amount with cents`() {
        val money = Money(-1_500_050L)
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("-$15,000.50")
    }

    @Test
    fun `format COP in English with max limit amount`() {
        val money = Money.MAX
        val formatted = MoneyFormatter.format(money, locale = enLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$999,999,999,999.99")
    }

    @Test
    fun `extension format delegates to MoneyFormatter`() {
        val money = Money(1_500_000L)
        val formatted = money.format(locale = esLocale).normalizeSpaces()
        assertThat(formatted).isEqualTo("$ 15.000")
    }

    @Test
    fun `format COP with language-only locales uses dollar symbol`() {
        val money = Money(1_500_000L)
        val es = MoneyFormatter.format(money, locale = Locale.forLanguageTag("es")).normalizeSpaces()
        val en = MoneyFormatter.format(money, locale = Locale.forLanguageTag("en")).normalizeSpaces()
        assertThat(es).doesNotContain("COP")
        assertThat(es).contains("$")
        assertThat(es).contains("15.000")
        assertThat(en).isEqualTo("$15,000")
    }
}
