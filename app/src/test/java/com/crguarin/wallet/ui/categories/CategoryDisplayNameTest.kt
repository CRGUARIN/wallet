package com.crguarin.wallet.ui.categories

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.ui.components.displayName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryDisplayNameTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val foodCategory =
        Category(
            id = 1L,
            name = null,
            systemKey = "food",
            type = CategoryType.EXPENSE,
            color = 1,
            icon = "restaurant",
        )

    @Test
    @Config(qualifiers = "es")
    fun `system category displays Spanish name when locale is Spanish`() {
        composeRule.setContent {
            Text(text = foodCategory.displayName())
        }

        composeRule.onNodeWithText("Alimentación").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `system category displays English name when locale is English`() {
        composeRule.setContent {
            Text(text = foodCategory.displayName())
        }

        composeRule.onNodeWithText("Food").assertIsDisplayed()
    }
}
