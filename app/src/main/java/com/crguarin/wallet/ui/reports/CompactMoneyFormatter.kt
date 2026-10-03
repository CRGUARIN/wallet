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
 * The caller resolves the localized suffix templates with `stringResource` (the existing
 * `reports_compact_thousands`, `reports_compact_millions` and `reports_compact_billions` strings
 * contain a `%1$s` placeholder) and passes them in, so translations decide where the suffix goes.
 *
 * Rules:
 * - Below 1,000: no suffix (e.g. `$ 850` / `$850`).
 * - From 1,000: thousands suffix (e.g. `$ 12 mil` / `$12K`).
 * - From 1,000,000: millions suffix (e.g. `$ 1,2 M` / `$1.2M`).
 * - From 1,000,000,000: Spanish never uses "B"; it reuses the millions suffix scaling the value
 *   by 1,000 (e.g. `$ 1.200 M`). Other languages use the billions suffix (e.g. `$1.2B`).
 * - At most 1 decimal, rounded with [RoundingMode.HALF_UP], with the decimal separator of the
 *   locale; the "$" symbol and its spacing follow [com.crguarin.wallet.core.money.MoneyFormatter].
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
        return when {
            magnitude < THOUSAND -> formatCurrency(amount, locale)
            magnitude < MILLION -> formatWithSuffix(amount, THOUSAND, thousandSuffix, locale)
            magnitude < BILLION -> formatWithSuffix(amount, MILLION, millionSuffix, locale)
            // Decision (spec 5.4): Spanish shows "mil millones" as millions, never "B".
            locale.language == "es" -> formatWithSuffix(amount, MILLION, millionSuffix, locale)
            else -> formatWithSuffix(amount, BILLION, billionSuffix, locale)
        }
    }

    private fun formatWithSuffix(
        amount: BigDecimal,
        divisor: BigDecimal,
        suffixTemplate: String,
        locale: Locale,
    ): String {
        val rounded = amount.divide(divisor, 1, RoundingMode.HALF_UP)
        return String.format(locale, suffixTemplate, formatCurrency(rounded, locale))
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
