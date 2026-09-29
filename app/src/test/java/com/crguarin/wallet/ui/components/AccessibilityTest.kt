package com.crguarin.wallet.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO")
class AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `expense amount is read as expense and not as a minus sign`() {
        composeRule.setContent {
            WalletTheme { MoneyText(amount = Money(4_550_000L), type = TransactionType.EXPENSE) }
        }

        composeRule.onNodeWithText("− $ 45.500").assertContentDescriptionEquals("Gasto, $ 45.500")
    }

    @Test
    fun `income amount is read as income`() {
        composeRule.setContent {
            WalletTheme { MoneyText(amount = Money(4_550_000L), type = TransactionType.INCOME) }
        }

        composeRule.onNodeWithText("+ $ 45.500").assertContentDescriptionEquals("Ingreso, $ 45.500")
    }

    @Test
    fun `transfer amount is read as transfer`() {
        composeRule.setContent {
            WalletTheme { MoneyText(amount = Money(4_550_000L), type = TransactionType.TRANSFER) }
        }

        composeRule.onNodeWithText("$ 45.500").assertContentDescriptionEquals("Transferencia, $ 45.500")
    }

    @Test
    fun `plain amount without type is read as is`() {
        composeRule.setContent {
            WalletTheme { MoneyText(amount = Money(4_550_000L)) }
        }

        composeRule.onNodeWithText("$ 45.500").assertContentDescriptionEquals("$ 45.500")
    }

    @Test
    fun `day header label is marked as heading`() {
        val today = LocalDate.of(2026, 9, 28)
        composeRule.setContent {
            WalletTheme { TransactionDayHeader(date = today, net = Money(-50_000L), today = today) }
        }

        composeRule
            .onNode(hasText("Hoy") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    fun `day net is read with its sign spelled out`() {
        val today = LocalDate.of(2026, 9, 28)
        composeRule.setContent {
            WalletTheme { TransactionDayHeader(date = today, net = Money(-50_000L), today = today) }
        }

        composeRule.onNodeWithText("− $ 500").assertContentDescriptionEquals("Menos $ 500")
    }
}
