package com.crguarin.wallet.ui.transactions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class TransactionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

    private fun details(
        id: Long,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: Long = 1L,
        toAccountId: Long? = null,
        categorySystemKey: String? = "groceries",
        parentCategorySystemKey: String? = "food",
        note: String? = null,
    ) = TransactionDetails(
        transaction =
            Transaction(
                id = id,
                type = type,
                amount = Money(4_550_000L),
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = if (type == TransactionType.TRANSFER) null else 10L,
                date = today,
                note = note,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        toAccountName = if (type == TransactionType.TRANSFER) "Ahorros" else null,
        toAccountColor = if (type == TransactionType.TRANSFER) 0xFF42A5F5.toInt() else null,
        toAccountIcon = if (type == TransactionType.TRANSFER) "savings" else null,
        categorySystemKey = categorySystemKey,
        categoryColor = if (type == TransactionType.TRANSFER) null else 0xFFEF5350.toInt(),
        categoryIcon = if (type == TransactionType.TRANSFER) null else "restaurant",
        categoryParentId = if (parentCategorySystemKey != null) 1L else null,
        parentCategorySystemKey = parentCategorySystemKey,
    )

    private val entries =
        listOf(
            TransactionListEntry.DayHeader(today),
            TransactionListEntry.Item(details(id = 1L, note = "Mercado semanal")),
            TransactionListEntry.DayHeader(today.minusDays(1)),
            TransactionListEntry.Item(details(id = 2L, type = TransactionType.TRANSFER, accountId = 1L, toAccountId = 2L)),
        )

    @Composable
    private fun Screen(
        uiState: TransactionsUiState =
            TransactionsUiState(
                isLoading = false,
                today = today,
                dailyTotals = mapOf(today to Money(100_000L), today.minusDays(1) to Money(-50_000L)),
            ),
        list: List<TransactionListEntry> = entries,
        onDeleteTransaction: (TransactionDetails) -> Unit = {},
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    ) {
        WalletTheme {
            TransactionsScreen(
                uiState = uiState,
                pagingItems = rememberEntriesPagingItems(list),
                onPreviousMonth = {},
                onNextMonth = {},
                onTypeSelected = {},
                onOpenAccountSheet = {},
                onDismissAccountSheet = {},
                onAccountSelected = {},
                onOpenCategorySheet = {},
                onDismissCategorySheet = {},
                onCategorySelected = {},
                onClearFilters = {},
                onSearchQueryChanged = {},
                onTransactionClick = {},
                onDeleteTransaction = onDeleteTransaction,
                snackbarHostState = snackbarHostState,
            )
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `shows day headers with relative labels and the day net`() {
        composeRule.setContent { Screen() }

        waitForText("Hoy")
        composeRule.onNodeWithText("Hoy").assertIsDisplayed()
        composeRule.onNodeWithText("+ $ 1.000").assertIsDisplayed()
        composeRule.onNodeWithText("Alimentación › Mercado").assertIsDisplayed()
        composeRule.onNodeWithText("Ayer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("− $ 500").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `filtering by account signs transfers from that account's perspective`() {
        composeRule.setContent {
            Screen(
                uiState = TransactionsUiState(isLoading = false, today = today, accountId = 1L),
                list =
                    listOf(
                        TransactionListEntry.Item(details(id = 2L, type = TransactionType.TRANSFER, accountId = 1L, toAccountId = 2L)),
                    ),
            )
        }

        waitForText("− $ 45.500")
        composeRule.onNodeWithText("− $ 45.500").assertIsDisplayed()
    }

    @Test
    fun `shows the filtered empty state when there are no results`() {
        composeRule.setContent {
            Screen(
                uiState =
                    TransactionsUiState(
                        isLoading = false,
                        today = today,
                        type = TransactionType.EXPENSE,
                    ),
                list = emptyList(),
            )
        }

        waitForText("Sin resultados")
        composeRule.onNodeWithText("Sin resultados").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("No hay movimientos con estos filtros.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `swiping a transaction left triggers the delete callback`() {
        var deletedId: Long? = null
        composeRule.setContent {
            Screen(onDeleteTransaction = { deletedId = it.transaction.id })
        }

        waitForText("Alimentación › Mercado")
        composeRule.onNodeWithText("Alimentación › Mercado").performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 5_000) { deletedId == 1L }
    }

    @Test
    fun `deleting shows a snackbar with undo that triggers the undo callback`() {
        var undoCount = 0
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val deletedMessage = stringResource(R.string.transactions_deleted_message)
            val undoLabel = stringResource(R.string.action_undo)
            val scope = rememberCoroutineScope()
            Screen(
                onDeleteTransaction = {
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = deletedMessage,
                                actionLabel = undoLabel,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            undoCount++
                        }
                    }
                },
                snackbarHostState = snackbarHostState,
            )
        }

        waitForText("Alimentación › Mercado")
        composeRule.onNodeWithText("Alimentación › Mercado").performTouchInput { swipeLeft() }

        waitForText("Deshacer")
        composeRule.onNodeWithText("Movimiento eliminado").assertIsDisplayed()
        composeRule.onNodeWithText("Deshacer").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { undoCount == 1 }
    }
}
