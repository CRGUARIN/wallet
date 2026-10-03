package com.crguarin.wallet.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class BrandIntroTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the app name and finishes after the animation`() {
        var finished = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { WalletTheme { BrandIntro(onFinished = { finished = true }) } }

        composeRule.mainClock.advanceTimeBy(700)
        // Reads app_name instead of hardcoding it: debug builds append "-debug" to the app name.
        val appName = ApplicationProvider.getApplicationContext<Context>().getString(R.string.app_name)
        composeRule.onNodeWithText(appName).assertExists()
        assertThat(finished).isFalse()

        composeRule.mainClock.advanceTimeBy(1_000)
        assertThat(finished).isTrue()
    }

    @Test
    fun `blocks taps on the content underneath while it plays`() {
        var clicks = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WalletTheme {
                Box {
                    Button(onClick = { clicks++ }) {
                        Text("Debajo")
                    }
                    BrandIntro(onFinished = {})
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithText("Debajo").performClick()

        assertThat(clicks).isEqualTo(0)
    }
}
