package com.crguarin.wallet.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * The progressive reveal (§9.3 and §9.6): with [revealProgressively] and an empty amount only the
 * top, the keypad and Save show; typing reveals the rest block by block and it sticks; edition
 * starts fully visible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-w360dp-h2400dp")
class TransactionProgressiveRevealTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cash =
        AccountWithBalance(
            account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(100_000L),
                    color = 1,
                    icon = "wallet",
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(100_000L),
        )

    private val food =
        Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")

    private fun show(
        amountText: String,
        revealProgressively: Boolean = true,
        isEditMode: Boolean = false,
        onAmountChanged: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState =
                        TransactionEditUiState(
                            isLoading = false,
                            isEditMode = isEditMode,
                            selectedType = TransactionType.EXPENSE,
                            amountText = amountText,
                            accountId = 1L,
                            accounts = listOf(cash),
                            categoryId = 10L,
                            selectedCategory = food,
                            date = LocalDate.of(2026, 9, 28),
                            today = LocalDate.of(2026, 9, 28),
                        ),
                    onNavigateBack = {},
                    onTypeSelected = {},
                    onAmountChanged = onAmountChanged,
                    onOpenAccountSheet = {},
                    onDismissAccountSheet = {},
                    onAccountSelected = {},
                    onOpenToAccountSheet = {},
                    onDismissToAccountSheet = {},
                    onToAccountSelected = {},
                    onSwapAccounts = {},
                    onOpenCategorySheet = {},
                    onDismissCategorySheet = {},
                    onCategorySelected = {},
                    onNewCategoryClick = {},
                    onNewAccountClick = {},
                    onOpenDatePicker = {},
                    onDismissDatePicker = {},
                    onDateSelected = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                    revealProgressively = revealProgressively,
                )
            }
        }
    }

    private fun amountField() = composeRule.onNode(hasSetTextAction() and hasImeAction(ImeAction.Done))

    @Test
    fun `with an empty amount only the top the keypad and save show`() {
        show(amountText = "")

        composeRule.onNodeWithText("¿Cuánto fue?").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar gasto").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tres ceros").assertIsDisplayed()
        composeRule.onNodeWithText("Categoría").assertDoesNotExist()
        composeRule.onNodeWithText("Cuenta").assertDoesNotExist()
        composeRule.onNodeWithText("Fecha").assertDoesNotExist()
        composeRule.onNodeWithText("Nota (opcional)").assertDoesNotExist()
    }

    @Test
    fun `typing the amount reveals the rest in the usual order`() {
        // The amount state is read inside the composition, so typing recomposes the screen.
        var amount by mutableStateOf("")
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState =
                        TransactionEditUiState(
                            isLoading = false,
                            selectedType = TransactionType.EXPENSE,
                            amountText = amount,
                            accountId = 1L,
                            accounts = listOf(cash),
                            categoryId = 10L,
                            selectedCategory = food,
                            date = LocalDate.of(2026, 9, 28),
                            today = LocalDate.of(2026, 9, 28),
                        ),
                    onNavigateBack = {},
                    onTypeSelected = {},
                    onAmountChanged = { amount = it },
                    onOpenAccountSheet = {},
                    onDismissAccountSheet = {},
                    onAccountSelected = {},
                    onOpenToAccountSheet = {},
                    onDismissToAccountSheet = {},
                    onToAccountSelected = {},
                    onSwapAccounts = {},
                    onOpenCategorySheet = {},
                    onDismissCategorySheet = {},
                    onCategorySelected = {},
                    onNewCategoryClick = {},
                    onNewAccountClick = {},
                    onOpenDatePicker = {},
                    onDismissDatePicker = {},
                    onDateSelected = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                    revealProgressively = true,
                )
            }
        }

        amountField().performTextInput("50000")

        fun top(text: String) =
            composeRule
                .onNodeWithText(text)
                .fetchSemanticsNode()
                .boundsInRoot.top

        val order = listOf("Gasto", "¿Cuánto fue?", "Categoría", "Cuenta", "Fecha", "Nota (opcional)").map(::top)
        assertThat(order).isInStrictOrder()
    }

    @Test
    fun `the reveal sticks when the amount is cleared again`() {
        var amount by mutableStateOf("50000")
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState =
                        TransactionEditUiState(
                            isLoading = false,
                            selectedType = TransactionType.EXPENSE,
                            amountText = amount,
                            accountId = 1L,
                            accounts = listOf(cash),
                            categoryId = 10L,
                            selectedCategory = food,
                            date = LocalDate.of(2026, 9, 28),
                            today = LocalDate.of(2026, 9, 28),
                        ),
                    onNavigateBack = {},
                    onTypeSelected = {},
                    onAmountChanged = { amount = it },
                    onOpenAccountSheet = {},
                    onDismissAccountSheet = {},
                    onAccountSelected = {},
                    onOpenToAccountSheet = {},
                    onDismissToAccountSheet = {},
                    onToAccountSelected = {},
                    onSwapAccounts = {},
                    onOpenCategorySheet = {},
                    onDismissCategorySheet = {},
                    onCategorySelected = {},
                    onNewCategoryClick = {},
                    onNewAccountClick = {},
                    onOpenDatePicker = {},
                    onDismissDatePicker = {},
                    onDateSelected = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                    revealProgressively = true,
                )
            }
        }

        composeRule.onNodeWithText("Categoría").assertIsDisplayed()

        amountField().performTextClearance()

        composeRule.onNodeWithText("Categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha").assertIsDisplayed()
    }

    @Test
    fun `edition starts fully visible`() {
        show(amountText = "", isEditMode = true)

        composeRule.onNodeWithText("Categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha").assertIsDisplayed()
    }

    @Test
    fun `without the progressive flag everything shows from the start`() {
        show(amountText = "", revealProgressively = false)

        composeRule.onNodeWithText("Categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha").assertIsDisplayed()
    }
}
