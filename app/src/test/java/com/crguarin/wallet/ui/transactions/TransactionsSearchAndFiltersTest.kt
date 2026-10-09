package com.crguarin.wallet.ui.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The search behind the magnifier and the single-row filters of the movements list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class TransactionsSearchAndFiltersTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

    /** Hosts the query the way the ViewModel would, so typing and clearing are visible in the UI. */
    @Composable
    private fun Screen(
        initialQuery: String = "",
        initial: TransactionsUiState = TransactionsUiState(isLoading = false, today = today),
        onQueryChanged: (String) -> Unit = {},
        onTypeSelected: (TransactionType?) -> Unit = {},
        onClearFilters: () -> Unit = {},
    ) {
        var query by remember { mutableStateOf(initialQuery) }
        WalletTheme {
            TransactionsScreen(
                uiState = initial.copy(searchQuery = query),
                pagingItems = rememberEntriesPagingItems(emptyList()),
                onPreviousMonth = {},
                onNextMonth = {},
                onTypeSelected = onTypeSelected,
                onOpenAccountSheet = {},
                onDismissAccountSheet = {},
                onAccountSelected = {},
                onOpenCategorySheet = {},
                onDismissCategorySheet = {},
                onCategorySelected = {},
                onClearFilters = onClearFilters,
                onSearchQueryChanged = {
                    query = it
                    onQueryChanged(it)
                },
                onTransactionClick = {},
                onDeleteTransaction = {},
            )
        }
    }

    @Test
    fun `the search field takes no room until the magnifier is tapped`() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithContentDescription("Buscar").assertIsDisplayed()
        composeRule.onNodeWithText("Buscar por nota").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Cerrar búsqueda").assertDoesNotExist()
    }

    @Test
    fun `tapping the magnifier shows the search field with its close button`() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithContentDescription("Buscar").performClick()

        composeRule.onNodeWithText("Buscar por nota").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cerrar búsqueda").assertIsDisplayed()
    }

    @Test
    fun `tapping the magnifier again hides the search`() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithContentDescription("Buscar").performClick()
        composeRule.onNodeWithText("Buscar por nota").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Buscar").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Buscar por nota").assertDoesNotExist()
    }

    @Test
    fun `typing in the search field reports the query`() {
        val queries = mutableListOf<String>()
        composeRule.setContent { Screen(onQueryChanged = { queries += it }) }

        composeRule.onNodeWithContentDescription("Buscar").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("mercado")

        assertThat(queries.last()).isEqualTo("mercado")
    }

    @Test
    fun `closing the search clears the query and hides the field`() {
        val queries = mutableListOf<String>()
        composeRule.setContent { Screen(onQueryChanged = { queries += it }) }

        composeRule.onNodeWithContentDescription("Buscar").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("mercado")
        composeRule.onNodeWithContentDescription("Cerrar búsqueda").performClick()

        composeRule.waitForIdle()
        assertThat(queries.last()).isEmpty()
        composeRule.onNodeWithText("Buscar por nota").assertDoesNotExist()
        composeRule.onNodeWithText("mercado").assertDoesNotExist()
    }

    @Test
    fun `closing the search with the magnifier also clears the query`() {
        val queries = mutableListOf<String>()
        composeRule.setContent { Screen(onQueryChanged = { queries += it }) }

        composeRule.onNodeWithContentDescription("Buscar").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("pan")
        composeRule.onNodeWithContentDescription("Buscar").performClick()

        composeRule.waitForIdle()
        assertThat(queries.last()).isEmpty()
        composeRule.onNodeWithContentDescription("Cerrar búsqueda").assertDoesNotExist()
    }

    @Test
    fun `an active query keeps the search field visible`() {
        composeRule.setContent { Screen(initialQuery = "mercado") }

        composeRule.onNodeWithText("mercado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cerrar búsqueda").assertIsDisplayed()
    }

    @Test
    fun `filters sit in one row with expense before income`() {
        composeRule.setContent { Screen() }

        val labels = listOf("Todos", "Gasto", "Ingreso", "Transferencia", "Cuenta", "Categoría")
        val bounds = labels.map { composeRule.onNode(hasText(it) and hasClickAction()).fetchSemanticsNode().boundsInRoot }

        assertThat(bounds.map { it.left }).isInStrictOrder()
        // One row: every chip overlaps the first one vertically.
        bounds.forEach { chip ->
            assertThat(chip.top).isLessThan(bounds.first().bottom)
            assertThat(chip.bottom).isGreaterThan(bounds.first().top)
        }
    }

    @Test
    fun `the type chips report the chosen type`() {
        val types = mutableListOf<TransactionType?>()
        composeRule.setContent { Screen(onTypeSelected = { types += it }) }

        composeRule.onNode(hasText("Gasto") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Ingreso") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Transferencia") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Todos") and hasClickAction()).performClick()

        assertThat(types)
            .containsExactly(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER, null)
            .inOrder()
    }

    @Test
    fun `the selected type chip is marked as selected`() {
        composeRule.setContent { Screen(initial = TransactionsUiState(isLoading = false, today = today, type = TransactionType.EXPENSE)) }

        composeRule.onNode(hasText("Gasto") and hasClickAction()).assertIsSelected()
        composeRule.onNode(hasText("Ingreso") and hasClickAction()).assertIsNotSelected()
        composeRule.onNode(hasText("Todos") and hasClickAction()).assertIsNotSelected()
    }

    @Test
    fun `clear filters appears in the filter row while a filter is on`() {
        var cleared = 0
        composeRule.setContent {
            Screen(
                initial = TransactionsUiState(isLoading = false, today = today, type = TransactionType.INCOME),
                onClearFilters = { cleared++ },
            )
        }

        // The empty state offers the same action, so take the first one: the one in the chip row.
        composeRule.onAllNodes(hasText("Limpiar filtros") and hasClickAction()).onFirst().performClick()

        assertThat(cleared).isEqualTo(1)
    }

    @Test
    fun `clear filters is not offered without filters`() {
        composeRule.setContent { Screen() }

        composeRule.onNodeWithText("Limpiar filtros").assertDoesNotExist()
    }
}
