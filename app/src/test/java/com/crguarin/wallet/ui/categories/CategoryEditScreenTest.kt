package com.crguarin.wallet.ui.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class CategoryEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val parentCategory =
        Category(
            id = 1L,
            name = "Transporte",
            type = CategoryType.EXPENSE,
            color = 0xFF42A5F5.toInt(),
            icon = "directions_bus",
        )

    @Test
    fun `save button is disabled when name is empty`() {
        composeRule.setContent {
            WalletTheme {
                CategoryEditScreen(
                    uiState =
                        CategoryEditUiState(
                            name = "",
                            isEditMode = false,
                        ),
                    onNavigateBack = {},
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

        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `creating subcategory displays correct title and parent and calls onSaveClick when valid`() {
        var saveClicked = false

        composeRule.setContent {
            WalletTheme {
                CategoryEditScreen(
                    uiState =
                        CategoryEditUiState(
                            name = "Taxi",
                            parentId = 1L,
                            parentOptions = listOf(parentCategory),
                            isEditMode = false,
                        ),
                    onNavigateBack = {},
                    onNameChange = {},
                    onTypeChange = {},
                    onParentIdChange = {},
                    onColorChange = {},
                    onIconChange = {},
                    onSaveClick = { saveClicked = true },
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

        composeRule.onNodeWithText("Nueva subcategoría").assertIsDisplayed()
        composeRule.onNodeWithText("Transporte").assertIsDisplayed()

        composeRule.onNodeWithText("Guardar").performScrollTo().performClick()
        assertThat(saveClicked).isTrue()
    }

    @Test
    fun `CategoryHasTransactions dialog displays movement count and both action options`() {
        var archiveClicked = false
        var reassignClicked = false

        composeRule.setContent {
            WalletTheme {
                CategoryEditScreen(
                    uiState =
                        CategoryEditUiState(
                            isEditMode = true,
                            categoryId = 1L,
                            name = "Transporte",
                            deleteDialogState = DeleteDialogState.HasTransactions(count = 5),
                        ),
                    onNavigateBack = {},
                    onNameChange = {},
                    onTypeChange = {},
                    onParentIdChange = {},
                    onColorChange = {},
                    onIconChange = {},
                    onSaveClick = {},
                    onArchiveClick = { archiveClicked = true },
                    onDeleteClick = {},
                    onDismissDeleteDialog = {},
                    onConfirmSimpleDelete = {},
                    onReassignOptionSelected = { reassignClicked = true },
                    onDismissReassignSheet = {},
                    onReassignAndConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText("Categoría con movimientos").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Esta categoría tiene 5 movimientos asociados. Puedes archivarla o reasignar sus movimientos a otra categoría para eliminarla.",
            ).assertIsDisplayed()

        composeRule.onNodeWithText("Archivar").performClick()
        assertThat(archiveClicked).isTrue()

        composeRule.onNodeWithText("Reasignar y eliminar").performClick()
        assertThat(reassignClicked).isTrue()
    }
}
