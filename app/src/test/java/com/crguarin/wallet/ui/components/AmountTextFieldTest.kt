package com.crguarin.wallet.ui.components

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class AmountTextFieldTest {
    private val esLocale = Locale.forLanguageTag("es-CO")
    private val enLocale = Locale.forLanguageTag("en-US")

    // -------------------------------------------------------------
    // Input Filtering Tests
    // -------------------------------------------------------------

    @Test
    fun `input filtering accepts digits and empty text`() {
        assertThat(AmountVisualTransformation.isValidInput("", esLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("123456", esLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("0", esLocale)).isTrue()
    }

    @Test
    fun `input filtering accepts valid decimal inputs in ES locale`() {
        val decSep = AmountVisualTransformation(esLocale).decimalSeparator
        assertThat(AmountVisualTransformation.isValidInput("123$decSep", esLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("123${decSep}4", esLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("123${decSep}45", esLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("${decSep}5", esLocale)).isTrue()
    }

    @Test
    fun `input filtering accepts valid decimal inputs in EN locale`() {
        assertThat(AmountVisualTransformation.isValidInput("123.", enLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("123.4", enLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput("123.45", enLocale)).isTrue()
        assertThat(AmountVisualTransformation.isValidInput(".5", enLocale)).isTrue()
    }

    @Test
    fun `input filtering rejects letters, special symbols, negative sign`() {
        assertThat(AmountVisualTransformation.isValidInput("12a3", esLocale)).isFalse()
        assertThat(AmountVisualTransformation.isValidInput("-500", esLocale)).isFalse()
        assertThat(AmountVisualTransformation.isValidInput("$100", esLocale)).isFalse()
        assertThat(AmountVisualTransformation.isValidInput("100 000", esLocale)).isFalse()
    }

    @Test
    fun `input filtering rejects multiple decimal separators`() {
        val decSep = AmountVisualTransformation(esLocale).decimalSeparator
        assertThat(AmountVisualTransformation.isValidInput("10${decSep}5${decSep}2", esLocale)).isFalse()
        assertThat(AmountVisualTransformation.isValidInput("10.5.2", enLocale)).isFalse()
    }

    @Test
    fun `input filtering rejects more than 2 decimal digits`() {
        val decSep = AmountVisualTransformation(esLocale).decimalSeparator
        assertThat(AmountVisualTransformation.isValidInput("10${decSep}123", esLocale)).isFalse()
        assertThat(AmountVisualTransformation.isValidInput("10.123", enLocale)).isFalse()
    }

    // -------------------------------------------------------------
    // VisualTransformation & OffsetMapping Tests
    // -------------------------------------------------------------

    @Test
    fun `transformation formats integer without decimals in ES locale`() {
        val vt = AmountVisualTransformation(esLocale)
        val result = vt.filter(AnnotatedString("1234567"))
        val grp = vt.groupingSeparator
        assertThat(result.text.text).isEqualTo("1${grp}234${grp}567")

        val mapping = result.offsetMapping
        // Cursor at 0
        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.transformedToOriginal(0)).isEqualTo(0)

        // Cursor at end (original 7 -> transformed 9)
        assertThat(mapping.originalToTransformed(7)).isEqualTo(9)
        assertThat(mapping.transformedToOriginal(9)).isEqualTo(7)

        // Bidirectional round-trip for every original position
        for (i in 0..7) {
            val trans = mapping.originalToTransformed(i)
            val back = mapping.transformedToOriginal(trans)
            assertThat(back).isEqualTo(i)
        }
    }

    @Test
    fun `transformation formats with decimals in ES locale`() {
        val vt = AmountVisualTransformation(esLocale)
        val dec = vt.decimalSeparator
        val grp = vt.groupingSeparator
        val raw = "1234567${dec}89"
        val result = vt.filter(AnnotatedString(raw))

        assertThat(result.text.text).isEqualTo("1${grp}234${grp}567${dec}89")

        val mapping = result.offsetMapping
        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(raw.length)).isEqualTo(result.text.text.length)

        for (i in 0..raw.length) {
            val trans = mapping.originalToTransformed(i)
            val back = mapping.transformedToOriginal(trans)
            assertThat(back).isEqualTo(i)
        }
    }

    @Test
    fun `transformation formats without decimals in EN locale`() {
        val vt = AmountVisualTransformation(enLocale)
        val result = vt.filter(AnnotatedString("1234567"))
        assertThat(result.text.text).isEqualTo("1,234,567")

        val mapping = result.offsetMapping
        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.transformedToOriginal(0)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(7)).isEqualTo(9)
        assertThat(mapping.transformedToOriginal(9)).isEqualTo(7)

        for (i in 0..7) {
            val trans = mapping.originalToTransformed(i)
            val back = mapping.transformedToOriginal(trans)
            assertThat(back).isEqualTo(i)
        }
    }

    @Test
    fun `transformation formats with decimals in EN locale`() {
        val vt = AmountVisualTransformation(enLocale)
        val raw = "1234567.89"
        val result = vt.filter(AnnotatedString(raw))
        assertThat(result.text.text).isEqualTo("1,234,567.89")

        val mapping = result.offsetMapping
        assertThat(mapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(raw.length)).isEqualTo(12)
        assertThat(mapping.transformedToOriginal(0)).isEqualTo(0)
        assertThat(mapping.transformedToOriginal(12)).isEqualTo(raw.length)

        for (i in 0..raw.length) {
            val trans = mapping.originalToTransformed(i)
            val back = mapping.transformedToOriginal(trans)
            assertThat(back).isEqualTo(i)
        }
    }

    @Test
    fun `transformation handles empty string and single digit`() {
        val vt = AmountVisualTransformation(esLocale)
        val emptyResult = vt.filter(AnnotatedString(""))
        assertThat(emptyResult.text.text).isEmpty()
        assertThat(emptyResult.offsetMapping.originalToTransformed(0)).isEqualTo(0)
        assertThat(emptyResult.offsetMapping.transformedToOriginal(0)).isEqualTo(0)

        val singleResult = vt.filter(AnnotatedString("5"))
        assertThat(singleResult.text.text).isEqualTo("5")
        assertThat(singleResult.offsetMapping.originalToTransformed(1)).isEqualTo(1)
        assertThat(singleResult.offsetMapping.transformedToOriginal(1)).isEqualTo(1)
    }

    @Test
    fun `offset mapping clamps out of bounds offsets safely`() {
        val vt = AmountVisualTransformation(enLocale)
        val result = vt.filter(AnnotatedString("1000")) // Transformed: "1,000" (len 5)
        val mapping = result.offsetMapping

        assertThat(mapping.originalToTransformed(-5)).isEqualTo(0)
        assertThat(mapping.originalToTransformed(100)).isEqualTo(5)

        assertThat(mapping.transformedToOriginal(-10)).isEqualTo(0)
        assertThat(mapping.transformedToOriginal(100)).isEqualTo(4)
    }
}
