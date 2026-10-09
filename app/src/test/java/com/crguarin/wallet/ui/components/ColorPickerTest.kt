package com.crguarin.wallet.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ColorPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val red = 0xFFEF5350.toInt()
    private val blue = 0xFF42A5F5.toInt()
    private val green = 0xFF66BB6A.toInt()

    @Test
    fun `disabled colors are announced as in use and not enabled`() {
        composeRule.setContent {
            WalletTheme {
                ColorPicker(
                    selectedColor = red,
                    onColorSelected = {},
                    disabledColors = setOf(blue),
                    colors = listOf(green, blue, red),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Color en uso").assertIsNotEnabled()
    }

    @Test
    fun `enabled colors can still be picked`() {
        var picked: Int? = null
        composeRule.setContent {
            WalletTheme {
                ColorPicker(
                    selectedColor = red,
                    onColorSelected = { picked = it },
                    disabledColors = setOf(blue),
                    colors = listOf(green, blue, red),
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Color").onFirst().performClick()

        assertThat(picked).isEqualTo(green)
    }

    @Test
    fun `selected color is never disabled even when listed as used`() {
        composeRule.setContent {
            WalletTheme {
                ColorPicker(
                    selectedColor = blue,
                    onColorSelected = {},
                    disabledColors = setOf(blue),
                    colors = listOf(green, blue, red),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Color en uso").assertDoesNotExist()
    }

    @Test
    fun `every swatch has a touch target of at least 48 dp`() {
        composeRule.setContent {
            WalletTheme {
                ColorPicker(
                    selectedColor = red,
                    onColorSelected = {},
                    disabledColors = setOf(blue),
                    colors = listOf(green, blue, red),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Color en uso").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeRule
            .onAllNodesWithContentDescription("Color")
            .onFirst()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
