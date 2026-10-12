package com.crguarin.wallet.ui.transactions

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.crguarin.wallet.R
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
 * The layout of the movement form on a tall screen, where nothing needs scrolling: the order of its
 * fields, the frequent categories, the date chips, the amount and the keyboard action. The tests of
 * the save/delete flow are in [TransactionEditScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-w360dp-h2400dp")
class TransactionEditFormTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

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

    private val food = Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")
    private val transport = Category(id = 11L, name = "Transporte", type = CategoryType.EXPENSE, color = 2, icon = "bus")
    private val coffee =
        Category(id = 12L, name = "Café", type = CategoryType.EXPENSE, parentId = 10L, color = 3, icon = "coffee")

    private fun expenseState(
        amountText: String = "50000",
        categoryId: Long? = 10L,
    ) = TransactionEditUiState(
        isLoading = false,
        selectedType = TransactionType.EXPENSE,
        amountText = amountText,
        accountId = 1L,
        accounts = listOf(cash, savings),
        categoryId = categoryId,
        selectedCategory = if (categoryId == 10L) food else null,
        date = today,
        today = today,
    )

    private fun transferState() =
        expenseState().copy(
            selectedType = TransactionType.TRANSFER,
            categoryId = null,
            selectedCategory = null,
            toAccountId = 2L,
        )

    private fun withFrequent(
        state: TransactionEditUiState,
        selected: Long? = null,
    ) = state.copy(
        categoryId = selected,
        selectedCategory = listOf(food, transport, coffee).firstOrNull { it.id == selected },
        frequentCategories =
            listOf(
                FrequentCategory(transport),
                FrequentCategory(food),
                FrequentCategory(coffee, parent = food),
            ),
    )

    private fun show(
        uiState: TransactionEditUiState,
        onAmountChanged: (String) -> Unit = {},
        onOpenCategorySheet: () -> Unit = {},
        onCategorySelected: (Long) -> Unit = {},
        onTodayClick: () -> Unit = {},
        onYesterdayClick: () -> Unit = {},
        onSaveClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState = uiState,
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
                    onOpenCategorySheet = onOpenCategorySheet,
                    onDismissCategorySheet = {},
                    onCategorySelected = onCategorySelected,
                    onNewCategoryClick = {},
                    onNewAccountClick = {},
                    onOpenDatePicker = {},
                    onDismissDatePicker = {},
                    onDateSelected = {},
                    onTodayClick = onTodayClick,
                    onYesterdayClick = onYesterdayClick,
                    onNoteChanged = {},
                    onSaveClick = onSaveClick,
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }
    }

    private fun chip(label: String) = composeRule.onNode(hasText(label) and hasClickAction())

    private fun amountField() = composeRule.onNode(hasSetTextAction() and hasImeAction(ImeAction.Done))

    // ---- Order and layout

    @Test
    fun `fields follow the order type amount category account date note`() {
        show(expenseState())

        fun top(text: String) =
            composeRule
                .onNodeWithText(text)
                .fetchSemanticsNode()
                .boundsInRoot.top

        val order = listOf("Gasto", "Monto", "Categoría", "Cuenta", "Fecha", "Nota (opcional)").map(::top)
        assertThat(order).isInStrictOrder()
    }

    @Test
    fun `the three type segments fit in one row`() {
        show(expenseState())

        val bounds = listOf("Gasto", "Ingreso", "Transferencia").map { composeRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        assertThat(bounds.map { it.left }).isInStrictOrder()
        assertThat(bounds.map { it.top }.toSet()).hasSize(1)
    }

    @Test
    fun `the date chips and the date field share a row`() {
        show(expenseState())

        val todayChip = chip("Hoy").fetchSemanticsNode().boundsInRoot
        val dateField = composeRule.onNodeWithText("Fecha").fetchSemanticsNode().boundsInRoot
        assertThat(todayChip.top).isLessThan(dateField.bottom)
        assertThat(todayChip.bottom).isGreaterThan(dateField.top)
    }

    // ---- Save bar

    @Test
    fun `save button is shown in the bottom bar and enabled with a valid expense`() {
        var saves = 0
        show(expenseState(), onSaveClick = { saves++ })

        composeRule
            .onNodeWithText("Guardar")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertThat(saves).isEqualTo(1)
    }

    @Test
    fun `save button is shown but disabled while the form is incomplete`() {
        show(expenseState(categoryId = null))

        composeRule.onNodeWithText("Guardar").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun `a general error shows right above the save button`() {
        show(expenseState().copy(generalErrorRes = R.string.error_invalid_transaction))

        val error = composeRule.onNodeWithText("Movimiento no válido").fetchSemanticsNode()
        val save = composeRule.onNodeWithText("Guardar").fetchSemanticsNode()
        assertThat(error.boundsInRoot.bottom).isAtMost(save.boundsInRoot.top)
    }

    // ---- Frequent categories

    @Test
    fun `frequent categories show under the category selector in the given order`() {
        show(withFrequent(expenseState()))

        composeRule.onNodeWithText("Frecuentes").assertIsDisplayed()
        val lefts =
            listOf("Transporte", "Alimentación", "Alimentación › Café").map { label ->
                chip(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
        // Same row or following rows: reading order is left to right, then top to bottom.
        val readingOrder = lefts.sortedWith(compareBy({ it.top }, { it.left }))
        assertThat(readingOrder).isEqualTo(lefts)
        val selectorTop =
            composeRule
                .onNodeWithText("Categoría")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val accountTop =
            composeRule
                .onNodeWithText("Cuenta")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertThat(lefts.first().top).isGreaterThan(selectorTop)
        assertThat(lefts.last().bottom).isAtMost(accountTop)
    }

    @Test
    fun `tapping a frequent category chip selects that category`() {
        var selected: Long? = null
        show(withFrequent(expenseState()), onCategorySelected = { selected = it })

        chip("Transporte").performClick()

        assertThat(selected).isEqualTo(11L)
    }

    @Test
    fun `a subcategory chip reads parent and child and selects the child`() {
        var selected: Long? = null
        show(withFrequent(expenseState()), onCategorySelected = { selected = it })

        chip("Alimentación › Café").performClick()

        assertThat(selected).isEqualTo(12L)
    }

    @Test
    fun `the chip of the chosen category is selected and the others are not`() {
        show(withFrequent(expenseState(), selected = 10L))

        chip("Transporte").assertIsNotSelected()
        // The selector shows the chosen category too; among the nodes with that text the chip is selected.
        composeRule.onAllNodes(hasText("Alimentación") and hasClickAction()).assertAny(isSelected())
    }

    @Test
    fun `no frequent chips without data`() {
        show(expenseState())

        composeRule.onNodeWithText("Frecuentes").assertDoesNotExist()
    }

    @Test
    fun `frequent chips are hidden in transfers`() {
        show(transferState().copy(frequentCategories = listOf(FrequentCategory(transport))))

        composeRule.onNodeWithText("Frecuentes").assertDoesNotExist()
        composeRule.onNodeWithText("Transporte").assertDoesNotExist()
    }

    // ---- Date chips

    @Test
    fun `today chip is selected when the date is today`() {
        show(expenseState())

        chip("Hoy").assertIsSelected()
        chip("Ayer").assertIsNotSelected()
    }

    @Test
    fun `yesterday chip is selected when the date is yesterday`() {
        show(expenseState().copy(date = today.minusDays(1)))

        chip("Ayer").assertIsSelected()
        chip("Hoy").assertIsNotSelected()
    }

    @Test
    fun `neither chip is selected for another day`() {
        show(expenseState().copy(date = today.minusDays(5)))

        chip("Hoy").assertIsNotSelected()
        chip("Ayer").assertIsNotSelected()
    }

    @Test
    fun `today and yesterday chips call their callbacks`() {
        var todays = 0
        var yesterdays = 0
        show(
            expenseState().copy(date = today.minusDays(5)),
            onTodayClick = { todays++ },
            onYesterdayClick = { yesterdays++ },
        )

        chip("Hoy").performClick()
        chip("Ayer").performClick()

        assertThat(todays).isEqualTo(1)
        assertThat(yesterdays).isEqualTo(1)
    }

    // ---- Amount and keyboard action

    @Test
    fun `the amount shows a small dollar sign and grouped digits`() {
        show(expenseState(amountText = "1250000"))

        composeRule.onNodeWithText("$ 1.250.000").assertIsDisplayed()
    }

    @Test
    fun `typing in the amount field reports the text`() {
        var typed = ""
        show(expenseState(amountText = ""), onAmountChanged = { typed = it })

        amountField().performTextInput("12500")
        // The field reports its text from a snapshot flow, one frame after the edit.
        composeRule.waitForIdle()

        assertThat(typed).isEqualTo("12500")
    }

    @Test
    fun `done on the keyboard opens the category list when the category is missing`() {
        var sheets = 0
        show(expenseState(categoryId = null), onOpenCategorySheet = { sheets++ })

        amountField().performImeAction()

        assertThat(sheets).isEqualTo(1)
    }

    @Test
    fun `done on the keyboard leaves the category list closed when a category is chosen`() {
        var sheets = 0
        show(expenseState(), onOpenCategorySheet = { sheets++ })

        amountField().performImeAction()

        assertThat(sheets).isEqualTo(0)
    }

    @Test
    fun `done on the keyboard does not open the category list in a transfer`() {
        var sheets = 0
        show(transferState(), onOpenCategorySheet = { sheets++ })

        amountField().performImeAction()

        assertThat(sheets).isEqualTo(0)
    }
}
