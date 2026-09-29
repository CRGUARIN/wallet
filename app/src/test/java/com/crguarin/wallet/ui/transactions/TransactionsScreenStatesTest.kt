package com.crguarin.wallet.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class TransactionsScreenStatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

    private fun statesWith(refresh: LoadState) =
        LoadStates(
            refresh = refresh,
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )

    @Composable
    private fun Screen(
        uiState: TransactionsUiState = TransactionsUiState(isLoading = false, today = today, month = YearMonth.of(2026, 9)),
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        onClearFilters: () -> Unit = {},
    ) {
        val flow =
            remember(refresh) {
                MutableStateFlow(
                    PagingData.from<TransactionListEntry>(
                        data = emptyList(),
                        sourceLoadStates = statesWith(refresh),
                    ),
                )
            }
        WalletTheme {
            TransactionsScreen(
                uiState = uiState,
                pagingItems = flow.collectAsLazyPagingItems(),
                onPreviousMonth = {},
                onNextMonth = {},
                onTypeSelected = {},
                onOpenAccountSheet = {},
                onDismissAccountSheet = {},
                onAccountSelected = {},
                onOpenCategorySheet = {},
                onDismissCategorySheet = {},
                onCategorySelected = {},
                onClearFilters = onClearFilters,
                onSearchQueryChanged = {},
                onTransactionClick = {},
                onDeleteTransaction = {},
            )
        }
    }

    @Test
    fun `shows loading indicator while the first page loads`() {
        composeRule.setContent {
            Screen(
                uiState = TransactionsUiState(isLoading = true, today = today),
                refresh = LoadState.Loading,
            )
        }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
        composeRule.onNodeWithText("Sin movimientos").assertDoesNotExist()
    }

    @Test
    fun `empty month without filters shows the plain empty state`() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithText("Sin movimientos").assertIsDisplayed()
        composeRule.onNodeWithText("Tus movimientos aparecerán aquí.").assertIsDisplayed()
        composeRule.onNodeWithText("Sin resultados").assertDoesNotExist()
    }

    @Test
    fun `empty result with filters offers to clear them`() {
        var cleared = 0
        composeRule.setContent {
            Screen(
                uiState = TransactionsUiState(isLoading = false, today = today, type = TransactionType.EXPENSE),
                onClearFilters = { cleared++ },
            )
        }

        composeRule.onNodeWithText("Sin resultados").assertIsDisplayed()
        composeRule.onNodeWithText("No hay movimientos con estos filtros.").assertIsDisplayed()
        // Both the chip row and the empty state have a "clear filters" button; either one clears.
        composeRule.onAllNodesWithText("Limpiar filtros").onFirst().performClick()

        assertThat(cleared).isEqualTo(1)
    }

    @Test
    fun `failed first page shows an error with a retry button`() {
        composeRule.setContent {
            Screen(refresh = LoadState.Error(IllegalStateException("boom")))
        }

        composeRule.onNodeWithText("No se pudieron cargar los movimientos").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
        composeRule.onNodeWithText("Sin movimientos").assertDoesNotExist()
    }

    @Test
    fun `month title is marked as heading`() {
        composeRule.setContent { Screen() }

        composeRule
            .onNode(hasText("Septiembre 2026") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }
}
