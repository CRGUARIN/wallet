package com.crguarin.wallet.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Visual transformation that formats monetary text with thousands separators
 * according to [locale] while keeping the raw string untouched.
 */
class AmountVisualTransformation(
    val locale: Locale = Locale.getDefault(),
) : VisualTransformation {
    private val symbols = DecimalFormatSymbols.getInstance(locale)
    val decimalSeparator: Char = symbols.decimalSeparator
    val groupingSeparator: Char = symbols.groupingSeparator

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val decIndex = original.indexOf(decimalSeparator)
        val integerPart = if (decIndex >= 0) original.substring(0, decIndex) else original
        val intLen = integerPart.length

        val transformedBuilder = java.lang.StringBuilder()
        val origToTrans = IntArray(original.length + 1)
        val transToOrig = mutableListOf<Int>()

        origToTrans[0] = 0

        for (i in 0 until intLen) {
            transToOrig.add(i)
            transformedBuilder.append(integerPart[i])
            origToTrans[i + 1] = transformedBuilder.length

            if ((intLen - 1 - i) > 0 && (intLen - 1 - i) % 3 == 0) {
                transToOrig.add(i + 1)
                transformedBuilder.append(groupingSeparator)
                origToTrans[i + 1] = transformedBuilder.length
            }
        }

        for (i in intLen until original.length) {
            transToOrig.add(i)
            transformedBuilder.append(original[i])
            origToTrans[i + 1] = transformedBuilder.length
        }
        transToOrig.add(original.length)

        val transformedString = transformedBuilder.toString()
        val transToOrigArray = transToOrig.toIntArray()

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val clamped = offset.coerceIn(0, original.length)
                    return origToTrans[clamped].coerceIn(0, transformedString.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val clamped = offset.coerceIn(0, transformedString.length)
                    return transToOrigArray[clamped].coerceIn(0, original.length)
                }
            }

        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }

    companion object {
        /**
         * Validates if [input] contains only digits and at most one decimal separator (matching [locale]),
         * with at most 2 decimal places.
         */
        fun isValidInput(
            input: String,
            locale: Locale = Locale.getDefault(),
        ): Boolean {
            if (input.isEmpty()) return true
            val decSep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
            var separatorCount = 0
            var decimalsCount = 0
            var hasDecimal = false

            for (ch in input) {
                if (ch == decSep) {
                    separatorCount++
                    if (separatorCount > 1) return false
                    hasDecimal = true
                } else if (ch in '0'..'9') {
                    if (hasDecimal) {
                        decimalsCount++
                        if (decimalsCount > 2) return false
                    }
                } else {
                    return false
                }
            }
            return true
        }
    }
}
