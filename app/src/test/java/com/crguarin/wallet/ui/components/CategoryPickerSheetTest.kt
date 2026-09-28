package com.crguarin.wallet.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class CategoryPickerSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `expanding root category reveals and allows selecting subcategory`() {
        var selectedCategory: Category? = null

        val subcategory =
            Category(
                id = 11,
                name = null,
                systemKey = "groceries",
                type = CategoryType.EXPENSE,
                parentId = 10,
                color = 0xFFEF5350.toInt(),
                icon = "local_grocery_store",
            )

        val rootCategory =
            Category(
                id = 10,
                name = null,
                systemKey = "food",
                type = CategoryType.EXPENSE,
                parentId = null,
                color = 0xFFEF5350.toInt(),
                icon = "restaurant",
            )

        val categories =
            listOf(
                CategoryNode(
                    category = rootCategory,
                    children = listOf(CategoryNode(category = subcategory)),
                ),
            )

        composeRule.setContent {
            WalletTheme {
                CategoryPickerSheet(
                    categories = categories,
                    onCategorySelected = { selectedCategory = it },
                    onNewCategoryClick = {},
                    onDismissRequest = {},
                )
            }
        }

        // Subcategory "Mercado" should not be visible before expanding
        composeRule.onNodeWithText("Mercado").assertDoesNotExist()

        // Click expand button
        composeRule.onNodeWithContentDescription("Expandir").performClick()

        // Now subcategory "Mercado" is displayed
        composeRule.onNodeWithText("Mercado").assertIsDisplayed()

        // Click subcategory
        composeRule.onNodeWithText("Mercado").performClick()

        // Callback receives the correct subcategory id
        assertThat(selectedCategory).isNotNull()
        assertThat(selectedCategory?.id).isEqualTo(11L)
        assertThat(selectedCategory?.systemKey).isEqualTo("groceries")
    }
}
