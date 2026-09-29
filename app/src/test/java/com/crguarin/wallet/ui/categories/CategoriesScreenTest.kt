package com.crguarin.wallet.ui.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
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
class CategoriesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rootCategory =
        Category(
            id = 1L,
            name = "Transporte",
            type = CategoryType.EXPENSE,
            color = 0xFF42A5F5.toInt(),
            icon = "directions_bus",
        )
    private val subcategory =
        Category(
            id = 2L,
            name = "Taxi",
            type = CategoryType.EXPENSE,
            parentId = 1L,
            color = 0xFF42A5F5.toInt(),
            icon = "local_taxi",
        )

    @Test
    fun `displays category tree with root and subcategory`() {
        composeRule.setContent {
            WalletTheme {
                CategoriesScreen(
                    uiState =
                        CategoriesUiState(
                            selectedType = CategoryType.EXPENSE,
                            activeTree =
                                listOf(
                                    CategoryNode(
                                        category = rootCategory,
                                        children = listOf(CategoryNode(subcategory)),
                                    ),
                                ),
                        ),
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

        composeRule.onNodeWithText("Transporte").assertIsDisplayed()
        composeRule.onNodeWithText("Taxi").assertIsDisplayed()
    }

    @Test
    fun `tapping subcategory button calls onNewSubcategoryClick with root id`() {
        var clickedParentId: Long? = null
        var clickedType: CategoryType? = null

        composeRule.setContent {
            WalletTheme {
                CategoriesScreen(
                    uiState =
                        CategoriesUiState(
                            selectedType = CategoryType.EXPENSE,
                            activeTree =
                                listOf(
                                    CategoryNode(
                                        category = rootCategory,
                                        children = emptyList(),
                                    ),
                                ),
                        ),
                    onNavigateBack = {},
                    onCategoryClick = { _, _ -> },
                    onNewCategoryClick = {},
                    onNewSubcategoryClick = { parentId, type ->
                        clickedParentId = parentId
                        clickedType = type
                    },
                    onSelectType = {},
                    onToggleArchivedExpanded = {},
                    onUnarchiveCategory = {},
                )
            }
        }

        composeRule.onNodeWithText("Subcategoría").performClick()
        assertThat(clickedParentId).isEqualTo(1L)
        assertThat(clickedType).isEqualTo(CategoryType.EXPENSE)
    }

    @Test
    fun `tapping category row calls onCategoryClick`() {
        var clickedCategoryId: Long? = null
        var clickedType: CategoryType? = null

        composeRule.setContent {
            WalletTheme {
                CategoriesScreen(
                    uiState =
                        CategoriesUiState(
                            selectedType = CategoryType.EXPENSE,
                            activeTree =
                                listOf(
                                    CategoryNode(
                                        category = rootCategory,
                                        children = emptyList(),
                                    ),
                                ),
                        ),
                    onNavigateBack = {},
                    onCategoryClick = { id, type ->
                        clickedCategoryId = id
                        clickedType = type
                    },
                    onNewCategoryClick = {},
                    onNewSubcategoryClick = { _, _ -> },
                    onSelectType = {},
                    onToggleArchivedExpanded = {},
                    onUnarchiveCategory = {},
                )
            }
        }

        composeRule.onNodeWithText("Transporte").performClick()
        assertThat(clickedCategoryId).isEqualTo(1L)
        assertThat(clickedType).isEqualTo(CategoryType.EXPENSE)
    }

    @Test
    fun `tapping FAB calls onNewCategoryClick`() {
        var clickedType: CategoryType? = null

        composeRule.setContent {
            WalletTheme {
                CategoriesScreen(
                    uiState = CategoriesUiState(selectedType = CategoryType.INCOME),
                    onNavigateBack = {},
                    onCategoryClick = { _, _ -> },
                    onNewCategoryClick = { type -> clickedType = type },
                    onNewSubcategoryClick = { _, _ -> },
                    onSelectType = {},
                    onToggleArchivedExpanded = {},
                    onUnarchiveCategory = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Nueva categoría").performClick()
        assertThat(clickedType).isEqualTo(CategoryType.INCOME)
    }
}
