package com.crguarin.wallet.ui.accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.motion.LocalMotionPolicy
import com.crguarin.wallet.ui.motion.MotionPolicy
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.contrastRatio
import com.crguarin.wallet.ui.transactions.rememberEntriesPagingItems
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
class AccountHeroTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cashAccount =
        Account(
            id = 11L,
            name = "Caja menor",
            type = AccountType.CASH,
            initialBalance = Money(150_000L),
            color = 0xFF4CAF50.toInt(),
            icon = "wallet",
            sortOrder = 0,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val cardAccount =
        Account(
            id = 7L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-250_000_00L),
            color = 0xFFC2185B.toInt(),
            icon = "credit_card",
            sortOrder = 0,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val transitAccount =
        Account(
            id = 9L,
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            initialBalance = Money(1_050_000L),
            color = 0xFF00897B.toInt(),
            icon = "directions_bus",
            fare = Money(350_000L),
            sortOrder = 0,
            isArchived = false,
            includeInTotal = true,
            createdAt = Instant.EPOCH,
        )

    private val monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L))

    private val today = LocalDate.of(2026, 9, 28)

    @Test
    fun `hero face keeps white content with contrast over any account color`() {
        // A light color (yellow) must come out darkened; content stays white either way.
        val yellow = heroCardFace(Color(0xFFFFEB3B.toInt()))
        assertThat(yellow.content).isEqualTo(Color.White)
        assertThat(contrastRatio(Color.White, yellow.start)).isAtLeast(4.5f)

        val green = heroCardFace(Color(0xFF4CAF50.toInt()))
        assertThat(green.content).isEqualTo(Color.White)
        assertThat(contrastRatio(Color.White, green.start)).isAtLeast(4.5f)
    }

    @Test
    fun `cash hero shows identity and the floor shadow in 3d mode`() {
        composeRule.setContent {
            WalletTheme {
                AccountHeaderCard(
                    account = cashAccount,
                    balance = Money(450_000L),
                    monthSummary = monthSummary,
                )
            }
        }

        composeRule.onNodeWithText("Caja menor").assertIsDisplayed()
        composeRule.onNode(hasTestTag(HERO_FACE_TAG)).assertExists()
        composeRule.onNode(hasTestTag(HERO_FLOOR_TAG)).assertExists()
    }

    @Test
    fun `card hero amount is a single node with the expense description`() {
        composeRule.setContent {
            WalletTheme {
                CardSummaryCard(
                    overview = sampleCardOverview(accountId = 7L),
                    today = today,
                    account = cardAccount,
                )
            }
        }

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        // The type is not repeated under the name any more: the name is the title of the screen.
        composeRule.onNodeWithText("Tarjeta de crédito").assertDoesNotExist()
        composeRule.onNodeWithText("Deuda").assertIsDisplayed()

        val amountNodes =
            composeRule
                .onAllNodes(hasTestTag(HERO_FACE_TAG))
                .fetchSemanticsNodes()
        assertThat(amountNodes).hasSize(1)
        val described =
            composeRule
                .onAllNodes(hasContentDescription("Gasto", substring = true))
                .fetchSemanticsNodes()
        assertThat(described).hasSize(1)
        val text = described[0].config[SemanticsProperties.Text].first().text
        assertThat(text).contains("$")
    }

    @Test
    fun `transit hero shows the balance label and the trips left`() {
        composeRule.setContent {
            WalletTheme {
                TransitCardSummaryCard(
                    account = transitAccount,
                    balance = Money(1_050_000L),
                    monthSummary = monthSummary,
                )
            }
        }

        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("3 viajes restantes").assertIsDisplayed()
    }

    @Test
    fun `flat mode keeps the face without the floor shadow`() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                WalletTheme {
                    AccountHeaderCard(
                        account = cashAccount,
                        balance = Money(450_000L),
                        monthSummary = monthSummary,
                    )
                }
            }
        }

        composeRule.onNode(hasTestTag(HERO_FACE_TAG)).assertExists()
        composeRule.onNode(hasTestTag(HERO_FLOOR_TAG)).assertDoesNotExist()
        composeRule.onNodeWithText("Caja menor").assertIsDisplayed()
    }

    @Test
    fun `account-less card keeps a flat primary face without floor shadow`() {
        composeRule.setContent {
            WalletTheme {
                CardSummaryCard(
                    overview = sampleCardOverview(),
                    today = today,
                )
            }
        }

        composeRule.onNodeWithText("Deuda").assertIsDisplayed()
        composeRule.onNode(hasTestTag(HERO_FACE_TAG)).assertExists()
        composeRule.onNode(hasTestTag(HERO_FLOOR_TAG)).assertDoesNotExist()
    }

    @Test
    fun `quota bar ends at used per mille over 1000`() {
        composeRule.setContent {
            WalletTheme {
                CardSummaryCard(
                    overview = sampleCardOverview().copy(usedPerMille = 500),
                    today = today,
                )
            }
        }

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 0.5f, range = 0f..1f, steps = 0)))
            .assertExists()
    }

    @Test
    fun `animated hero settles and the clock goes idle`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animate = true, screenReader = false)) {
                WalletTheme {
                    AccountHeaderCard(
                        account = cashAccount,
                        balance = Money(450_000L),
                        monthSummary = monthSummary,
                    )
                }
            }
        }

        // Past the 10 s idle window plus every entrance: float, sheen and rolling settle.
        composeRule.mainClock.advanceTimeBy(12_000L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Caja menor").assertIsDisplayed()
    }

    @Test
    fun `card keys are clickable and tall enough`() {
        var paid = false
        var reconciled = false
        composeRule.setContent {
            WalletTheme {
                AccountDetailScreen(
                    uiState =
                        AccountDetailUiState(
                            isLoading = false,
                            accountWithBalance = AccountWithBalance(cardAccount, Money(-250_000_00L)),
                            today = today,
                            cardOverview = sampleCardOverview(accountId = 7L),
                        ),
                    pagingItems = rememberEntriesPagingItems(emptyList()),
                    onNavigateBack = {},
                    onEditClick = {},
                    onTransferClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onArchiveClick = {},
                    onUnarchiveClick = {},
                    onDeleteClick = {},
                    onConfirmDelete = {},
                    onConfirmArchiveInsteadOfDelete = {},
                    onDismissDialogs = {},
                    onPayCardClick = { paid = true },
                    onReconcileClick = { reconciled = true },
                )
            }
        }

        composeRule.onNodeWithText("Pagar tarjeta").performScrollTo().performClick()
        assertThat(paid).isTrue()
        composeRule.onNodeWithText("Pagar tarjeta").assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithText("Conciliar extracto").performScrollTo().performClick()
        assertThat(reconciled).isTrue()
        composeRule.onNodeWithText("Conciliar extracto").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `cash secondary keys call back`() {
        var clicked: TransactionType? = null
        composeRule.setContent {
            WalletTheme {
                AccountDetailScreen(
                    uiState =
                        AccountDetailUiState(
                            isLoading = false,
                            accountWithBalance = AccountWithBalance(cashAccount, Money(450_000L)),
                            monthSummary = monthSummary,
                        ),
                    pagingItems = rememberEntriesPagingItems(emptyList()),
                    onNavigateBack = {},
                    onEditClick = {},
                    onTransferClick = {},
                    onTransactionClick = {},
                    onViewAllTransactions = {},
                    onArchiveClick = {},
                    onUnarchiveClick = {},
                    onDeleteClick = {},
                    onConfirmDelete = {},
                    onConfirmArchiveInsteadOfDelete = {},
                    onDismissDialogs = {},
                    onNewTransactionClick = { clicked = it },
                )
            }
        }

        composeRule.onNodeWithText("Gasto").performScrollTo().performClick()
        assertThat(clicked).isEqualTo(TransactionType.EXPENSE)
    }
}
