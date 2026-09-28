package com.crguarin.wallet.core.money

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formatter for [Money] instances based on currency and locale.
 *
 * For COP (default currency), displays 0 decimals by default (e.g. "$ 15.000"),
 * and 2 decimals only when non-zero cents are present (e.g. "$ 15.000,50").
 */
object MoneyFormatter {
    fun format(
        money: Money,
        locale: Locale = Locale.getDefault(),
        currencyCode: String = APP_CURRENCY,
    ): String {
        val currency = Currency.getInstance(currencyCode)
        val format =
            NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                val hasFractions = (money.minorUnits % 100L) != 0L
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
        val decimalAmount = BigDecimal.valueOf(money.minorUnits).movePointLeft(2)
        return format.format(decimalAmount)
    }
}

fun Money.format(
    locale: Locale = Locale.getDefault(),
    currencyCode: String = APP_CURRENCY,
): String = MoneyFormatter.format(this, locale, currencyCode)
