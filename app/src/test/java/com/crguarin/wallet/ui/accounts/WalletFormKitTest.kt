package com.crguarin.wallet.ui.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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
class WalletFormKitTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `segmented control marks the chosen option and reports taps`() {
        var selected by mutableStateOf("Gasto")
        composeRule.setContent {
            WalletTheme {
                WalletSegmentedControl(
                    options = listOf("Gasto", "Ingreso"),
                    selected = selected,
                    onSelect = { selected = it },
                    label = { it },
                )
            }
        }

        composeRule.onNodeWithText("Gasto").assertIsSelected()
        composeRule.onNodeWithText("Ingreso").assertIsNotSelected()

        composeRule.onNodeWithText("Ingreso").performClick()

        assertThat(selected).isEqualTo("Ingreso")
        composeRule.onNodeWithText("Ingreso").assertIsSelected()
        composeRule.onNodeWithText("Gasto").assertIsNotSelected()
    }

    @Test
    fun `segments are at least 48 dp tall`() {
        composeRule.setContent {
            WalletTheme {
                WalletSegmentedControl(
                    options = listOf("Mes", "Año"),
                    selected = "Mes",
                    onSelect = {},
                    label = { it },
                )
            }
        }

        composeRule.onNodeWithText("Mes").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Año").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `a disabled segmented control ignores taps`() {
        var taps = 0
        composeRule.setContent {
            WalletTheme {
                WalletSegmentedControl(
                    options = listOf("Gasto", "Ingreso"),
                    selected = "Gasto",
                    onSelect = { taps++ },
                    label = { it },
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Ingreso").assertIsNotEnabled()
        composeRule.onNodeWithText("Ingreso").performClick()

        assertThat(taps).isEqualTo(0)
    }

    @Test
    fun `filter chip shows the state in its semantics and reports taps`() {
        var selected by mutableStateOf(false)
        composeRule.setContent {
            WalletTheme {
                WalletFilterChip(selected = selected, onClick = { selected = !selected }, label = "Semanal")
            }
        }

        composeRule.onNodeWithText("Semanal").assertIsNotSelected()
        composeRule.onNodeWithText("Semanal").performClick()
        composeRule.onNodeWithText("Semanal").assertIsSelected()
    }

    @Test
    fun `toggle card switches when any part of the row is tapped`() {
        var checked by mutableStateOf(false)
        composeRule.setContent {
            WalletTheme {
                ToggleCard(
                    title = "Incluir en el saldo total",
                    subtitle = "Suma esta cuenta al total",
                    checked = checked,
                    onCheckedChange = { checked = it },
                )
            }
        }

        composeRule.onNodeWithText("Incluir en el saldo total").assertIsOff()
        composeRule.onNodeWithText("Suma esta cuenta al total").performClick()

        assertThat(checked).isTrue()
        composeRule.onNodeWithText("Incluir en el saldo total").assertIsOn()
    }

    @Test
    fun `tonal selector shows its caption and value and opens the picker`() {
        var opened = 0
        composeRule.setContent {
            WalletTheme {
                TonalSelector(
                    label = "Fecha",
                    value = "4 oct 2026",
                    leadingIcon = Icons.Default.CalendarMonth,
                    onClick = { opened++ },
                )
            }
        }

        composeRule.onNodeWithText("Fecha").assertIsDisplayed()
        composeRule.onNodeWithText("4 oct 2026").assertIsDisplayed().performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `a disabled tonal selector does not open`() {
        var opened = 0
        composeRule.setContent {
            WalletTheme {
                TonalSelector(value = "Sin padre", onClick = { opened++ }, enabled = false)
            }
        }

        composeRule.onNodeWithText("Sin padre").assertIsNotEnabled()
        composeRule.onNodeWithText("Sin padre").performClick()

        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `option card reads as a selectable radio button`() {
        var selected by mutableStateOf(false)
        composeRule.setContent {
            WalletTheme {
                TonalOptionCard(selected = selected, onClick = { selected = true }) {
                    Text(text = "Efectivo")
                }
            }
        }

        composeRule.onNodeWithText("Efectivo").assertIsEnabled().assertIsNotSelected()
        composeRule.onNodeWithText("Efectivo").performClick()
        composeRule.onNodeWithText("Efectivo").assertIsSelected()
    }

    @Test
    fun `text field shows the error in place of the hint`() {
        var text by mutableStateOf("")
        composeRule.setContent {
            WalletTheme {
                WalletTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Nombre",
                    supportingText = "Un nombre corto",
                    errorRes = if (text.isEmpty()) R.string.error_name_empty else null,
                )
            }
        }

        composeRule.onNodeWithText("El nombre no puede estar vacío").assertIsDisplayed()
        composeRule.onNodeWithText("Un nombre corto").assertDoesNotExist()

        composeRule.onNodeWithText("Nombre").performTextInput("Ahorros")

        composeRule.onNodeWithText("Un nombre corto").assertIsDisplayed()
        composeRule.onNodeWithText("El nombre no puede estar vacío").assertDoesNotExist()
    }
}
