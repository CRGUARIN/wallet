package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
class KeycapTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `clicking the face calls onClick`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(onClick = { clicks += 1 }, face = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNodeWithText("Tecla").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `a disabled keycap is not clickable`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = { clicks += 1 },
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                ) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNodeWithText("Tecla").assertIsNotEnabled()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun `the touch area covers at least 48 dp`() {
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = {},
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(52.dp),
                ) {
                    Text("Tecla")
                }
            }
        }

        composeRule.onNode(hasClickAction()).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `long press calls onLongClick when given`() {
        var longs = 0
        composeRule.setContent {
            WalletTheme(darkTheme = false, dynamicColor = false) {
                Keycap(
                    onClick = {},
                    face = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    role = Role.Button,
                    onLongClick = { longs += 1 },
                ) {
                    Text("Tecla")
                }
            }
        }
        composeRule.onNodeWithText("Tecla").performTouchInput { longClick() }

        assertThat(longs).isEqualTo(1)
    }
}
