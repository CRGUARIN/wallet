package com.crguarin.wallet.ui.accounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.transactions.rememberEntriesPagingItems
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
class AccountDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sampleAccount =
        Account(
            id = 42L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(150_000L),
            color = 0xFF4CAF50.toInt(),
            icon = "wallet",
            sortOrder = 0,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val baseState =
        AccountDetailUiState(
            isLoading = false,
            accountWithBalance = AccountWithBalance(sampleAccount, Money(450_000L)),
            monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L)),
        )

    @Composable
    private fun TestScreen(
        uiState: AccountDetailUiState = baseState,
        onEditClick: () -> Unit = {},
        onTransferClick: () -> Unit = {},
        onConfirmDelete: () -> Unit = {},
        onConfirmArchiveInsteadOfDelete: () -> Unit = {},
        onDismissDialogs: () -> Unit = {},
        onTransactionClick: (Long) -> Unit = {},
        onPayCardClick: () -> Unit = {},
        onReconcileClick: () -> Unit = {},
    ) {
        WalletTheme {
            AccountDetailScreen(
                uiState = uiState,
                pagingItems = rememberEntriesPagingItems(emptyList()),
                onNavigateBack = {},
                onEditClick = onEditClick,
                onTransferClick = onTransferClick,
                onTransactionClick = onTransactionClick,
                onViewAllTransactions = {},
                onArchiveClick = {},
                onUnarchiveClick = {},
                onDeleteClick = {},
                onConfirmDelete = onConfirmDelete,
                onConfirmArchiveInsteadOfDelete = onConfirmArchiveInsteadOfDelete,
                onDismissDialogs = onDismissDialogs,
                onPayCardClick = onPayCardClick,
                onReconcileClick = onReconcileClick,
            )
        }
    }

    private val cardAccount =
        sampleAccount.copy(
            id = 7L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-250_000_00L),
            icon = "credit_card",
        )

    private fun cardState(): AccountDetailUiState =
        AccountDetailUiState(
            isLoading = false,
            accountWithBalance = AccountWithBalance(cardAccount, Money(-250_000_00L)),
            today = LocalDate.of(2026, 9, 28),
            cardOverview = sampleCardOverview(accountId = 7L),
            statements =
                listOf(
                    sampleCardStatement(id = 1L, accountId = 7L),
                    sampleCardStatement(
                        id = 2L,
                        accountId = 7L,
                        closingDate = LocalDate.of(2026, 8, 15),
                        adjustmentTransactionId = 99L,
                    ),
                ),
            adjustmentAmounts = mapOf(2L to Money(5_000_00L)),
        )

    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `card detail shows summary actions installments and statements`() {
        composeRule.setContent {
            TestScreen(uiState = cardState())
        }

        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNodeWithText("Pagar tarjeta").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conciliar extracto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Compras a cuotas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Portátil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cuota 1 de 3", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Extractos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("cuadró", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ajuste de", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `card actions and installment tap trigger callbacks`() {
        var payClicks = 0
        var reconcileClicks = 0
        var transactionClick: Long? = null
        composeRule.setContent {
            TestScreen(
                uiState = cardState(),
                onPayCardClick = { payClicks++ },
                onReconcileClick = { reconcileClicks++ },
                onTransactionClick = { transactionClick = it },
            )
        }

        composeRule.onNodeWithText("Pagar tarjeta").performScrollTo().performClick()
        composeRule.onNodeWithText("Conciliar extracto").performScrollTo().performClick()
        composeRule.onNodeWithText("Portátil").performScrollTo().performClick()

        assertThat(payClicks).isEqualTo(1)
        assertThat(reconcileClicks).isEqualTo(1)
        assertThat(transactionClick).isEqualTo(11L)
    }

    @Test
    fun `displays account information and month summary`() {
        composeRule.setContent {
            TestScreen()
        }

        composeRule.onAllNodesWithText("Efectivo").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Resumen del mes").assertIsDisplayed()
    }

    @Test
    fun `shows delete confirmation dialog and clicking delete calls onConfirmDelete`() {
        var confirmDeleteClicked = false

        composeRule.setContent {
            TestScreen(
                uiState = baseState.copy(showDeleteConfirmDialog = true),
                onConfirmDelete = { confirmDeleteClicked = true },
            )
        }

        composeRule.onNodeWithText("Eliminar cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Eliminar").performClick()
        assertThat(confirmDeleteClicked).isTrue()
    }

    @Test
    fun `shows offer archive dialog when deleting fails and clicking archive calls onConfirmArchive`() {
        var confirmArchiveClicked = false

        composeRule.setContent {
            TestScreen(
                uiState = baseState.copy(showOfferArchiveDialog = true),
                onConfirmArchiveInsteadOfDelete = { confirmArchiveClicked = true },
            )
        }

        // Title explaining that account cannot be deleted
        composeRule.onNodeWithText("No se puede eliminar").assertIsDisplayed()

        // Archive action is offered
        composeRule.onNodeWithText("Archivar").performClick()
        assertThat(confirmArchiveClicked).isTrue()
    }

    @Test
    fun `overflow menu options trigger corresponding callbacks`() {
        var editClicked = false

        composeRule.setContent {
            TestScreen(onEditClick = { editClicked = true })
        }

        // Open overflow menu
        composeRule.onNodeWithContentDescription("Más opciones").performClick()

        // Click Edit
        composeRule.onNodeWithText("Editar").performClick()
        assertThat(editClicked).isTrue()
    }

    @Test
    fun `transfer option is shown and clickable when another active account exists`() {
        var transferClicked = false

        composeRule.setContent {
            TestScreen(
                uiState = baseState.copy(canTransfer = true),
                onTransferClick = { transferClicked = true },
            )
        }

        composeRule.onNodeWithContentDescription("Más opciones").performClick()
        composeRule.onNodeWithText("Transferir desde esta cuenta").performClick()

        assertThat(transferClicked).isTrue()
    }

    @Test
    fun `transfer option is hidden when there is no other active account`() {
        composeRule.setContent {
            TestScreen()
        }

        composeRule.onNodeWithContentDescription("Más opciones").performClick()

        composeRule.onNodeWithText("Transferir desde esta cuenta").assertDoesNotExist()
    }
}
