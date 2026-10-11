package com.crguarin.wallet.ui.motion

import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class OneShotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetGate() = FirstShowGate.reset()

    @After
    fun restoreGate() = FirstShowGate.reset()

    @Test
    fun `the first claimant wins until reset`() {
        assertThat(FirstShowGate.claim("deal")).isTrue()
        assertThat(FirstShowGate.claim("deal")).isFalse()
        assertThat(FirstShowGate.claim("other")).isTrue()

        FirstShowGate.reset()

        assertThat(FirstShowGate.claim("deal")).isTrue()
    }

    @Test
    fun `rememberPlayedOnce starts false and keeps a manual true`() {
        var played: MutableState<Boolean>? = null
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                played = rememberPlayedOnce("probe")
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(played?.value).isFalse()

        composeRule.runOnIdle { played?.value = true }

        assertThat(played?.value).isTrue()
    }
}
