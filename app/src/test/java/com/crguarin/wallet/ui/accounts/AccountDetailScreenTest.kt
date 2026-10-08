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
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
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
        onRegisterTripClick: () -> Unit = {},
        onRechargeClick: () -> Unit = {},
        onConfirmTrip: () -> Unit = {},
        entries: List<TransactionListEntry> = emptyList(),
    ) {
        WalletTheme {
            AccountDetailScreen(
                uiState = uiState,
                pagingItems = rememberEntriesPagingItems(entries),
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
                onRegisterTripClick = onRegisterTripClick,
                onRechargeClick = onRechargeClick,
                onConfirmTrip = onConfirmTrip,
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

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Tarjeta de crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNodeWithText("Pagar tarjeta").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conciliar extracto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Compras a cuotas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Portátil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cuota 2 de 3", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Extractos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("cuadró", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ajuste de", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `card detail lists a purchase both as installment plan and as movement`() {
        // Transaction 11 is the installment plan; transaction 1 shares its id with statement 1.
        val entries =
            listOf(11L, 1L).map { id ->
                TransactionListEntry.Item(
                    TransactionDetails(
                        transaction =
                            Transaction(
                                id = id,
                                type = TransactionType.EXPENSE,
                                amount = Money(100_000L),
                                accountId = 7L,
                                date = LocalDate.of(2026, 9, 20),
                                note = "Movimiento $id",
                                createdAt = Instant.EPOCH,
                                updatedAt = Instant.EPOCH,
                            ),
                        accountName = "RappiCard",
                        accountColor = 0,
                        accountIcon = "credit_card",
                    ),
                )
            }
        composeRule.setContent {
            TestScreen(uiState = cardState(), entries = entries)
        }

        composeRule.onNodeWithText("Portátil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Movimiento 11").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Movimiento 1").performScrollTo().assertIsDisplayed()
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

    private val transitAccount =
        sampleAccount.copy(
            id = 9L,
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            initialBalance = Money(1_050_000L),
            icon = "directions_bus",
            fare = Money(350_000L),
        )

    // The header pushes the action row to the bottom edge of the default screen.
    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `transit card header shows the trips left and the trip and top up actions`() {
        composeRule.setContent {
            TestScreen(
                uiState =
                    baseState.copy(
                        accountWithBalance = AccountWithBalance(transitAccount, Money(1_050_000L)),
                        canTransfer = true,
                    ),
            )
        }

        composeRule.onNodeWithText("Alcanza para 3 viajes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Registrar viaje").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Recargar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `transit card that cannot pay one trip warns about the low balance`() {
        composeRule.setContent {
            TestScreen(
                uiState =
                    baseState.copy(
                        accountWithBalance = AccountWithBalance(transitAccount, Money(200_000L)),
                    ),
            )
        }

        composeRule.onNodeWithText("Saldo insuficiente para un viaje").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `archived transit card hides the trip actions`() {
        composeRule.setContent {
            TestScreen(
                uiState =
                    baseState.copy(
                        accountWithBalance =
                            AccountWithBalance(transitAccount.copy(isArchived = true), Money(1_050_000L)),
                        canTransfer = true,
                    ),
            )
        }

        composeRule.onNodeWithText("Registrar viaje").assertDoesNotExist()
        composeRule.onNodeWithText("Recargar").assertDoesNotExist()
    }

    // The header pushes the action row to the bottom edge of the default screen.
    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `trip and top up buttons call back`() {
        var tripClicked = false
        var rechargeClicked = false

        composeRule.setContent {
            TestScreen(
                uiState =
                    baseState.copy(
                        accountWithBalance = AccountWithBalance(transitAccount, Money(1_050_000L)),
                        canTransfer = true,
                    ),
                onRegisterTripClick = { tripClicked = true },
                onRechargeClick = { rechargeClicked = true },
            )
        }

        composeRule.onNodeWithText("Registrar viaje").performScrollTo().performClick()
        composeRule.onNodeWithText("Recargar").performScrollTo().performClick()

        assertThat(tripClicked).isTrue()
        assertThat(rechargeClicked).isTrue()
    }

    @Test
    fun `trip that would leave the card negative asks for confirmation`() {
        var confirmed = false

        composeRule.setContent {
            TestScreen(
                uiState =
                    baseState.copy(
                        accountWithBalance = AccountWithBalance(transitAccount, Money(200_000L)),
                        showTripNegativeDialog = true,
                    ),
                onConfirmTrip = { confirmed = true },
            )
        }

        composeRule
            .onNodeWithText("La tarjeta quedará en negativo. ¿Registrar el viaje de todas formas?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Registrar").performClick()

        assertThat(confirmed).isTrue()
    }
}
