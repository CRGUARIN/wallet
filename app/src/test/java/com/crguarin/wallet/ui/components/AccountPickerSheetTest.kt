package com.crguarin.wallet.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.ui.accounts.sampleCardOverview
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AccountPickerSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cash =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(100_000L),
            color = 0xFF4CAF50.toInt(),
            icon = "wallet",
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    private val card =
        Account(
            id = 7L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-250_000_00L),
            color = 0xFFFF6F00.toInt(),
            icon = "credit_card",
            sortOrder = 1,
            createdAt = Instant.EPOCH,
        )

    @Test
    fun `card row shows debt and available instead of the raw balance`() {
        val today = LocalDate.of(2026, 9, 28)
        composeRule.setContent {
            WalletTheme {
                AccountPickerSheet(
                    accounts =
                        listOf(
                            AccountWithBalance(cash, Money(100_000L)),
                            AccountWithBalance(card, Money(-250_000_00L)),
                        ),
                    onAccountSelected = {},
                    onDismissRequest = {},
                    cardOverviews = mapOf(7L to sampleCardOverview(accountId = 7L)),
                    today = today,
                )
            }
        }

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
    }

    @Test
    fun `selecting an account calls back with the selected account`() {
        var selectedId: Long? = null
        composeRule.setContent {
            WalletTheme {
                AccountPickerSheet(
                    accounts = listOf(AccountWithBalance(cash, Money(100_000L))),
                    onAccountSelected = { selectedId = it.account.id },
                    onDismissRequest = {},
                )
            }
        }

        composeRule.onNodeWithText("Efectivo").performClick()
        assertThat(selectedId).isEqualTo(1L)
    }
}
