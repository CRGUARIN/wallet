package com.crguarin.wallet.ui.accounts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AccountListRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 28)

    private fun account(
        name: String,
        type: AccountType,
        fare: Money? = null,
        includeInTotal: Boolean = true,
    ) = Account(
        id = 1L,
        name = name,
        type = type,
        initialBalance = Money.ZERO,
        color = 0xFF42A5F5.toInt(),
        icon = "wallet",
        sortOrder = 0,
        fare = fare,
        includeInTotal = includeInTotal,
        createdAt = Instant.EPOCH,
    )

    private fun show(
        item: AccountWithBalance,
        cardOverview: CardOverview? = null,
        showTypeLabel: Boolean = false,
        trailing: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                AccountListRow(
                    item = item,
                    onClick = onClick,
                    today = today,
                    cardOverview = cardOverview,
                    showTypeLabel = showTypeLabel,
                    enabled = enabled,
                    trailing = trailing,
                )
            }
        }
    }

    @Test
    fun `card with a payment due within a week shows the payment day and neither debt nor credit`() {
        val card = account("RappiCard", AccountType.CREDIT_CARD)
        show(
            AccountWithBalance(card, Money(-250_000_00L)),
            cardOverview = sampleCardOverview(accountId = 1L, today = today),
        )

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Pagar en 7 días").assertIsDisplayed()
        composeRule.onNodeWithText("Deuda").assertDoesNotExist()
        composeRule.onNodeWithText("Disponible", substring = true).assertDoesNotExist()
    }

    @Test
    fun `card due today or tomorrow says so`() {
        val card = account("RappiCard", AccountType.CREDIT_CARD)
        show(
            AccountWithBalance(card, Money(-250_000_00L)),
            cardOverview = sampleCardOverview(accountId = 1L, today = today, dueDate = today.plusDays(1)),
        )

        composeRule.onNodeWithText("Pagar mañana").assertIsDisplayed()
    }

    @Test
    fun `card without a near payment shows the available credit`() {
        val card = account("RappiCard", AccountType.CREDIT_CARD)
        show(
            AccountWithBalance(card, Money(-250_000_00L)),
            cardOverview = sampleCardOverview(accountId = 1L, today = today, dueDate = LocalDate.of(2026, 11, 5)),
        )

        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Pagar en", substring = true).assertDoesNotExist()
    }

    @Test
    fun `overdue card shows the overdue status`() {
        val card = account("RappiCard", AccountType.CREDIT_CARD)
        show(
            AccountWithBalance(card, Money(-250_000_00L)),
            cardOverview =
                sampleCardOverview(
                    accountId = 1L,
                    today = today,
                    paymentStatus = StatementPaymentStatus.OVERDUE,
                ),
        )

        composeRule.onNodeWithText("Pago vencido").assertIsDisplayed()
        composeRule.onNodeWithText("Pagar en", substring = true).assertDoesNotExist()
    }

    @Test
    fun `transit card shows the trips it still pays`() {
        val transit = account("Cívica", AccountType.TRANSIT_CARD, fare = Money(355_000L), includeInTotal = false)
        show(AccountWithBalance(transit, Money(1_065_000L)))

        composeRule.onNodeWithText("3 viajes").assertIsDisplayed()
    }

    @Test
    fun `transit card in credit use says it has to be topped up`() {
        val transit = account("Tullave", AccountType.TRANSIT_CARD, fare = Money(355_000L))
        show(AccountWithBalance(transit, Money(-100_000L)))

        composeRule.onNodeWithText("Recarga para viajar").assertIsDisplayed()
    }

    @Test
    fun `ordinary account shows its type only when asked`() {
        val savings = account("Caja menor", AccountType.SAVINGS)
        show(AccountWithBalance(savings, Money(800_000L)), showTypeLabel = true)
        composeRule.onNodeWithText("Cuenta de ahorros").assertIsDisplayed()
    }

    @Test
    fun `ordinary account without the type flag shows no status line`() {
        val savings = account("Caja menor", AccountType.SAVINGS)
        show(AccountWithBalance(savings, Money(800_000L)), showTypeLabel = false)
        composeRule.onNodeWithText("Cuenta de ahorros").assertDoesNotExist()
        composeRule.onNodeWithText("8.000", substring = true).assertIsDisplayed()
    }

    @Test
    fun `trailing content replaces the balance`() {
        val savings = account("Caja menor", AccountType.SAVINGS)
        show(
            AccountWithBalance(savings, Money(1_234_500L)),
            trailing = { Text(text = "Controles") },
        )
        composeRule.onNodeWithText("Controles").assertIsDisplayed()
        composeRule.onNodeWithText("12.345", substring = true).assertDoesNotExist()
    }

    @Test
    fun `disabled row is not clickable`() {
        val savings = account("Caja menor", AccountType.SAVINGS)
        var clicks = 0
        show(AccountWithBalance(savings, Money.ZERO), enabled = false, onClick = { clicks++ })

        composeRule.onNodeWithText("Caja menor").assertIsNotEnabled()
        assertThat(clicks).isEqualTo(0)
    }
}
