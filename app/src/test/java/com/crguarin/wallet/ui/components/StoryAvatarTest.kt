package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
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
class StoryAvatarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the label and subtitle and reports the click`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme {
                StoryAvatar(
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF66BB6A),
                    label = "Efectivo",
                    onClick = { clicks++ },
                    subtitle = { Text(text = "3 viajes") },
                )
            }
        }

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("3 viajes").assertIsDisplayed()
        composeRule.onNodeWithText("Efectivo").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `an avatar that needs attention keeps working and adds no semantics`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme {
                Column {
                    StoryAvatar(
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF42A5F5),
                        label = "Visa",
                        onClick = { clicks++ },
                        needsAttention = true,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Visa").assertIsDisplayed().performClick()

        assertThat(clicks).isEqualTo(1)
        // The dot is decorative: the only clickable node is the avatar itself.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun `the touch target is at least 48 dp high`() {
        composeRule.setContent {
            WalletTheme {
                StoryAvatar(
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF66BB6A),
                    label = "Efectivo",
                    onClick = {},
                )
            }
        }

        composeRule.onAllNodes(hasClickAction()).onFirst().assertHeightIsAtLeast(48.dp)
    }
}
