package com.crguarin.wallet.ui.transactions

import com.crguarin.wallet.ui.components.AmountVisualTransformation
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Pure logic for the custom numeric keypad (§9.1).
 *
 * Operates on the raw amount text already handled by the amount field (for example "28000" or
 * "1500,50"). The raw text never contains grouping separators (they are only visual), so the only
 * non-digit character ever expected is the locale decimal separator.
 *
 * Rules:
 * - No leading zeros: pressing "5" on "0" gives "5"; pressing "0" or "000" on an empty (or
 *   all-zero) integer part does nothing.
 * - At most [MAX_INTEGER_DIGITS] integer digits (the `MoneyParser` / `Money.MAX` cap). A key that
 *   would exceed it does nothing: it neither truncates nor reports an error.
 * - "000" appends as many zeros as fit.
 * - When the text already carries a decimal part (typed or pasted), new digits extend the fraction
 *   up to [MAX_FRACTION_DIGITS]. Extra digits do nothing.
 * - [backspace] removes the last character; when only the decimal separator is left, it removes
 *   that too.
 * - Every result satisfies `AmountVisualTransformation.isValidInput` for the same locale, as long
 *   as the input did too. A [press] on text that is already invalid for [locale] does nothing, so
 *   pasted content is never corrupted; [backspace] still works character by character so the user
 *   can fix it.
 */
object AmountKeypadMath {
    /** Maximum integer digits, matching the `MoneyParser` / `Money.MAX` cap. */
    const val MAX_INTEGER_DIGITS = 12

    /** Maximum fraction digits, matching `AmountVisualTransformation.isValidInput`. */
    const val MAX_FRACTION_DIGITS = 2

    /** Keys of the custom numeric keypad. They carry no text semantics (see §9.1). */
    sealed interface KeypadKey {
        data class Digit(
            val d: Int,
        ) : KeypadKey {
            init {
                require(d in 0..9)
            }
        }

        data object TripleZero : KeypadKey

        data object Backspace : KeypadKey
    }

    /**
     * Applies [key] to the raw [current] amount text for [locale] and returns the new raw text.
     */
    fun press(
        current: String,
        key: KeypadKey,
        locale: Locale = Locale.getDefault(),
    ): String =
        when (key) {
            is KeypadKey.Backspace -> backspace(current)
            is KeypadKey.Digit -> pressDigits(current, key.d.toString(), locale)
            KeypadKey.TripleZero -> pressDigits(current, "000", locale)
        }

    /**
     * Removes the last character of the raw [current] amount text. When only the decimal separator
     * is left (for example "5,5" becomes "5,"), it removes that too. Locale independent: the raw
     * text never holds grouping separators, so a trailing '.' or ',' can only be the decimal
     * separator left orphaned.
     */
    fun backspace(current: String): String {
        if (current.isEmpty()) return current
        var result = current.dropLast(1)
        if (result.isNotEmpty() && (result.last() == '.' || result.last() == ',')) {
            result = result.dropLast(1)
        }
        return result
    }

    private fun pressDigits(
        current: String,
        digits: String,
        locale: Locale,
    ): String {
        if (!AmountVisualTransformation.isValidInput(current, locale)) return current
        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val separatorIndex = current.indexOf(decimalSeparator)
        return if (separatorIndex >= 0) {
            appendFraction(current, separatorIndex, digits)
        } else {
            appendInteger(current, digits)
        }
    }

    private fun appendFraction(
        current: String,
        separatorIndex: Int,
        digits: String,
    ): String {
        val free = MAX_FRACTION_DIGITS - (current.length - separatorIndex - 1)
        if (free <= 0) return current
        return current + digits.take(free)
    }

    private fun appendInteger(
        current: String,
        digits: String,
    ): String {
        val base = current.trimStart('0')
        if (digits.all { it == '0' }) {
            if (base.isEmpty()) return current
            val free = MAX_INTEGER_DIGITS - base.length
            if (free <= 0) return current
            return base + "0".repeat(minOf(digits.length, free))
        }
        val stripped = (current + digits).trimStart('0')
        if (stripped.isEmpty()) return current
        if (stripped.length > MAX_INTEGER_DIGITS) return current
        return stripped
    }
}
