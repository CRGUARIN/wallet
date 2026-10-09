package com.crguarin.wallet.ui.debts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class DebtEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun validState() =
        DebtEditUiState(
            isLoading = false,
            personName = "Luis",
            amountText = "20000",
            date = LocalDate.of(2026, 10, 4),
        )

    private fun setScreen(
        uiState: DebtEditUiState,
        onSaveClick: () -> Unit = {},
        onDirectionSelected: (DebtDirection) -> Unit = {},
        onClearDueDate: () -> Unit = {},
        onOpenStartDatePicker: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                DebtEditScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onDirectionSelected = onDirectionSelected,
                    onPersonNameChanged = {},
                    onAmountChanged = {},
                    onOpenStartDatePicker = onOpenStartDatePicker,
                    onDismissStartDatePicker = {},
                    onStartDateSelected = {},
                    onOpenDueDatePicker = {},
                    onDismissDueDatePicker = {},
                    onDueDateSelected = {},
                    onClearDueDate = onClearDueDate,
                    onNoteChanged = {},
                    onSaveClick = onSaveClick,
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                )
            }
        }
    }

    @Test
    fun `save sits in the bottom bar and is enabled with a valid debt`() {
        var saves = 0
        setScreen(validState(), onSaveClick = { saves++ })

        composeRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").assertIsEnabled()
        composeRule.onNodeWithText("Guardar").performClick()

        assertThat(saves).isEqualTo(1)
    }

    @Test
    fun `save is disabled without a name`() {
        setScreen(validState().copy(personName = ""))

        composeRule.onNodeWithText("Guardar").assertIsNotEnabled()
    }

    @Test
    fun `direction segments show the current one and report the change`() {
        var direction: DebtDirection? = null
        setScreen(validState().copy(direction = DebtDirection.OWED_TO_ME), onDirectionSelected = { direction = it })

        composeRule.onNodeWithText("Me deben").assertIsSelected()
        composeRule.onNodeWithText("Debo").performClick()

        assertThat(direction).isEqualTo(DebtDirection.I_OWE)
    }

    @Test
    fun `the date field opens the date picker`() {
        var opened = 0
        setScreen(validState(), onOpenStartDatePicker = { opened++ })

        composeRule.onNodeWithText("Fecha").performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `a due date can be cleared`() {
        var cleared = 0
        setScreen(validState().copy(dueDate = LocalDate.of(2026, 11, 4)), onClearDueDate = { cleared++ })

        composeRule.onNodeWithText("Quitar fecha límite").performScrollTo().performClick()

        assertThat(cleared).isEqualTo(1)
    }
}
