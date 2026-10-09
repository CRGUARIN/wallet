package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class HeroAmountTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `styles only the dollar sign and the space after it and keeps the text`() {
        val text = amountWithSmallSymbol("$ 1.250.000")

        assertThat(text.text).isEqualTo("$ 1.250.000")
        assertThat(text.spanStyles).hasSize(1)
        val span = text.spanStyles.single()
        assertThat(span.start).isEqualTo(0)
        assertThat(span.end).isEqualTo(2)
        assertThat(span.item.fontSize.value).isEqualTo(0.58f)
        assertThat(span.item.baselineShift!!.multiplier).isEqualTo(HERO_SYMBOL_RAISE)

        // Without a space after it, only the symbol itself is styled.
        assertThat(amountWithSmallSymbol("$5").spanStyles.single().end).isEqualTo(1)
    }

    @Test
    fun `finds the dollar sign after a minus sign and in other positions`() {
        val negative = amountWithSmallSymbol("-$ 1.120.000")
        val trailing = amountWithSmallSymbol("1.120.000 $")

        assertThat(negative.text).isEqualTo("-$ 1.120.000")
        assertThat(negative.spanStyles.single().start).isEqualTo(1)
        assertThat(negative.spanStyles.single().end).isEqualTo(3)
        assertThat(trailing.spanStyles.single().start).isEqualTo(trailing.text.length - 1)
        assertThat(trailing.spanStyles.single().end).isEqualTo(trailing.text.length)
    }

    @Test
    fun `text without a dollar sign is not styled and the symbol color can be dimmed`() {
        assertThat(amountWithSmallSymbol("1.250.000").spanStyles).isEmpty()

        val dimmed = amountWithSmallSymbol("$ 5", symbolColor = Color.Gray)
        assertThat(
            dimmed.spanStyles
                .single()
                .item.color,
        ).isEqualTo(Color.Gray)
    }

    @Test
    fun `renders the exact amount text on one line`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(300.dp)) {
                    HeroAmountText(text = "$ 2.340.500", color = MaterialTheme.walletColors.onHero)
                }
            }
        }

        composeRule.onNodeWithText("$ 2.340.500").assertExists()
    }

    @Test
    fun `a very long amount still shows its full text`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(250.dp)) {
                    HeroAmountText(text = "$ 999.999.999.999", color = MaterialTheme.walletColors.onHero)
                }
            }
        }

        composeRule.onNodeWithText("$ 999.999.999.999").assertExists()
    }
}
