package com.crguarin.wallet.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransactionListItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun details(
        type: TransactionType,
        accountId: Long = 1L,
        toAccountId: Long? = null,
        categorySystemKey: String? = null,
        parentCategorySystemKey: String? = null,
    ) = TransactionDetails(
        transaction =
            Transaction(
                id = 1L,
                type = type,
                amount = Money(4_550_000L),
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = if (type == TransactionType.TRANSFER) null else 10L,
                date = LocalDate.of(2026, 9, 28),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        toAccountName = if (type == TransactionType.TRANSFER) "Ahorros" else null,
        toAccountColor = if (type == TransactionType.TRANSFER) 0xFF42A5F5.toInt() else null,
        toAccountIcon = if (type == TransactionType.TRANSFER) "savings" else null,
        categoryName = null,
        categorySystemKey = categorySystemKey,
        categoryColor = if (type == TransactionType.TRANSFER) null else 0xFFEF5350.toInt(),
        categoryIcon = if (type == TransactionType.TRANSFER) null else "restaurant",
        categoryParentId = if (parentCategorySystemKey != null) 1L else null,
        parentCategoryName = null,
        parentCategorySystemKey = parentCategorySystemKey,
    )

    @Test
    @Config(qualifiers = "es-rCO")
    fun `expense is shown in red with a minus sign`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.EXPENSE, categorySystemKey = "groceries", parentCategorySystemKey = "food"),
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Alimentación › Mercado").assertIsDisplayed()
        composeRule.onNodeWithText("− $ 45.500").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-rCO")
    fun `income is shown with a plus sign`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.INCOME, categorySystemKey = "salary"),
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("+ $ 45.500").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-rCO")
    fun `transfer subtitle shows both accounts and no sign without perspective`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.TRANSFER, accountId = 1L, toAccountId = 2L),
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Transferencia").assertIsDisplayed()
        composeRule.onNodeWithText("Efectivo → Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("$ 45.500").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-rCO")
    fun `transfer is negative from the source account perspective`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.TRANSFER, accountId = 1L, toAccountId = 2L),
                    onClick = {},
                    perspectiveAccountId = 1L,
                )
            }
        }

        composeRule.onNodeWithText("− $ 45.500").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-rCO")
    fun `transfer is positive from the destination account perspective`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.TRANSFER, accountId = 1L, toAccountId = 2L),
                    onClick = {},
                    perspectiveAccountId = 2L,
                )
            }
        }

        composeRule.onNodeWithText("+ $ 45.500").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `system categories are translated to English`() {
        composeRule.setContent {
            WalletTheme {
                TransactionListItem(
                    details = details(TransactionType.EXPENSE, categorySystemKey = "groceries", parentCategorySystemKey = "food"),
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Food › Groceries").assertIsDisplayed()
    }
}
