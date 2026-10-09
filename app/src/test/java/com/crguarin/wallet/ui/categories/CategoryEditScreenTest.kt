package com.crguarin.wallet.ui.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeRule.onNodeWithText("Guardar").assertIsNotEnabled()
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

        composeRule.onNodeWithText("Guardar").performClick()
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

        composeRule.onNodeWithText("Categoría en uso").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Tiene 5 movimientos. Puedes archivarla, o pasar sus movimientos a otra categoría y eliminarla.",
            ).assertIsDisplayed()

        composeRule.onNodeWithText("Archivar").performClick()
        assertThat(archiveClicked).isTrue()

        composeRule.onNodeWithText("Reasignar y eliminar").performClick()
        assertThat(reassignClicked).isTrue()
    }

    private fun setEditor(
        uiState: CategoryEditUiState,
        onNavigateBack: () -> Unit = {},
        onTypeChange: (CategoryType) -> Unit = {},
        onSaveClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                CategoryEditScreen(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onNameChange = {},
                    onTypeChange = onTypeChange,
                    onParentIdChange = {},
                    onColorChange = {},
                    onIconChange = {},
                    onSaveClick = onSaveClick,
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
    }

    @Test
    fun `cancel and save share the bottom bar and cancel goes back`() {
        var backClicks = 0
        var saveClicks = 0
        setEditor(
            CategoryEditUiState(name = "Mascotas"),
            onNavigateBack = { backClicks++ },
            onSaveClick = { saveClicks++ },
        )

        composeRule.onNodeWithText("Guardar").assertIsEnabled()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed().performClick()
        assertThat(backClicks).isEqualTo(1)
        assertThat(saveClicks).isEqualTo(0)
    }

    @Test
    fun `type segments show the current type and report the change`() {
        var chosen: CategoryType? = null
        setEditor(CategoryEditUiState(name = "Mascotas", type = CategoryType.EXPENSE), onTypeChange = { chosen = it })

        composeRule.onNodeWithText("Gasto").assertIsSelected()
        composeRule.onNodeWithText("Ingreso").performClick()

        assertThat(chosen).isEqualTo(CategoryType.INCOME)
    }

    @Test
    fun `a locked type cannot be changed`() {
        var chosen: CategoryType? = null
        setEditor(
            CategoryEditUiState(name = "Taxi", type = CategoryType.EXPENSE, isTypeLocked = true),
            onTypeChange = { chosen = it },
        )

        composeRule.onNodeWithText("Ingreso").assertIsNotEnabled()
        composeRule.onNodeWithText("Ingreso").performClick()

        assertThat(chosen).isNull()
    }
}
