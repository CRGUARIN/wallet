package com.crguarin.wallet.ui.accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.theme.WalletTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class CardSummaryCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

    private fun show(overview: CardOverview = sampleCardOverview()) {
        composeRule.setContent {
            WalletTheme {
                CardSummaryCard(
                    overview = overview,
                    today = today,
                )
            }
        }
    }

    @Test
    fun `debt summary shows debt usage available closing countdown pending and estimate`() {
        show()

        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNodeWithText("usados", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Corte en 17 días").assertIsDisplayed()
        composeRule.onNodeWithText("Por pagar del extracto").assertIsDisplayed()
        composeRule.onNodeWithText("Pagar antes del", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cuotas del próximo extracto").assertIsDisplayed()
        composeRule.onNodeWithText("Estimado, sin intereses").assertIsDisplayed()
    }

    @Test
    fun `closing today shows the today label`() {
        show(sampleCardOverview().copy(nextClosingDate = today))

        composeRule.onNodeWithText("Corte hoy").assertIsDisplayed()
    }

    @Test
    fun `paid statement shows the paid label`() {
        show(
            sampleCardOverview().copy(
                paymentStatus = StatementPaymentStatus.PAID,
                pendingStatementAmount = Money.ZERO,
            ),
        )

        composeRule.onNodeWithText("Extracto pagado").assertIsDisplayed()
    }

    @Test
    fun `overdue statement shows the overdue label`() {
        show(sampleCardOverview().copy(paymentStatus = StatementPaymentStatus.OVERDUE))

        composeRule.onNodeWithText("Pago vencido").assertIsDisplayed()
    }

    @Test
    fun `credit in favor shows the credit balance label`() {
        show(sampleCardOverview().copy(debt = Money(-50_000_00L)))

        composeRule.onNodeWithText("Saldo a favor").assertIsDisplayed()
    }
}
