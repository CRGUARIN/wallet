package com.crguarin.wallet.ui.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class TransactionEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val account =
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

    private val foodCategory =
        Category(id = 10L, name = "Alimentación", type = CategoryType.EXPENSE, color = 1, icon = "restaurant")

    private fun validExpenseState(
        amountText: String = "50000",
        categoryId: Long? = 10L,
    ) = TransactionEditUiState(
        isLoading = false,
        isEditMode = false,
        selectedType = TransactionType.EXPENSE,
        amountText = amountText,
        accountId = 1L,
        accounts = listOf(account),
        categoryId = categoryId,
        selectedCategory = if (categoryId != null) foodCategory else null,
        date = LocalDate.of(2026, 9, 28),
    )

    private fun setScreen(
        uiState: TransactionEditUiState,
        onSaveClick: () -> Unit = {},
        onTypeSelected: (TransactionType) -> Unit = {},
        onConfirmNegativeBalance: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onTypeSelected = onTypeSelected,
                    onAmountChanged = {},
                    onOpenAccountSheet = {},
                    onDismissAccountSheet = {},
                    onAccountSelected = {},
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
                    onSaveClick = onSaveClick,
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = onConfirmNegativeBalance,
                    onDismissNegativeBalance = {},
                )
            }
        }
    }

    @Test
    fun `save button is disabled without amount`() {
        setScreen(validExpenseState(amountText = ""))

        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `save button is disabled without category`() {
        setScreen(validExpenseState(categoryId = null))

        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `creating an expense calls the save callback`() {
        var saveClicked = false
        setScreen(validExpenseState(), onSaveClick = { saveClicked = true })

        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Guardar").performClick()

        assertThat(saveClicked).isTrue()
    }

    @Test
    fun `selecting the income segment notifies the callback`() {
        var selectedType: TransactionType? = null
        setScreen(validExpenseState(), onTypeSelected = { selectedType = it })

        composeRule.onNodeWithText("Ingreso").performClick()

        assertThat(selectedType).isEqualTo(TransactionType.INCOME)
    }

    @Test
    fun `negative balance dialog is shown and confirming notifies the callback`() {
        var confirmed = false
        setScreen(
            validExpenseState().copy(showNegativeBalanceDialog = true),
            onConfirmNegativeBalance = { confirmed = true },
        )

        composeRule
            .onNodeWithText("La cuenta quedará en negativo. ¿Guardar de todas formas?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Confirmar").performClick()

        assertThat(confirmed).isTrue()
    }
}
