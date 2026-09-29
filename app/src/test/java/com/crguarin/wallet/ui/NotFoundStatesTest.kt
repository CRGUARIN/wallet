package com.crguarin.wallet.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.ui.accounts.AccountDetailScreen
import com.crguarin.wallet.ui.accounts.AccountDetailUiState
import com.crguarin.wallet.ui.accounts.AccountEditScreen
import com.crguarin.wallet.ui.accounts.AccountEditUiState
import com.crguarin.wallet.ui.categories.CategoriesScreen
import com.crguarin.wallet.ui.categories.CategoriesUiState
import com.crguarin.wallet.ui.categories.CategoryEditScreen
import com.crguarin.wallet.ui.categories.CategoryEditUiState
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.transactions.TransactionEditScreen
import com.crguarin.wallet.ui.transactions.TransactionEditUiState
import com.crguarin.wallet.ui.transactions.rememberEntriesPagingItems
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screens opened by id must explain a missing item and let the user leave, instead of showing an empty form. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class NotFoundStatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `transaction edit shows not found and goes back`() {
        var backs = 0
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState = TransactionEditUiState(isLoading = false, notFound = true),
                    onNavigateBack = { backs++ },
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
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }

        composeRule.onNodeWithText("Este movimiento ya no existe").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").assertDoesNotExist()
        composeRule.onNodeWithText("Atrás").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `transaction edit shows loading while the transaction is read`() {
        composeRule.setContent {
            WalletTheme {
                TransactionEditScreen(
                    uiState = TransactionEditUiState(isLoading = true),
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
                    onSaveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmDelete = {},
                    onConfirmNegativeBalance = {},
                    onDismissNegativeBalance = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
    }

    @Test
    fun `account edit shows not found and goes back`() {
        var backs = 0
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState = AccountEditUiState(isEditMode = true, notFound = true),
                    onNavigateBack = { backs++ },
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Elemento no encontrado").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").assertDoesNotExist()
        composeRule.onNodeWithText("Atrás").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `account edit shows loading while the account is read`() {
        composeRule.setContent {
            WalletTheme {
                AccountEditScreen(
                    uiState = AccountEditUiState(isLoading = true, isEditMode = true),
                    onNavigateBack = {},
                    onNameChanged = {},
                    onTypeChanged = {},
                    onInitialBalanceChanged = {},
                    onColorSelected = {},
                    onIconSelected = {},
                    onIncludeInTotalChanged = {},
                    onSaveClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
    }

    @Test
    fun `category edit shows not found and goes back`() {
        var backs = 0
        composeRule.setContent {
            WalletTheme {
                CategoryEditScreen(
                    uiState = CategoryEditUiState(notFound = true),
                    onNavigateBack = { backs++ },
                    onNameChange = {},
                    onTypeChange = {},
                    onParentIdChange = {},
                    onColorChange = {},
                    onIconChange = {},
                    onSaveClick = {},
                    onArchiveClick = {},
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmSimpleDelete = {},
                    onReassignOptionSelected = {},
                    onDismissReassignSheet = {},
                    onReassignAndConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText("Elemento no encontrado").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").assertDoesNotExist()
        composeRule.onNodeWithText("Atrás").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `account detail shows not found when the account no longer exists`() {
        var backs = 0
        composeRule.setContent {
            WalletTheme {
                AccountDetailScreen(
                    uiState = AccountDetailUiState(isLoading = false, accountWithBalance = null),
                    pagingItems = rememberEntriesPagingItems(emptyList()),
                    onNavigateBack = { backs++ },
                    onEditClick = {},
                    onTransferClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onArchiveClick = {},
                    onUnarchiveClick = {},
                    onDeleteClick = {},
                    onConfirmDelete = {},
                    onConfirmArchiveInsteadOfDelete = {},
                    onDismissDialogs = {},
                )
            }
        }

        composeRule.onNodeWithText("Esta cuenta ya no existe").assertIsDisplayed()
        composeRule.onNodeWithText("Últimos movimientos").assertDoesNotExist()
        composeRule.onNodeWithText("Atrás").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `categories shows loading instead of the empty state`() {
        composeRule.setContent {
            WalletTheme {
                CategoriesScreen(
                    uiState = CategoriesUiState(isLoading = true),
                    onNavigateBack = {},
                    onCategoryClick = { _, _ -> },
                    onNewCategoryClick = {},
                    onNewSubcategoryClick = { _, _ -> },
                    onSelectType = {},
                    onToggleArchivedExpanded = {},
                    onUnarchiveCategory = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
        composeRule.onNodeWithText("Sin categorías de gastos").assertDoesNotExist()
    }
}
