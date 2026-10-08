package com.crguarin.wallet.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
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
class IconPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `collapsed picker hides group headers and offers to show all icons`() {
        composeRule.setContent {
            WalletTheme {
                IconPicker(selectedIcon = "wallet", onIconSelected = {})
            }
        }

        composeRule.onNodeWithText("Ver todos los íconos").assertExists()
        composeRule.onNodeWithText("Dinero").assertDoesNotExist()
    }

    @Test
    fun `show all reveals group headers and a show fewer button`() {
        composeRule.setContent {
            WalletTheme {
                IconPicker(selectedIcon = "wallet", onIconSelected = {})
            }
        }

        composeRule.onNodeWithText("Ver todos los íconos").performClick()

        composeRule.onNodeWithText("Dinero").assertExists()
        composeRule.onNodeWithText("Ver menos").assertExists()
    }

    @Test
    fun `clicking a collapsed icon reports its key`() {
        var picked: String? = null
        composeRule.setContent {
            WalletTheme {
                IconPicker(
                    selectedIcon = "",
                    onIconSelected = { picked = it },
                    icons = listOf("pets"),
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Ícono").onFirst().performClick()

        assertThat(picked).isEqualTo("pets")
    }

    @Test
    fun `clicking an expanded icon reports its key`() {
        var picked: String? = null
        composeRule.setContent {
            WalletTheme {
                IconPicker(
                    selectedIcon = "",
                    onIconSelected = { picked = it },
                )
            }
        }

        composeRule.onNodeWithText("Ver todos los íconos").performClick()
        composeRule.onAllNodesWithContentDescription("Ícono").onFirst().performClick()

        assertThat(picked).isEqualTo("wallet")
    }
}
