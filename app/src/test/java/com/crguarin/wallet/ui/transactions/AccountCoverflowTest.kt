package com.crguarin.wallet.ui.transactions

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.motion.LocalMotionPolicy
import com.crguarin.wallet.ui.motion.MotionPolicy
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * The 3D account coverflow (§9.4 and §9.6): tapping a side card selects it, the chosen card is
 * selected, the flat fallback shows today's selector, and "Cuenta" opens the sheet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class AccountCoverflowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cash =
        AccountWithBalance(
            account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(100_000L),
                    color = 0xFF42A5F5.toInt(),
                    icon = "wallet",
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(100_000L),
        )

    private val savings =
        AccountWithBalance(
            account =
                Account(
                    id = 2L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(500_000L),
                    color = 0xFF66BB6A.toInt(),
                    icon = "savings",
                    sortOrder = 1,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(500_000L),
        )

    private fun showCoverflow(
        selectedAccountId: Long? = 1L,
        onAccountSelected: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                AccountCoverflow(
                    accounts = listOf(cash, savings),
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = onAccountSelected,
                    onOpenAccountSheet = {},
                )
            }
        }
    }

    private fun showScreen(
        accounts: List<AccountWithBalance> = listOf(cash, savings),
        archivedAccount: Account? = null,
        onOpenAccountSheet: () -> Unit = {},
        fontScale: Float = 1f,
        screenReader: Boolean = false,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalMotionPolicy provides MotionPolicy(animate = false, screenReader = screenReader),
            ) {
                WalletTheme {
                    TransactionEditScreen(
                        uiState =
                            TransactionEditUiState(
                                isLoading = false,
                                selectedType = TransactionType.EXPENSE,
                                amountText = "50000",
                                accountId = 1L,
                                accounts = accounts,
                                archivedAccount = archivedAccount,
                                date = LocalDate.of(2026, 9, 28),
                            ),
                        onNavigateBack = {},
                        onTypeSelected = {},
                        onAmountChanged = {},
                        onOpenAccountSheet = onOpenAccountSheet,
                        onDismissAccountSheet = {},
                        onAccountSelected = {},
                        onOpenToAccountSheet = {},
                        onDismissToAccountSheet = {},
                        onToAccountSelected = {},
                        onSwapAccounts = {},
                        onOpenCategorySheet = {},
                        onDismissCategorySheet = {},
                        onCategorySelected = {},
                        onNewCategoryClick = {},
                        onNewAccountClick = {},
                        onOpenDatePicker = {},
                        onDismissDatePicker = {},
                        onDateSelected = {},
                        onTodayClick = {},
                        onYesterdayClick = {},
                        onNoteChanged = {},
                        onSaveClick = {},
                        onDeleteClick = {},
                        onDismissDeleteDialog = {},
                        onConfirmDelete = {},
                        onConfirmNegativeBalance = {},
                        onDismissNegativeBalance = {},
                    )
                }
            }
        }
    }

    @Test
    fun `tapping a side card selects that account`() {
        var selected: Long? = null
        showCoverflow(onAccountSelected = { selected = it })

        composeRule.onNodeWithText("Ahorros").performClick()

        assertThat(selected).isEqualTo(2L)
    }

    @Test
    fun `the chosen card is selected and the other is not`() {
        showCoverflow(selectedAccountId = 1L)

        composeRule.onNodeWithText("Efectivo").assertIsSelected()
        composeRule.onNodeWithText("Ahorros").assertIsNotSelected()
    }

    @Test
    fun `the coverflow shows every account with its balance`() {
        showCoverflow()

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
    }

    @Test
    fun `a card shows the available credit`() {
        val card =
            AccountWithBalance(
                account =
                    Account(
                        id = 3L,
                        name = "RappiCard",
                        type = AccountType.CREDIT_CARD,
                        initialBalance = Money.ZERO,
                        color = 0xFFEF6C00.toInt(),
                        icon = "credit_card",
                        createdAt = Instant.EPOCH,
                    ),
                balance = Money.ZERO,
            )
        val overview =
            CardOverview(
                accountId = 3L,
                debt = Money(1_280_000L),
                creditLimit = Money(5_000_000L),
                available = Money(3_720_000L),
                usedPerMille = 256,
                nextClosingDate = LocalDate.of(2026, 10, 15),
                lastStatement = null,
                pendingStatementAmount = null,
                pendingStatementDueDate = null,
                paymentStatus = null,
                nextStatementCapitalEstimate = Money.ZERO,
                activeInstallments = emptyList(),
            )
        composeRule.setContent {
            WalletTheme {
                AccountCoverflow(
                    accounts = listOf(cash, card),
                    selectedAccountId = 3L,
                    onAccountSelected = {},
                    onOpenAccountSheet = {},
                    cardOverviews = mapOf(3L to overview),
                )
            }
        }

        composeRule.onNodeWithText("Disponible", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w360dp-h2400dp")
    fun `with a large font the flat selector replaces the coverflow`() {
        showScreen(fontScale = 2f)

        // The flat selector only shows the chosen account: the other name is gone.
        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es-w360dp-h2400dp")
    fun `with a screen reader the flat selector replaces the coverflow`() {
        showScreen(screenReader = true)

        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es-w360dp-h2400dp")
    fun `with a single account the flat selector stays`() {
        showScreen(accounts = listOf(cash))

        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w360dp-h2400dp")
    fun `with an archived account the current selector stays`() {
        showScreen(archivedAccount = cash.account.copy(isArchived = true))

        composeRule.onNodeWithText("Cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es-w360dp-h2400dp")
    fun `cuenta opens the account sheet from the coverflow header`() {
        var sheets = 0
        showScreen(onOpenAccountSheet = { sheets++ })

        // Both names prove the coverflow (not the flat selector) is showing.
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("Cuenta").performClick()

        assertThat(sheets).isEqualTo(1)
    }
}
