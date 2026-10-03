package com.crguarin.wallet.ui.reports

import com.crguarin.wallet.core.money.APP_CURRENCY
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats monetary amounts in minor units (cents) as short, human-friendly labels for chart axes.
 *
 * The caller resolves the localized suffix templates with `stringResource` (the
 * `reports_compact_thousands`, `reports_compact_millions` and `reports_compact_billions` strings
 * contain a `%1$s` placeholder that receives ONLY the formatted number, without any currency
 * symbol) and passes them in, so translations decide where the `$` goes.
 *
 * Rules:
 * - Below 1,000: full currency, no suffix (e.g. `$ 850` / `$850`).
 * - From 1,000: the number alone is formatted with `NumberFormat.getNumberInstance(locale)` (at
 *   most 1 decimal, [RoundingMode.HALF_UP], with grouping) and placed into the suffix template
 *   (e.g. `500 mil $` / `$500K`).
 * - Unit promotion after rounding: the scaled value is rounded to 1 decimal first; if it reaches
 *   1,000 and a larger unit exists, the next unit is used (e.g. 999.950 $ becomes `1 M $`, not
 *   `1.000 mil $`).
 * - From 1,000,000,000: Spanish never uses "B"; it reuses the millions suffix scaling the value
 *   by 1,000 (e.g. `1.200 M $`). Other languages use the billions suffix (e.g. `$1.2B`).
 * - Negative amounts carry the `-` at the very front (e.g. `-500 mil $` / `-$500K`).
 * - The "$" symbol and its spacing follow [com.crguarin.wallet.core.money.MoneyFormatter].
 */
object CompactMoneyFormatter {
    private val THOUSAND = BigDecimal.valueOf(1_000)
    private val MILLION = BigDecimal.valueOf(1_000_000)
    private val BILLION = BigDecimal.valueOf(1_000_000_000)
    private const val COP_SYMBOL = "$"

    fun formatCompact(
        minorUnits: Long,
        locale: Locale = Locale.getDefault(),
        thousandSuffix: String,
        millionSuffix: String,
        billionSuffix: String,
    ): String {
        val amount = BigDecimal.valueOf(minorUnits).movePointLeft(2)
        val magnitude = amount.abs()
        if (magnitude < THOUSAND) return formatCurrency(amount, locale)
        // Decision (spec 5.4): Spanish shows "mil millones" as millions, never "B".
        val units =
            if (locale.language == "es") {
                listOf(THOUSAND to thousandSuffix, MILLION to millionSuffix)
            } else {
                listOf(THOUSAND to thousandSuffix, MILLION to millionSuffix, BILLION to billionSuffix)
            }
        var index =
            when {
                magnitude < MILLION -> 0
                magnitude < BILLION || locale.language == "es" -> 1
                else -> 2
            }
        var scaled = magnitude.divide(units[index].first, 1, RoundingMode.HALF_UP)
        while (scaled >= THOUSAND && index < units.lastIndex) {
            index++
            scaled = magnitude.divide(units[index].first, 1, RoundingMode.HALF_UP)
        }
        val positive = String.format(locale, units[index].second, formatNumber(scaled, locale))
        return if (amount.signum() < 0) "-$positive" else positive
    }

    private fun formatNumber(
        value: BigDecimal,
        locale: Locale,
    ): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.roundingMode = RoundingMode.HALF_UP
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 1
        format.isGroupingUsed = true
        return format
            .format(value)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
    }

    private fun formatCurrency(
        value: BigDecimal,
        locale: Locale,
    ): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = Currency.getInstance(APP_CURRENCY)
        if (format is DecimalFormat) {
            format.decimalFormatSymbols = format.decimalFormatSymbols.apply { currencySymbol = COP_SYMBOL }
        }
        format.roundingMode = RoundingMode.HALF_UP
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 1
        return format
            .format(value)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
    }
}
