package com.crguarin.wallet.ui.debts

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class DebtListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private val spanish = Locale.forLanguageTag("es")

    private val today = LocalDate.of(2026, 10, 4)

    private val marta = summary(3L, "Marta", DebtDirection.OWED_TO_ME, 300_000L, paid = 300_000L)

    private fun str(
        res: Int,
        vararg args: Any,
    ): String = app.getString(res, *args)

    private fun money(minorUnits: Long): String = Money(minorUnits).formatDisplay(spanish)

    private fun summary(
        id: Long,
        name: String,
        direction: DebtDirection,
        amount: Long,
        paid: Long = 0L,
        dueDate: LocalDate? = null,
        note: String? = null,
    ) = DebtSummary(
        debt =
            testDebt(
                id = id,
                personName = name,
                direction = direction,
                amount = Money(amount),
                dueDate = dueDate,
                note = note,
            ),
        paid = Money(paid),
    )

    private fun overview(
        owedToMe: List<DebtSummary> = emptyList(),
        iOwe: List<DebtSummary> = emptyList(),
        settled: List<DebtSummary> = emptyList(),
    ) = DebtsOverview(
        owedToMe = owedToMe,
        iOwe = iOwe,
        settled = settled,
        totalOwedToMe = owedToMe.fold(Money.ZERO) { acc, item -> acc + item.remaining },
        totalIOwe = iOwe.fold(Money.ZERO) { acc, item -> acc + item.remaining },
    )

    private fun setScreen(
        uiState: DebtListUiState,
        onAddClick: () -> Unit = {},
        onDebtClick: (Long) -> Unit = {},
        onToggleSettled: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                DebtListScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onAddClick = onAddClick,
                    onDebtClick = onDebtClick,
                    onToggleSettled = onToggleSettled,
                )
            }
        }
    }

    /**
     * Scrolls the lazy list in short steps until a node with [text] is composed.
     * Short swipes keep nearby items composed; full-viewport swipes would dispose them.
     */
    private fun scrollToComposed(text: String) {
        repeat(15) {
            if (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            composeRule.onRoot().performTouchInput {
                swipeUp(
                    startY = visibleSize.height * 2f / 3f,
                    endY = visibleSize.height * 1f / 3f,
                    durationMillis = 300,
                )
            }
        }
    }

    @Test
    fun `summary shows the balance of what is owed to me and what I owe`() {
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview =
                    overview(
                        owedToMe = listOf(summary(1L, "Ana", DebtDirection.OWED_TO_ME, 5_000_000L)),
                        iOwe = listOf(summary(2L, "Luis", DebtDirection.I_OWE, 2_000_000L, paid = 500_000L)),
                    ),
                today = today,
            ),
        )

        composeRule.onNodeWithText(str(R.string.debts_total_net)).assertIsDisplayed()
        composeRule.onNodeWithText("+ ${money(3_500_000L)}").assertIsDisplayed()
        composeRule.onNodeWithText("Ana").assertIsDisplayed()
    }

    @Test
    fun `rows show the partial payment and an overdue due date`() {
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview =
                    overview(
                        iOwe =
                            listOf(
                                summary(
                                    id = 2L,
                                    name = "Luis",
                                    direction = DebtDirection.I_OWE,
                                    amount = 2_000_000L,
                                    paid = 500_000L,
                                    dueDate = LocalDate.of(2026, 9, 30),
                                    note = "Moto",
                                ),
                            ),
                    ),
                today = today,
            ),
        )

        composeRule.onNodeWithText("Luis").assertIsDisplayed()
        composeRule.onNodeWithText("Moto").assertIsDisplayed()
        composeRule
            .onNodeWithText(str(R.string.debts_remaining_of, money(1_500_000L), money(2_000_000L)))
            .assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.debts_overdue)).assertIsDisplayed()
    }

    @Test
    fun `empty state offers the add action`() {
        var addClicked = false
        setScreen(
            DebtListUiState(isLoading = false, overview = overview(), today = today),
            onAddClick = { addClicked = true },
        )

        composeRule.onNodeWithText(str(R.string.debts_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.debts_empty_message)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.debts_add)).performClick()
        assertThat(addClicked).isTrue()
    }

    @Test
    fun `settled debts stay hidden while the section is collapsed`() {
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview = overview(settled = listOf(marta)),
                settledExpanded = false,
                today = today,
            ),
        )

        composeRule.onNodeWithText(str(R.string.debts_section_settled)).assertIsDisplayed()
        composeRule.onNodeWithText("Marta").assertDoesNotExist()
    }

    @Test
    fun `settled debts show with their badge once the section is expanded`() {
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview = overview(settled = listOf(marta)),
                settledExpanded = true,
                today = today,
            ),
        )

        scrollToComposed("Marta")
        composeRule.onNodeWithText("Marta").assertExists()
        composeRule.onNodeWithText(str(R.string.debts_settled_badge)).assertExists()
    }

    @Test
    fun `tapping the settled header asks to toggle the section`() {
        var toggled = false
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview = overview(settled = listOf(marta)),
                today = today,
            ),
            onToggleSettled = { toggled = true },
        )

        composeRule.onNodeWithText(str(R.string.debts_section_settled)).performClick()

        assertThat(toggled).isTrue()
    }

    @Test
    fun `tapping a debt asks to open its detail`() {
        var openedId: Long? = null
        setScreen(
            DebtListUiState(
                isLoading = false,
                overview = overview(owedToMe = listOf(summary(7L, "Ana", DebtDirection.OWED_TO_ME, 5_000_000L))),
                today = today,
            ),
            onDebtClick = { openedId = it },
        )

        composeRule.onNodeWithText("Ana").performClick()

        assertThat(openedId).isEqualTo(7L)
    }
}
