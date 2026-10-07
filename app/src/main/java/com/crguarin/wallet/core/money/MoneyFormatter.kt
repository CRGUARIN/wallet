package com.crguarin.wallet.core.money

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Formatter for [Money] instances based on currency and locale.
 *
 * For COP (default currency), displays 0 decimals by default (e.g. "$ 15.000"),
 * and 2 decimals only when non-zero cents are present (e.g. "$ 15.000,50").
 * COP always uses the "$" symbol, even in locales that would show "COP" (e.g. en-US).
 */
object MoneyFormatter {
    private const val COP_SYMBOL = "$"

    private data class FormatKey(
        val locale: Locale,
        val currencyCode: String,
        val hasFractions: Boolean,
    )

    // Building a currency NumberFormat loads locale data, and lists format many amounts per frame,
    // so each configuration is built once. NumberFormat is not thread-safe: each use is synchronized.
    private val formats = ConcurrentHashMap<FormatKey, NumberFormat>()

    fun format(
        money: Money,
        locale: Locale = Locale.getDefault(),
        currencyCode: String = APP_CURRENCY,
    ): String {
        val hasFractions = (money.minorUnits % 100L) != 0L
        val format =
            formats.getOrPut(FormatKey(locale, currencyCode, hasFractions)) {
                createFormat(locale, currencyCode, hasFractions)
            }
        val decimalAmount = BigDecimal.valueOf(money.minorUnits).movePointLeft(2)
        return synchronized(format) { format.format(decimalAmount) }
    }

    private fun createFormat(
        locale: Locale,
        currencyCode: String,
        hasFractions: Boolean,
    ): NumberFormat {
        val currency = Currency.getInstance(currencyCode)
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            if (currencyCode == APP_CURRENCY && this is DecimalFormat) {
                decimalFormatSymbols =
                    decimalFormatSymbols.apply { currencySymbol = COP_SYMBOL }
            }
            if (currencyCode == APP_CURRENCY) {
                if (hasFractions) {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                } else {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 0
                }
            } else {
                val digits = currency.defaultFractionDigits.coerceAtLeast(0)
                if (hasFractions || digits > 0) {
                    minimumFractionDigits = digits
                    maximumFractionDigits = digits
                } else {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 0
                }
            }
        }
    }
}

fun Money.format(
    locale: Locale = Locale.getDefault(),
    currencyCode: String = APP_CURRENCY,
): String = MoneyFormatter.format(this, locale, currencyCode)

/**
 * Plain editable text for an amount field: empty for zero, no decimals for whole amounts and a
 * two-digit fraction otherwise, using the decimal separator of [locale]. Inverse of [MoneyParser].
 */
fun formatForInput(
    money: Money,
    locale: Locale = Locale.getDefault(),
): String =
    when {
        money.minorUnits == 0L -> ""
        money.minorUnits % 100L == 0L -> (money.minorUnits / 100L).toString()
        else -> {
            val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
            String.format(Locale.ROOT, "%d%c%02d", money.minorUnits / 100L, separator, money.minorUnits % 100L)
        }
    }
