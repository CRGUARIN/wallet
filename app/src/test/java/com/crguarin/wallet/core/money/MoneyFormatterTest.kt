package com.crguarin.wallet.core.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale
import java.util.concurrent.Executors

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

    @Test
    fun `formatForInput is empty for zero and has no decimals for whole amounts`() {
        assertThat(formatForInput(Money.ZERO, esLocale)).isEmpty()
        assertThat(formatForInput(Money(1_500_000L), esLocale)).isEqualTo("15000")
        assertThat(formatForInput(Money(1_500_000L), enLocale)).isEqualTo("15000")
    }

    @Test
    fun `formatForInput uses the locale decimal separator with two fraction digits`() {
        assertThat(formatForInput(Money(150_050L), esLocale)).isEqualTo("1500,50")
        assertThat(formatForInput(Money(150_050L), enLocale)).isEqualTo("1500.50")
        assertThat(formatForInput(Money(105L), enLocale)).isEqualTo("1.05")
    }

    @Test
    fun `formatForInput round-trips through MoneyParser`() {
        val amounts = listOf(1L, 5L, 99L, 100L, 105L, 150_050L, 1_500_000L, 123_456_789L)
        for (locale in listOf(esLocale, enLocale)) {
            for (minor in amounts) {
                val money = Money(minor)
                assertThat(MoneyParser.parse(formatForInput(money, locale), locale)).isEqualTo(money)
            }
        }
    }

    @Test
    fun `cached formats keep whole and fractional amounts apart`() {
        val locale = Locale.forLanguageTag("es-CO")
        val whole = MoneyFormatter.format(Money(1_500_000L), locale)
        val fractional = MoneyFormatter.format(Money(1_500_050L), locale)

        assertThat(MoneyFormatter.format(Money(1_500_000L), locale)).isEqualTo(whole)
        assertThat(MoneyFormatter.format(Money(1_500_050L), locale)).isEqualTo(fractional)
        assertThat(whole).isNotEqualTo(fractional)
    }

    @Test
    fun `formatting from several threads gives the same result as one thread`() {
        val locale = Locale.forLanguageTag("es-CO")
        val amounts = (1L..200L).map { Money(it * 123_457L) }
        val expected = amounts.map { MoneyFormatter.format(it, locale) }
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = (1..8).map { pool.submit<List<String>> { amounts.map { MoneyFormatter.format(it, locale) } } }

            results.forEach { assertThat(it.get()).isEqualTo(expected) }
        } finally {
            pool.shutdown()
        }
    }
}
