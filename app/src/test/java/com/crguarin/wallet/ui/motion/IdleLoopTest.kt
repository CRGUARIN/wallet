package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class IdleLoopTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the loop runs while the window is open`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    val running = rememberIdleLoop(playKey = "open", interrupted = false)
                    Text(if (running.value) "Corre" else "Quieto")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)

        composeRule.onNodeWithText("Corre").assertIsDisplayed()
    }

    @Test
    fun `the loop stops after ten seconds`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    val running = rememberIdleLoop(playKey = "window", interrupted = false)
                    Text(if (running.value) "Corre" else "Quieto")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("Corre").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(10_001L)

        composeRule.onNodeWithText("Quieto").assertIsDisplayed()
    }

    @Test
    fun `an interruption stops the loop for good`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    // In production this flag is `listState.isScrollInProgress`: composition state.
                    var interrupted by remember { mutableStateOf(false) }
                    val running = rememberIdleLoop(playKey = "stop", interrupted = interrupted)
                    Column {
                        Text(if (running.value) "Corre" else "Quieto")
                        // A tap stands in for the user scrolling: it interrupts the idle loop.
                        Box(modifier = Modifier.clickable { interrupted = true }) {
                            Text("Toca")
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Corre").assertIsDisplayed()

        composeRule.onNodeWithText("Toca").performClick()

        composeRule.onNodeWithText("Quieto").assertIsDisplayed()
    }

    @Test
    fun `an interruption already at the start means the loop never runs`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    val running = rememberIdleLoop(playKey = "early", interrupted = true)
                    Text(if (running.value) "Corre" else "Quieto")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("Quieto").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(10_001L)

        composeRule.onNodeWithText("Quieto").assertIsDisplayed()
    }

    @Test
    fun `releasing an interruption does not restart the loop`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    var interrupted by remember { mutableStateOf(false) }
                    val running = rememberIdleLoop(playKey = "release", interrupted = interrupted)
                    Column {
                        Text(if (running.value) "Corre" else "Quieto")
                        Box(modifier = Modifier.clickable { interrupted = !interrupted }) {
                            Text("Alterna")
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("Corre").assertIsDisplayed()

        composeRule.onNodeWithText("Alterna").performClick()
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.onNodeWithText("Quieto").assertIsDisplayed()

        composeRule.onNodeWithText("Alterna").performClick()
        composeRule.mainClock.advanceTimeBy(50L)

        composeRule.onNodeWithText("Quieto").assertIsDisplayed()
    }

    @Test
    fun `without animations the loop never runs`() {
        composeRule.mainClock.autoAdvance = false
        var running: State<Boolean>? = null
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                running = rememberIdleLoop(playKey = "quiet", interrupted = false)
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(running?.value).isFalse()
    }
}
