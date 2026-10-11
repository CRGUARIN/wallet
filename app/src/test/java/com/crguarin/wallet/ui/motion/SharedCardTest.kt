package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class SharedCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the card key carries the account id`() {
        assertThat(accountCardKey(7L)).isEqualTo("account-card-7")
    }

    @Test
    fun `shared bounds without scopes do not change the layout`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Box(
                    modifier =
                        Modifier
                            .accountCardSharedBounds(7L)
                            .size(48.dp)
                            .testTag("card"),
                ) {
                    Text("Tarjeta")
                }
            }
        }

        composeRule.onNodeWithText("Tarjeta").assertIsDisplayed()
        composeRule.onNodeWithTag("card").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `nav enter exit without a scope does not change the layout`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Box(
                    modifier =
                        Modifier
                            .navEnterExit(slideY = 90.dp, millis = 300)
                            .size(48.dp)
                            .testTag("block"),
                ) {
                    Text("Bloque")
                }
            }
        }

        composeRule.onNodeWithText("Bloque").assertIsDisplayed()
        composeRule.onNodeWithTag("block").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `the flight tilt without scopes stays at rest`() {
        var tilt = -1f
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                tilt = rememberSharedFlightTilt(24f).value
                Text("probe")
            }
        }
        composeRule.waitForIdle()

        assertThat(tilt).isEqualTo(24f)
    }

    @Test
    fun `raised surface keeps its shape and size`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Box(
                    modifier =
                        Modifier
                            .raisedSurface(RoundedCornerShape(8.dp))
                            .size(48.dp)
                            .testTag("surface"),
                ) {
                    Text("Superficie")
                }
            }
        }

        composeRule.onNodeWithText("Superficie").assertIsDisplayed()
        composeRule.onNodeWithTag("surface").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `card shadow keeps its shape and size`() {
        composeRule.setContent {
            WalletTheme(darkTheme = true, dynamicColor = false) {
                Box(
                    modifier =
                        Modifier
                            .cardShadow(RoundedCornerShape(22.dp))
                            .size(48.dp)
                            .testTag("shadow"),
                ) {
                    Text("Sombra")
                }
            }
        }

        composeRule.onNodeWithText("Sombra").assertIsDisplayed()
        composeRule.onNodeWithTag("shadow").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
    }
}
