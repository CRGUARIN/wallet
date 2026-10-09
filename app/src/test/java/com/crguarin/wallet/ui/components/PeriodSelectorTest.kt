package com.crguarin.wallet.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class PeriodSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var previous = 0
    private var next = 0

    private fun show(
        canGoPrevious: Boolean = true,
        canGoNext: Boolean = true,
    ) {
        composeRule.setContent {
            WalletTheme {
                PeriodSelector(
                    label = "Octubre 2026",
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    onPrevious = { previous++ },
                    onNext = { next++ },
                    previousContentDescription = "Mes anterior",
                    nextContentDescription = "Mes siguiente",
                )
            }
        }
    }

    @Test
    fun `label is a heading and both arrows work`() {
        show()

        composeRule
            .onNode(hasText("Octubre 2026") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mes anterior").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("Mes siguiente").assertIsEnabled().performClick()

        assertThat(previous).isEqualTo(1)
        assertThat(next).isEqualTo(1)
    }

    @Test
    fun `an arrow without a period behind it is disabled`() {
        show(canGoNext = false)

        composeRule.onNodeWithContentDescription("Mes anterior").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Mes siguiente").assertIsNotEnabled().performClick()

        assertThat(next).isEqualTo(0)
    }

    @Test
    fun `arrows have touch targets of at least 48 dp`() {
        show()

        listOf("Mes anterior", "Mes siguiente").forEach {
            composeRule.onNodeWithContentDescription(it).assertTouchWidthIsEqualTo(48.dp).assertTouchHeightIsEqualTo(48.dp)
        }
    }
}
