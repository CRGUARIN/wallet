package com.crguarin.wallet.ui.motion

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class RollingAmountTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the amount is a single node with the exact text and description`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                RollingAmountText(
                    text = "$ 12.500",
                    color = Color.White,
                    contentDescription = "$ 12.500",
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("$ 12.500").assertCountEquals(1)
        composeRule.onNodeWithText("$ 12.500").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("$ 12.500").assertIsDisplayed()
    }

    @Test
    fun `without a policy it draws the final text with no animation`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                RollingAmountText(
                    text = "$ 28.000",
                    color = Color.White,
                    rollFrom = OdometerMath.zeroed("$ 28.000"),
                )
            }
        }

        composeRule.onNodeWithText("$ 28.000").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun `with animations it rolls and then goes idle`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    RollingAmountText(
                        text = "$ 28.000",
                        color = Color.White,
                        rollFrom = OdometerMath.zeroed("$ 28.000"),
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("$ 28.000").assertCountEquals(1)
    }

    @Test
    fun `rememberRollFrom rolls from zero on first show when asked`() {
        var from: String? = "unset"
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                from = rememberRollFrom(text = "$ 28.000", saveKey = "zero", fromZeroOnFirstShow = true)
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(from).isEqualTo("$ 00.000")
    }

    @Test
    fun `rememberRollFrom stays still on first show unless asked`() {
        var from: String? = "unset"
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                from = rememberRollFrom(text = "$ 28.000", saveKey = "still", fromZeroOnFirstShow = false)
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(from).isNull()
    }

    @Test
    fun `rememberRollFrom rolls from the previous text when it changes`() {
        var from: String? = "unset"
        var text by mutableStateOf("$ 12.500")
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                from = rememberRollFrom(text = text, saveKey = "change", fromZeroOnFirstShow = false)
                Text("probe")
            }
        }
        composeRule.waitForIdle()
        assertThat(from).isNull()

        composeRule.runOnIdle { text = "$ 28.000" }
        composeRule.waitForIdle()

        assertThat(from).isEqualTo("$ 12.500")
    }

    @Test
    fun `rememberRollFrom is always null without animations`() {
        var from: String? = "unset"
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                from = rememberRollFrom(text = "$ 28.000", saveKey = "quiet", fromZeroOnFirstShow = true)
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(from).isNull()
    }
}
