package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
class StaggerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `delays grow with the index and cap at four`() {
        var clock: StaggerClock? = null
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                clock = rememberStaggerClock("delays")
                Text("clock")
            }
        }
        composeRule.waitForIdle()

        val delays = (0..5).map { clock?.delayFor(it) }
        assertThat(delays).containsExactly(0, 40, 80, 120, 160, 160).inOrder()
    }

    @Test
    fun `blocks stay touchable from the first frame`() {
        composeRule.mainClock.autoAdvance = false
        var clicks = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                val clock = rememberStaggerClock("touch")
                Box(modifier = Modifier.staggeredEnter(clock, 0).clickable { clicks += 1 }) {
                    Text("Bloque")
                }
            }
        }

        // The node is in the tree (and clickable) even before its entrance played a frame.
        composeRule.onNodeWithText("Bloque").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `every block reaches its final state`() {
        composeRule.mainClock.autoAdvance = false
        var clock: StaggerClock? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                clock = rememberStaggerClock("final")
                Text("clock")
            }
        }

        // Flush the first composition and launch the entrance, then play it through.
        composeRule.mainClock.advanceTimeBy(50L)
        composeRule.mainClock.advanceTimeBy(1_000L)

        (0..5).forEach { assertThat(clock?.fractionFor(it)).isEqualTo(1f) }
        composeRule.waitForIdle()
    }

    @Test
    fun `without animations everything is final at once`() {
        composeRule.mainClock.autoAdvance = false
        var clock: StaggerClock? = null
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                clock = rememberStaggerClock("still")
                Text("clock")
            }
        }

        (0..5).forEach { assertThat(clock?.fractionFor(it)).isEqualTo(1f) }
        composeRule.waitForIdle()
    }
}
