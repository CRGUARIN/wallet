package com.crguarin.wallet.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.ui.components.HeroContourTrace
import com.crguarin.wallet.ui.motion.FLAT_FONT_SCALE
import com.crguarin.wallet.ui.motion.FirstShowGate
import com.crguarin.wallet.ui.motion.LocalMotionPolicy
import com.crguarin.wallet.ui.motion.MotionPolicy
import com.crguarin.wallet.ui.theme.DefaultWalletDepth
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class AccountDeckTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        FirstShowGate.reset()
        HeroContourTrace.reset()
    }

    @After
    fun tearDown() {
        FirstShowGate.reset()
        HeroContourTrace.reset()
    }

    private fun cashAccount() =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(0),
            color = 0xFF66BB6A.toInt(),
            icon = "wallet",
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    private val savingsAccount = cashAccount().copy(id = 2L, name = "Ahorros", icon = "savings", sortOrder = 1)

    private val transitAccount =
        cashAccount().copy(
            id = 3L,
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            icon = "directions_bus",
            sortOrder = 2,
            fare = Money(355_000L),
        )

    private val accounts =
        listOf(
            AccountWithBalance(cashAccount(), Money(450_000L)),
            AccountWithBalance(savingsAccount, Money(800_000L)),
            AccountWithBalance(transitAccount, Money(1_065_000L)),
        )

    private fun showDeck(
        onAccountClick: (Long) -> Unit = {},
        policy: MotionPolicy? = null,
    ) {
        composeRule.setContent {
            if (policy == null) {
                WalletTheme {
                    AccountDeck(
                        accounts = accounts,
                        totalBalance = Money(1_250_000L),
                        onAccountClick = onAccountClick,
                    )
                }
            } else {
                CompositionLocalProvider(LocalMotionPolicy provides policy) {
                    WalletTheme {
                        AccountDeck(
                            accounts = accounts,
                            totalBalance = Money(1_250_000L),
                            onAccountClick = onAccountClick,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `each card shows its name status and balance`() {
        showDeck()

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("$ 4.500").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("$ 8.000").assertIsDisplayed()
        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("3 viajes").assertIsDisplayed()
        composeRule.onNodeWithText("$ 10.650").assertIsDisplayed()
    }

    /** Taps the center of the touch target of the strip of [accountId]: the area the user sees. */
    private fun tapStrip(accountId: Long) {
        composeRule.onNodeWithTag(deckHitTag(accountId)).performTouchInput { click() }
    }

    @Test
    fun `tapping the strip of the first middle and last card opens its detail`() {
        val opened = mutableListOf<Long>()
        showDeck(onAccountClick = { opened += it })

        tapStrip(1L)
        tapStrip(3L)
        tapStrip(2L)

        assertThat(opened).containsExactly(1L, 3L, 2L).inOrder()
    }

    @Test
    fun `the deck falls back to a flat list with a large font`() {
        val opened = mutableListOf<Long>()
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                WalletTheme {
                    AccountDeck(
                        accounts = accounts,
                        totalBalance = Money(1_250_000L),
                        onAccountClick = { opened += it },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").performClick()

        assertThat(opened).containsExactly(2L)
    }

    @Test
    fun `the deck falls back to a flat list with a screen reader`() {
        showDeck(policy = MotionPolicy(animate = false, screenReader = true))

        // The flat list carries the accounts; the total card lives in HomeScreen, not in the deck.
        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("3 viajes").assertIsDisplayed()
        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("Saldo total").assertDoesNotExist()
    }

    @Test
    fun `every touch target of a strip is at least 48 dp tall`() {
        showDeck()

        // Robolectric runs at density 1, so dp and px are the same here.
        accounts.forEach { item ->
            val height =
                composeRule
                    .onNodeWithTag(deckHitTag(item.account.id))
                    .fetchSemanticsNode()
                    .boundsInRoot.height
            assertThat(height).isAtLeast(48f)
        }
    }

    @Test
    fun `clicking the center of each hit target opens its account`() {
        val opened = mutableListOf<Long>()
        showDeck(onAccountClick = { opened += it })

        listOf(1L, 2L, 3L).forEach { id ->
            val bounds = composeRule.onNodeWithTag(deckHitTag(id)).fetchSemanticsNode().boundsInRoot
            composeRule.onRoot().performTouchInput { click(Offset(bounds.center.x, bounds.center.y)) }
        }

        assertThat(opened).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `the total card is not clickable and its balance is a single node`() {
        showDeck()

        composeRule.onNodeWithText("Saldo total").assertHasNoClickAction()
        composeRule.onNodeWithText("$ 12.500").assertIsDisplayed()
    }

    @Test
    fun `after the deal every card is at rest`() {
        composeRule.mainClock.autoAdvance = false
        showDeck(policy = MotionPolicy(animate = true, screenReader = false))

        composeRule.mainClock.advanceTimeBy(12_000L)

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("$ 12.500").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    @Test
    fun `the name and the status line fit the strip at the largest 3d font scale`() {
        // Top padding, then the name and the status line in sp: at 1.3 each sp is 1.3 dp.
        val used =
            DeckStripTopPadding.value +
                (DeckStripNameLineHeight.value + DeckStripStatusLineHeight.value) * FLAT_FONT_SCALE
        assertThat(used).isAtMost(DefaultWalletDepth.deckStrip.value)
    }

    @Test
    fun `without animations the deck shows its final state at once`() {
        composeRule.mainClock.autoAdvance = false
        showDeck()

        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText("Ahorros").assertIsDisplayed()
        composeRule.onNodeWithText("Cívica").assertIsDisplayed()
        composeRule.onNodeWithText("$ 12.500").assertIsDisplayed()
    }
}
