package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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

/**
 * The relieved save button (§9.5): still a button with its text that reports enabled state,
 * shows a spinner while loading, and is not clickable while disabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class BottomActionBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        enabled: Boolean = true,
        isLoading: Boolean = false,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                BottomActionBar {
                    PrimaryActionButton(
                        text = "Guardar",
                        onClick = onClick,
                        enabled = enabled,
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    @Test
    fun `an enabled save button is clickable`() {
        var clicks = 0
        show(onClick = { clicks++ })

        composeRule
            .onNodeWithText("Guardar")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `a disabled save button is not clickable`() {
        var clicks = 0
        show(enabled = false, onClick = { clicks++ })

        composeRule.onNodeWithText("Guardar").assertIsDisplayed().assertIsNotEnabled()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun `a loading save button hides its text`() {
        show(isLoading = true)

        composeRule.onNodeWithText("Guardar").assertDoesNotExist()
    }
}
