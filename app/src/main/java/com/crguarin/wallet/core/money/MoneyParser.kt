package com.crguarin.wallet.core.money

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Parser for text inputs representing monetary amounts.
 *
 * Rules:
 * - Accepts only digits and at most one decimal separator (matching [locale]), with at most 2 decimal digits.
 * - Rejects empty strings, zero, and negative values.
 * - Enforces an upper limit of [Money.MAX_MINOR_UNITS] (999_999_999_999_99 minor units) to prevent overflow.
 */
object MoneyParser {
    /**
     * Parses [text] into [Money], or null if invalid according to monetary input rules.
     */
    fun parse(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): Money? {
        val minorUnits = parseToMinorUnits(text, locale) ?: return null
        return Money(minorUnits)
    }

    /**
     * Parses [text] into raw minor units (Long), or null if invalid.
     */
    fun parseToMinorUnits(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator

        for (ch in trimmed) {
            if (ch !in '0'..'9' && ch != decimalSeparator) {
                return null
            }
        }

        val separatorCount = trimmed.count { it == decimalSeparator }
        if (separatorCount > 1) return null

        val majorUnits: Long
        val fractionUnits: Long

        if (separatorCount == 1) {
            val separatorIndex = trimmed.indexOf(decimalSeparator)
            val integerStr = trimmed.substring(0, separatorIndex)
            val fractionStr = trimmed.substring(separatorIndex + 1)

            if (fractionStr.length > 2) return null
            if (integerStr.isEmpty() && fractionStr.isEmpty()) return null

            majorUnits =
                if (integerStr.isEmpty()) {
                    0L
                } else {
                    integerStr.toLongOrNull() ?: return null
                }

            fractionUnits =
                when (fractionStr.length) {
                    0 -> 0L
                    1 -> (fractionStr[0] - '0') * 10L
                    2 -> (fractionStr[0] - '0') * 10L + (fractionStr[1] - '0')
                    else -> return null
                }
        } else {
            majorUnits = trimmed.toLongOrNull() ?: return null
            fractionUnits = 0L
        }

        val maxMajor = Money.MAX_MINOR_UNITS / 100L
        if (majorUnits > maxMajor) return null

        val minorUnits = majorUnits * 100L + fractionUnits

        if (minorUnits <= 0L || minorUnits > Money.MAX_MINOR_UNITS) {
            return null
        }

        return minorUnits
    }

    /**
     * Parses [text] into [DomainResult], returning [DomainError.AmountInvalid] on failure.
     */
    fun parseResult(
        text: String,
        locale: Locale = Locale.getDefault(),
    ): DomainResult<Money> {
        val money = parse(text, locale)
        return if (money != null) {
            DomainResult.Success(money)
        } else {
            DomainResult.Failure(DomainError.AmountInvalid)
        }
    }
}

fun String.toMoneyOrNull(locale: Locale = Locale.getDefault()): Money? = MoneyParser.parse(this, locale)
