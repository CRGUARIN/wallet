package com.crguarin.wallet.ui.transactions

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
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
 * The in-app numeric pad (§9.1 and §9.6): typing through the keys, backspace and long-press,
 * keypad visibility with the amount focus, and the no-[Text] semantics that keep
 * `onNodeWithText("1")` pointing at the installments field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AmountKeypadTest {
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

    private val savings =
        AccountWithBalance(
            account =
                Account(
                    id = 2L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(500_000L),
                    color = 2,
                    icon = "savings",
                    sortOrder = 1,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(500_000L),
        )

    private fun keypadState(initial: String = ""): () -> String {
        var text by mutableStateOf(initial)
        composeRule.setContent {
            WalletTheme {
                AmountKeypad(
                    current = text,
                    onAmountChanged = { text = it },
                )
            }
        }
        return { text }
    }

    /** The activity's back dispatcher, so a test can press Back the way the system does. */
    private var backOwner: OnBackPressedDispatcherOwner? = null

    private fun amountField() = composeRule.onNode(hasSetTextAction() and hasImeAction(ImeAction.Done))

    private fun expenseScreen(
        amountText: String = "",
        isEditMode: Boolean = false,
        onAmountChanged: (String) -> Unit = {},
    ) {
        // The amount lives here, so the keypad's presses flow back into the field as in the app.
        var amount by mutableStateOf(amountText)
        composeRule.setContent {
            backOwner = LocalOnBackPressedDispatcherOwner.current
            WalletTheme {
                TransactionEditScreen(
                    uiState =
                        TransactionEditUiState(
                            isLoading = false,
                            isEditMode = isEditMode,
                            selectedType = TransactionType.EXPENSE,
                            amountText = amount,
                            accountId = 1L,
                            accounts = listOf(cash, savings),
                            date = LocalDate.of(2026, 9, 28),
                        ),
                    onNavigateBack = {},
                    onTypeSelected = {},
                    onAmountChanged = {
                        amount = it
                        onAmountChanged(it)
                    },
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
                    onTodayClick = {},
                    onYesterdayClick = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }
    }

    @Test
    fun `keys 2 8 and 000 produce 28000`() {
        val text = keypadState()

        composeRule.onNodeWithContentDescription("2").performClick()
        composeRule.onNodeWithContentDescription("8").performClick()
        composeRule.onNodeWithContentDescription("Tres ceros").performClick()

        assertThat(text()).isEqualTo("28000")
    }

    @Test
    fun `backspace removes the last digit`() {
        val text = keypadState("28000")

        composeRule.onNodeWithContentDescription("Borrar").performClick()

        assertThat(text()).isEqualTo("2800")
    }

    @Test
    fun `holding backspace clears the whole amount`() {
        val text = keypadState("28000")

        composeRule.onNodeWithContentDescription("Borrar").performSemanticsAction(SemanticsActions.OnLongClick)

        assertThat(text()).isEmpty()
    }

    @Test
    fun `keys expose no text so digit searches skip them`() {
        keypadState()

        // The nine digits plus 000/0 must not be Text nodes: this is what keeps
        // onNodeWithText("1") pointing at the installments field.
        composeRule.onNodeWithText("1").assertDoesNotExist()
        composeRule.onNodeWithText("000").assertDoesNotExist()
        composeRule.onNode(hasContentDescription("Tres ceros")).assertIsDisplayed()
    }

    @Test
    fun `typing on the keypad formats the amount in the field`() {
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
                            date = LocalDate.of(2026, 9, 28),
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
                    onTodayClick = {},
                    onYesterdayClick = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("2").performClick()
        composeRule.onNodeWithContentDescription("8").performClick()
        composeRule.onNodeWithContentDescription("Tres ceros").performClick()

        assertThat(amount).isEqualTo("28000")
        // The small test window only fits the top: scroll the field into view first.
        composeRule
            .onNode(hasSetTextAction() and hasImeAction(ImeAction.Done))
            .performScrollTo()
        composeRule.onNodeWithText("$ 28.000").assertIsDisplayed()
    }

    @Test
    fun `the keypad shows with the focused amount in creation`() {
        expenseScreen()

        // Creation focuses the amount, so the in-app keypad is visible right away.
        composeRule.onNodeWithContentDescription("Tres ceros").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Borrar").assertIsDisplayed()
    }

    @Test
    fun `the keypad stays hidden in edition without focus`() {
        expenseScreen(amountText = "50000", isEditMode = true)

        composeRule.onNodeWithContentDescription("Tres ceros").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Borrar").assertDoesNotExist()
    }

    @Test
    fun `touching the category selector moves the focus out and hides the keypad`() {
        var sheets = 0
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
                            date = LocalDate.of(2026, 9, 28),
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
                    onOpenCategorySheet = { sheets++ },
                    onDismissCategorySheet = {},
                    onCategorySelected = {},
                    onNewCategoryClick = {},
                    onNewAccountClick = {},
                    onOpenDatePicker = {},
                    onDismissDatePicker = {},
                    onDateSelected = {},
                    onTodayClick = {},
                    onYesterdayClick = {},
                    onNoteChanged = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }
        // The Done matcher keeps the amount field unambiguous: the note field also edits text.
        composeRule.onNode(hasSetTextAction() and hasImeAction(ImeAction.Done)).performTextInput("5")
        composeRule.onNodeWithContentDescription("Tres ceros").assertIsDisplayed()

        composeRule.onNodeWithText("Categoría").performScrollTo().performClick()

        assertThat(sheets).isEqualTo(1)
        composeRule.onNodeWithContentDescription("Tres ceros").assertDoesNotExist()
    }

    @Test
    fun `keypad digits keep the caret at the end of the amount`() {
        expenseScreen()

        composeRule.onNodeWithContentDescription("2").performClick()
        composeRule.onNodeWithContentDescription("8").performClick()

        val node = amountField().fetchSemanticsNode()
        val shown = node.config[SemanticsProperties.EditableText].text
        val selection = node.config[SemanticsProperties.TextSelectionRange]
        // The "$ " is an output transformation, so the semantics report the caret on the shown text.
        assertThat(shown).isEqualTo("$ 28")
        assertThat(selection).isEqualTo(TextRange(shown.length))
    }

    @Test
    fun `tapping the unfocused amount focuses it and shows the keypad`() {
        expenseScreen(amountText = "50000", isEditMode = true)
        composeRule.onNodeWithContentDescription("Tres ceros").assertDoesNotExist()

        amountField().performClick()

        amountField().assertIsFocused()
        composeRule.onNodeWithContentDescription("Tres ceros").assertIsDisplayed()
    }

    @Test
    fun `back closes the keypad and keeps the form open`() {
        expenseScreen()
        composeRule.onNodeWithContentDescription("Tres ceros").assertIsDisplayed()

        composeRule.runOnIdle { backOwner?.onBackPressedDispatcher?.onBackPressed() }

        composeRule.onNodeWithContentDescription("Tres ceros").assertDoesNotExist()
        composeRule.onNodeWithText("Monto").assertIsDisplayed()
    }
}
