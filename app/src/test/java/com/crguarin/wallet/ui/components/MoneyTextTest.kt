package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MoneyTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val esLocale = Locale.forLanguageTag("es-CO")
    private val enLocale = Locale.forLanguageTag("en-US")

    @Test
    @Config(qualifiers = "es")
    fun `formats income expense and transfer in Spanish`() {
        composeRule.setContent {
            WalletTheme {
                Column {
                    MoneyText(
                        amount = Money(5_000_000L),
                        type = TransactionType.INCOME,
                        locale = esLocale,
                    )
                    MoneyText(
                        amount = Money(2_000_000L),
                        type = TransactionType.EXPENSE,
                        locale = esLocale,
                    )
                    MoneyText(
                        amount = Money(3_000_000L),
                        type = TransactionType.TRANSFER,
                        locale = esLocale,
                    )
                }
            }
        }
        composeRule.onNodeWithText("+ $ 50.000").assertIsDisplayed()
        composeRule.onNodeWithText("− $ 20.000").assertIsDisplayed()
        composeRule.onNodeWithText("$ 30.000").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `formats income expense and transfer in English`() {
        composeRule.setContent {
            WalletTheme {
                Column {
                    MoneyText(
                        amount = Money(5_000_000L),
                        type = TransactionType.INCOME,
                        locale = enLocale,
                    )
                    MoneyText(
                        amount = Money(2_000_000L),
                        type = TransactionType.EXPENSE,
                        locale = enLocale,
                    )
                    MoneyText(
                        amount = Money(3_000_000L),
                        type = TransactionType.TRANSFER,
                        locale = enLocale,
                    )
                }
            }
        }
        composeRule.onNodeWithText("+ $50,000").assertIsDisplayed()
        composeRule.onNodeWithText("− $20,000").assertIsDisplayed()
        composeRule.onNodeWithText("$30,000").assertIsDisplayed()
    }
}
