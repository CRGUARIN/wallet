package com.crguarin.wallet.ui.transactions

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
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
class TransactionEditCardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cardAccount =
        AccountWithBalance(
            account =
                Account(
                    id = 2L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money.ZERO,
                    color = 2,
                    icon = "credit_card",
                    createdAt = Instant.EPOCH,
                    creditLimit = Money(1_000_000L),
                    statementDay = 15,
                    paymentDueDay = 5,
                ),
            balance = Money.ZERO,
        )

    private val cashAccount =
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

    private fun cardExpenseState() =
        TransactionEditUiState(
            isLoading = false,
            selectedType = TransactionType.EXPENSE,
            amountText = "50000",
            accountId = 2L,
            accounts = listOf(cashAccount, cardAccount),
            categoryId = 10L,
            selectedCategory = foodCategory,
            date = LocalDate.of(2026, 9, 28),
        )

    private fun setScreen(
        uiState: TransactionEditUiState,
        onInstallmentsChanged: (Int) -> Unit = {},
        onConfirmNegativeBalance: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onTypeSelected = {},
                    onAmountChanged = {},
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
                    onInstallmentsChanged = onInstallmentsChanged,
                    onSaveClick = {},
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
    fun `installments field is shown for card expenses with its hint`() {
        setScreen(cardExpenseState())

        // The installments field sits below the amount, category, account and date: scroll to it.
        composeRule.onNodeWithText("Cuotas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Solo informativo. No cambia el valor.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `installments field is hidden for cash accounts`() {
        setScreen(cardExpenseState().copy(accountId = 1L))

        composeRule.onNodeWithText("Cuotas").assertDoesNotExist()
    }

    @Test
    fun `installments field is hidden for income`() {
        setScreen(cardExpenseState().copy(selectedType = TransactionType.INCOME))

        composeRule.onNodeWithText("Cuotas").assertDoesNotExist()
    }

    @Test
    fun `installments field is hidden for transfers`() {
        setScreen(cardExpenseState().copy(selectedType = TransactionType.TRANSFER, toAccountId = 1L))

        composeRule.onNodeWithText("Cuotas").assertDoesNotExist()
    }

    @Test
    fun `typing installments notifies the callback`() {
        var installments: Int? = null
        setScreen(cardExpenseState(), onInstallmentsChanged = { installments = it })

        composeRule.onNodeWithText("1").performScrollTo().performTextReplacement("12")

        assertThat(installments).isEqualTo(12)
    }

    @Test
    fun `installments field can be cleared and rejects values out of range`() {
        val received = mutableListOf<Int>()
        setScreen(cardExpenseState(), onInstallmentsChanged = { received += it })
        val field = composeRule.onNodeWithText("Cuotas").performScrollTo()

        field.performTextClearance()
        field.performTextInput("3")
        field.performTextReplacement("60")

        assertThat(received).containsExactly(1, 3).inOrder()
        field.assert(hasText("3"))
    }

    @Test
    fun `over limit dialog shows the card texts`() {
        var confirmed = false
        setScreen(
            cardExpenseState().copy(
                showNegativeBalanceDialog = true,
                balanceWarning = BalanceWarning.OVER_CREDIT_LIMIT,
            ),
            onConfirmNegativeBalance = { confirmed = true },
        )

        composeRule.onNodeWithText("¿Superar el cupo?").assertIsDisplayed()
        composeRule
            .onNodeWithText("Con este movimiento, la deuda supera el cupo de la tarjeta.")
            .assertIsDisplayed()
        composeRule.onNode(hasText("Guardar") and hasAnyAncestor(isDialog())).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `negative balance dialog keeps the existing texts`() {
        setScreen(
            cardExpenseState().copy(
                showNegativeBalanceDialog = true,
                balanceWarning = BalanceWarning.NEGATIVE_BALANCE,
            ),
        )

        composeRule
            .onNodeWithText("El saldo no alcanza para este movimiento.")
            .assertIsDisplayed()
    }
}
