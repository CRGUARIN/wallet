package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class PulseDotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the pulse is finite and then goes idle`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    PulseDot(color = Color.Red, size = 8.dp, pulse = true)
                }
            }
        }

        // Three rings of 480 ms with 120 ms gaps last 1_680 ms: after 3 s nothing may move, so
        // waiting for idle must return instead of hanging on an endless loop.
        composeRule.mainClock.advanceTimeBy(3_000L)
        composeRule.waitForIdle()

        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `without animations only the dot is drawn`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                PulseDot(color = Color.Red, size = 8.dp, pulse = true)
            }
        }

        composeRule.onRoot().assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun `a still dot never animates`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    Box(modifier = Modifier) {
                        PulseDot(color = Color.Red, size = 8.dp, pulse = false)
                    }
                }
            }
        }

        composeRule.onRoot().assertIsDisplayed()
        composeRule.waitForIdle()
    }
}
