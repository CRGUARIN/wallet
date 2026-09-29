package com.crguarin.wallet.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class StateComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loading state is announced to screen readers`() {
        composeRule.setContent { WalletTheme { LoadingState() } }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
    }

    @Test
    fun `error state shows the message and runs the action`() {
        var clicks = 0
        composeRule.setContent {
            WalletTheme {
                ErrorState(message = "Algo falló", actionLabel = "Reintentar", onActionClick = { clicks++ })
            }
        }

        composeRule.onNodeWithText("Algo falló").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertThat(clicks).isEqualTo(1)
    }
}
