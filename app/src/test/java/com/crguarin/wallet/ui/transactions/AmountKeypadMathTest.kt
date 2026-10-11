package com.crguarin.wallet.ui.transactions

import com.crguarin.wallet.ui.components.AmountVisualTransformation
import com.crguarin.wallet.ui.transactions.AmountKeypadMath.KeypadKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Unit tests for [AmountKeypadMath], covering every rule from §9.1 in both es-CO (decimal ',')
 * and en-US (decimal '.').
 */
class AmountKeypadMathTest {
    private val es = Locale("es", "CO")
    private val en = Locale.US
    private val locales = listOf(Locale("es", "CO"), Locale.US)

    private fun sep(locale: Locale): Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    @Test
    fun `locales use the expected decimal separators`() {
        assertThat(sep(es)).isEqualTo(',')
        assertThat(sep(en)).isEqualTo('.')
    }

    @Test
    fun `typing digits builds the raw text`() {
        for (locale in locales) {
            var current = ""
            for (digit in listOf(2, 8, 0, 0, 0)) {
                current = AmountKeypadMath.press(current, KeypadKey.Digit(digit), locale)
            }
            assertThat(current).isEqualTo("28000")
        }
    }

    @Test
    fun `zero on empty does nothing`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("", KeypadKey.Digit(0), locale)).isEmpty()
        }
    }

    @Test
    fun `triple zero on empty does nothing`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("", KeypadKey.TripleZero, locale)).isEmpty()
        }
    }

    @Test
    fun `digit on a lone zero replaces it`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("0", KeypadKey.Digit(5), locale)).isEqualTo("5")
        }
    }

    @Test
    fun `zero on a lone zero does nothing`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("0", KeypadKey.Digit(0), locale)).isEqualTo("0")
        }
    }

    @Test
    fun `triple zero on a lone zero does nothing`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("0", KeypadKey.TripleZero, locale)).isEqualTo("0")
        }
    }

    @Test
    fun `non-zero digit appends to the integer part`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("28000", KeypadKey.Digit(7), locale)).isEqualTo("280007")
        }
    }

    @Test
    fun `triple zero appends three zeros`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("28", KeypadKey.TripleZero, locale)).isEqualTo("28000")
        }
    }

    @Test
    fun `twelve integer digits are accepted`() {
        for (locale in locales) {
            var current = ""
            for (digit in "123456789012".map { it - '0' }) {
                current = AmountKeypadMath.press(current, KeypadKey.Digit(digit), locale)
            }
            assertThat(current).isEqualTo("123456789012")
        }
    }

    @Test
    fun `a thirteenth integer digit does nothing`() {
        for (locale in locales) {
            val full = "123456789012"
            assertThat(AmountKeypadMath.press(full, KeypadKey.Digit(3), locale)).isEqualTo(full)
            assertThat(AmountKeypadMath.press(full, KeypadKey.Digit(0), locale)).isEqualTo(full)
        }
    }

    @Test
    fun `triple zero on a full integer part does nothing`() {
        for (locale in locales) {
            val full = "123456789012"
            assertThat(AmountKeypadMath.press(full, KeypadKey.TripleZero, locale)).isEqualTo(full)
        }
    }

    @Test
    fun `triple zero adds only what fits`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("99999999999", KeypadKey.TripleZero, locale))
                .isEqualTo("999999999990")
            assertThat(AmountKeypadMath.press("9999999999", KeypadKey.TripleZero, locale))
                .isEqualTo("999999999900")
            assertThat(AmountKeypadMath.press("1", KeypadKey.TripleZero, locale)).isEqualTo("1000")
        }
    }

    @Test
    fun `digit on one decimal digit completes the fraction`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.press("1500${s}5", KeypadKey.Digit(7), locale))
                .isEqualTo("1500${s}57")
        }
    }

    @Test
    fun `digit on two decimal digits does nothing`() {
        for (locale in locales) {
            val s = sep(locale)
            val full = "1500${s}50"
            assertThat(AmountKeypadMath.press(full, KeypadKey.Digit(7), locale)).isEqualTo(full)
            assertThat(AmountKeypadMath.press(full, KeypadKey.Digit(0), locale)).isEqualTo(full)
        }
    }

    @Test
    fun `digit on a trailing separator starts the fraction`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.press("1500$s", KeypadKey.Digit(7), locale))
                .isEqualTo("1500${s}7")
        }
    }

    @Test
    fun `triple zero fills the fraction up to two digits`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.press("1500$s", KeypadKey.TripleZero, locale))
                .isEqualTo("1500${s}00")
            assertThat(AmountKeypadMath.press("1500${s}1", KeypadKey.TripleZero, locale))
                .isEqualTo("1500${s}10")
            val full = "1500${s}50"
            assertThat(AmountKeypadMath.press(full, KeypadKey.TripleZero, locale)).isEqualTo(full)
        }
    }

    @Test
    fun `integer cap still applies with a decimal part`() {
        for (locale in locales) {
            val s = sep(locale)
            val full = "123456789012${s}5"
            assertThat(AmountKeypadMath.press(full, KeypadKey.Digit(7), locale))
                .isEqualTo("123456789012${s}57")
        }
    }

    @Test
    fun `backspace removes the last digit`() {
        assertThat(AmountKeypadMath.backspace("28000")).isEqualTo("2800")
        assertThat(AmountKeypadMath.backspace("5")).isEqualTo("")
        assertThat(AmountKeypadMath.backspace("")).isEqualTo("")
    }

    @Test
    fun `backspace on one decimal digit leaves the integer part`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.backspace("1500${s}5")).isEqualTo("1500")
        }
    }

    @Test
    fun `backspace on two decimal digits removes one`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.backspace("1500${s}50")).isEqualTo("1500${s}5")
        }
    }

    @Test
    fun `backspace on a trailing separator removes it`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.backspace("1500$s")).isEqualTo("1500")
        }
    }

    @Test
    fun `pressing the backspace key delegates to backspace`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.press("28000", KeypadKey.Backspace, locale)).isEqualTo("2800")
            assertThat(AmountKeypadMath.press("1500${s}5", KeypadKey.Backspace, locale))
                .isEqualTo("1500")
            assertThat(AmountKeypadMath.press("", KeypadKey.Backspace, locale)).isEmpty()
        }
    }

    @Test
    fun `press on invalid text does nothing`() {
        assertThat(AmountKeypadMath.press("12.34", KeypadKey.Digit(5), es)).isEqualTo("12.34")
        assertThat(AmountKeypadMath.press("12,34", KeypadKey.Digit(5), en)).isEqualTo("12,34")
    }

    @Test
    fun `every result is valid input for its locale`() {
        for (locale in locales) {
            val s = sep(locale)
            val currents =
                listOf(
                    "",
                    "0",
                    "5",
                    "28000",
                    "123456789012",
                    "1500$s",
                    "1500${s}5",
                    "1500${s}50",
                )
            val keys =
                listOf(
                    KeypadKey.Digit(0),
                    KeypadKey.Digit(5),
                    KeypadKey.TripleZero,
                    KeypadKey.Backspace,
                )
            for (current in currents) {
                for (key in keys) {
                    val result = AmountKeypadMath.press(current, key, locale)
                    assertThat(AmountVisualTransformation.isValidInput(result, locale)).isTrue()
                }
                assertThat(AmountVisualTransformation.isValidInput(AmountKeypadMath.backspace(current), locale))
                    .isTrue()
            }
        }
    }

    @Test
    fun `leading zeros collapse when a non-zero digit arrives`() {
        for (locale in locales) {
            assertThat(AmountKeypadMath.press("00", KeypadKey.Digit(5), locale)).isEqualTo("5")
            assertThat(AmountKeypadMath.press("007", KeypadKey.Digit(0), locale)).isEqualTo("70")
        }
    }

    @Test
    fun `a decimal part starting at zero still accepts digits`() {
        for (locale in locales) {
            val s = sep(locale)
            assertThat(AmountKeypadMath.press("0${s}5", KeypadKey.Digit(7), locale))
                .isEqualTo("0${s}57")
        }
    }

    @Test
    fun `backspace on a lone zero clears the field`() {
        assertThat(AmountKeypadMath.backspace("0")).isEmpty()
    }

    @Test
    fun `digit outside 0 to 9 is rejected`() {
        try {
            KeypadKey.Digit(10)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected).hasMessageThat().isNotEmpty()
        }
    }
}
