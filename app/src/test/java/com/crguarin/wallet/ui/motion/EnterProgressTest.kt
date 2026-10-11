package com.crguarin.wallet.ui.motion

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class EnterProgressTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `it goes from zero to the target on entrance`() {
        composeRule.mainClock.autoAdvance = false
        var progress: State<Float>? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    progress = rememberEnterProgress(target = 0.7f, playKey = "entrance")
                    Text("probe")
                }
            }
        }

        assertThat(progress?.value).isEqualTo(0f)

        composeRule.mainClock.advanceTimeBy(450L)

        assertThat(progress?.value).isWithin(0.001f).of(0.7f)
        composeRule.waitForIdle()
    }

    @Test
    fun `a later target animates from the current value`() {
        composeRule.mainClock.autoAdvance = false
        var progress: State<Float>? = null
        var target by mutableFloatStateOf(0.7f)
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme(darkTheme = false, dynamicColor = false) {
                    progress = rememberEnterProgress(target = target, playKey = "retarget")
                    Text("probe")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(450L)
        assertThat(progress?.value).isWithin(0.001f).of(0.7f)

        composeRule.runOnIdle { target = 0.3f }
        composeRule.mainClock.advanceTimeBy(450L)

        assertThat(progress?.value).isWithin(0.001f).of(0.3f)
        composeRule.waitForIdle()
    }

    @Test
    fun `without animations it returns the target at once`() {
        var progress: State<Float>? = null
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                progress = rememberEnterProgress(target = 0.7f, playKey = "still")
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(progress?.value).isWithin(0.001f).of(0.7f)
    }
}
