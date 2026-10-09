package com.crguarin.wallet.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO")
class AmountTextFieldUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val locale = Locale.forLanguageTag("es-CO")

    @Test
    fun `shows the Monto label and reports the typed digits`() {
        var value by mutableStateOf("")
        composeRule.setContent {
            WalletTheme {
                AmountTextField(value = value, onValueChange = { value = it }, locale = locale)
            }
        }

        composeRule.onNodeWithText("Monto").assertIsDisplayed()
        composeRule.onNodeWithText("Monto").performClick()
        composeRule.onNodeWithText("Monto").performTextInput("1250000")

        assertThat(value).isEqualTo("1250000")
    }

    @Test
    fun `shows the amount with thousands separators and a small dollar sign`() {
        composeRule.setContent {
            WalletTheme {
                AmountTextField(value = "1250000", onValueChange = {}, locale = locale)
            }
        }

        composeRule.onNodeWithText("1.250.000", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("$", substring = true).assertIsDisplayed()
    }

    @Test
    fun `rejects letters and keeps the previous value`() {
        var value by mutableStateOf("15")
        composeRule.setContent {
            WalletTheme {
                AmountTextField(value = value, onValueChange = { value = it }, locale = locale)
            }
        }

        composeRule.onNodeWithText("15", substring = true).performTextInput("ab")

        assertThat(value).isEqualTo("15")
    }

    @Test
    fun `the keyboard action reaches the screen`() {
        var done = 0
        composeRule.setContent {
            WalletTheme {
                AmountTextField(
                    value = "500",
                    onValueChange = {},
                    locale = locale,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { done++ }),
                )
            }
        }

        composeRule.onNodeWithText("500", substring = true).performClick()
        composeRule.onNodeWithText("500", substring = true).performImeAction()

        assertThat(done).isEqualTo(1)
    }

    @Test
    fun `shows the error message under the field`() {
        composeRule.setContent {
            WalletTheme {
                AmountTextField(value = "", onValueChange = {}, errorMessage = "Monto no válido", locale = locale)
            }
        }

        composeRule.onNodeWithText("Monto no válido").assertIsDisplayed()
    }

    @Test
    fun `the amount keeps its size when it fits`() {
        val size = fitAmountFontSize(base = 44.sp, shownLength = 9, availableDp = 296f, fontScale = 1f)

        assertThat(size).isEqualTo(44.sp)
    }

    @Test
    fun `a long amount shrinks so it fits on one line`() {
        val fifteenChars = fitAmountFontSize(base = 44.sp, shownLength = 15, availableDp = 268f, fontScale = 1f)

        assertThat(fifteenChars.value).isLessThan(44f)
        // 15 digits wide (0.64 em each) at that size take no more than the room there is.
        assertThat(15 * 0.64f * fifteenChars.value).isAtMost(268f + 0.5f)
    }

    @Test
    fun `a big font scale shrinks the amount and it still fits`() {
        val size = fitAmountFontSize(base = 44.sp, shownLength = 15, availableDp = 268f, fontScale = 2f)

        assertThat(size.value).isAtLeast(12f)
        assertThat(15 * 0.64f * size.value * 2f).isAtMost(268f + 1f)
    }

    @Test
    fun `an unmeasured field keeps the base size`() {
        assertThat(fitAmountFontSize(base = 44.sp, shownLength = 15, availableDp = -20f, fontScale = 1f)).isEqualTo(44.sp)
        assertThat(fitAmountFontSize(base = 44.sp, shownLength = 0, availableDp = 268f, fontScale = 1f)).isEqualTo(44.sp)
    }
}
