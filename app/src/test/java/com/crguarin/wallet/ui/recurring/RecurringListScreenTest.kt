package com.crguarin.wallet.ui.recurring

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringOverview
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
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
class RecurringListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun str(res: Int): String = app.getString(res)

    private val today = LocalDate.of(2026, 10, 4)

    private fun payment(
        id: Long,
        name: String = "Netflix",
        mode: RecurringMode = RecurringMode.AUTO,
        reminder: Int? = null,
        isActive: Boolean = true,
        nextDueDate: LocalDate = LocalDate.of(2026, 10, 12),
    ) = RecurringPayment(
        id = id,
        name = name,
        type = TransactionType.EXPENSE,
        amount = Money(45_900_00L),
        accountId = 1L,
        categoryId = 10L,
        frequency = RecurrenceFrequency.MONTHLY,
        interval = 1,
        startDate = LocalDate.of(2026, 9, 12),
        nextDueDate = nextDueDate,
        mode = mode,
        reminderDaysBefore = reminder,
        isActive = isActive,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun item(
        payment: RecurringPayment,
        pendingCount: Int = 0,
    ) = RecurringItem(
        payment = payment,
        accountName = "Ahorros",
        accountType = AccountType.SAVINGS,
        category =
            Category(
                id = 10L,
                name = "Suscripciones",
                type = CategoryType.EXPENSE,
                color = 1,
                icon = "movie",
            ),
        pendingCount = pendingCount,
        blockReason = null,
        monthlyEquivalent = Money(45_900_00L),
    )

    private fun setScreen(
        uiState: RecurringListUiState,
        notificationsBlocked: Boolean = false,
        onAddClick: () -> Unit = {},
        onPaymentClick: (Long) -> Unit = {},
        onConfirmAmountChanged: (String) -> Unit = {},
        onConfirmRecord: () -> Unit = {},
        onEnableNotifications: () -> Unit = {},
        density: Density? = null,
    ) {
        composeRule.setContent {
            val content: @Composable () -> Unit = {
                WalletTheme {
                    RecurringListScreen(
                        uiState = uiState,
                        notificationsBlocked = notificationsBlocked,
                        onNavigateBack = {},
                        onAddClick = onAddClick,
                        onPaymentClick = onPaymentClick,
                        onConfirmClick = {},
                        onSkipClick = {},
                        onConfirmAmountChanged = onConfirmAmountChanged,
                        onOpenConfirmDatePicker = {},
                        onDismissConfirmDatePicker = {},
                        onConfirmDateSelected = {},
                        onConfirmRecord = onConfirmRecord,
                        onDismissConfirm = {},
                        onConfirmSkip = {},
                        onDismissSkip = {},
                        onEnableNotifications = onEnableNotifications,
                    )
                }
            }
            if (density == null) {
                content()
            } else {
                CompositionLocalProvider(LocalDensity provides density) {
                    content()
                }
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

    private fun emptyOverview() =
        RecurringOverview(
            pending = emptyList(),
            active = emptyList(),
            inactive = emptyList(),
            monthlyExpense = Money.ZERO,
            monthlyIncome = Money.ZERO,
        )

    @Test
    fun `empty state shows title message and add action`() {
        var addClicked = false
        setScreen(
            uiState = RecurringListUiState(isLoading = false, overview = emptyOverview(), today = today),
            onAddClick = { addClicked = true },
        )

        composeRule.onNodeWithText(str(R.string.recurring_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_empty_message)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_add)).performClick()
        assertThat(addClicked).isTrue()
    }

    // The payment cards are taller now: a phone-sized screen keeps the three sections in reach.
    @Test
    @Config(qualifiers = "es-w360dp-h800dp")
    fun `three sections are shown with their payments`() {
        val overview =
            RecurringOverview(
                pending =
                    listOf(
                        item(
                            payment(id = 1L, name = "Energía", mode = RecurringMode.CONFIRM),
                            pendingCount = 2,
                        ),
                    ),
                active = listOf(item(payment(id = 2L, name = "Netflix"))),
                inactive = listOf(item(payment(id = 3L, name = "Gimnasio", isActive = false))),
                monthlyExpense = Money(45_900_00L),
                monthlyIncome = Money.ZERO,
            )
        setScreen(
            uiState = RecurringListUiState(isLoading = false, overview = overview, today = today),
        )

        composeRule.onNodeWithText(str(R.string.recurring_section_pending)).assertIsDisplayed()
        composeRule.onNodeWithText("Energía").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_confirm_action)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_skip_action)).assertIsDisplayed()
        scrollToComposed(str(R.string.recurring_section_active))
        composeRule.onNodeWithText(str(R.string.recurring_section_active)).assertExists()
        composeRule.onNodeWithText("Netflix").assertExists()
        scrollToComposed(str(R.string.recurring_section_inactive))
        composeRule.onNodeWithText(str(R.string.recurring_section_inactive)).assertExists()
        composeRule.onNodeWithText("Gimnasio").assertExists()
        composeRule.onNodeWithText(str(R.string.recurring_paused)).assertExists()
    }

    @Test
    fun `confirm dialog shows prefilled amount and edits it`() {
        var edited = ""
        val pending = item(payment(id = 1L, name = "Energía", mode = RecurringMode.CONFIRM))
        setScreen(
            uiState =
                RecurringListUiState(
                    isLoading = false,
                    overview =
                        RecurringOverview(
                            pending = listOf(pending),
                            active = emptyList(),
                            inactive = emptyList(),
                            monthlyExpense = Money.ZERO,
                            monthlyIncome = Money.ZERO,
                        ),
                    confirmItem = pending,
                    confirmAmountText = "45900",
                    confirmDate = LocalDate.of(2026, 10, 4),
                    today = today,
                ),
            onConfirmAmountChanged = { edited = it },
        )

        composeRule.onNodeWithText("Registrar Energía").assertIsDisplayed()
        composeRule.onNodeWithText("45900").performTextReplacement("50000")
        assertThat(edited).isEqualTo("50000")
    }

    @Test
    fun `permission banner shows only when blocked and requests on click`() {
        var requested = false
        val overview =
            RecurringOverview(
                pending = emptyList(),
                active = listOf(item(payment(id = 2L, name = "Netflix", reminder = 3))),
                inactive = emptyList(),
                monthlyExpense = Money(45_900_00L),
                monthlyIncome = Money.ZERO,
            )
        setScreen(
            uiState = RecurringListUiState(isLoading = false, overview = overview, today = today),
            notificationsBlocked = true,
            onEnableNotifications = { requested = true },
        )

        composeRule.onNodeWithText(str(R.string.recurring_notifications_blocked)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_notifications_enable)).performClick()
        assertThat(requested).isTrue()
    }

    @Test
    fun `permission banner hidden when not blocked`() {
        val overview =
            RecurringOverview(
                pending = emptyList(),
                active = listOf(item(payment(id = 2L, name = "Netflix", reminder = 3))),
                inactive = emptyList(),
                monthlyExpense = Money(45_900_00L),
                monthlyIncome = Money.ZERO,
            )
        setScreen(
            uiState = RecurringListUiState(isLoading = false, overview = overview, today = today),
            notificationsBlocked = false,
        )

        composeRule.onNodeWithText(str(R.string.recurring_notifications_blocked)).assertDoesNotExist()
    }

    @Test
    fun `notifiable payments are those with a reminder or confirm mode`() {
        fun overview(
            pending: List<RecurringItem> = emptyList(),
            active: List<RecurringItem> = emptyList(),
            inactive: List<RecurringItem> = emptyList(),
        ) = RecurringOverview(
            pending = pending,
            active = active,
            inactive = inactive,
            monthlyExpense = Money.ZERO,
            monthlyIncome = Money.ZERO,
        )

        assertThat(hasNotifiablePayments(null)).isFalse()
        assertThat(hasNotifiablePayments(overview(active = listOf(item(payment(id = 1L, reminder = 3)))))).isTrue()
        assertThat(
            hasNotifiablePayments(
                overview(active = listOf(item(payment(id = 1L, mode = RecurringMode.CONFIRM)))),
            ),
        ).isTrue()
        assertThat(hasNotifiablePayments(overview(active = listOf(item(payment(id = 1L)))))).isFalse()
        assertThat(
            hasNotifiablePayments(
                overview(inactive = listOf(item(payment(id = 1L, reminder = 3, isActive = false)))),
            ),
        ).isFalse()
    }

    @Test
    fun `list rows and actions render at double font scale`() {
        val overview =
            RecurringOverview(
                pending =
                    listOf(
                        item(
                            payment(id = 1L, name = "Energía", mode = RecurringMode.CONFIRM),
                            pendingCount = 2,
                        ),
                    ),
                active = listOf(item(payment(id = 2L, name = "Netflix"))),
                inactive = emptyList(),
                monthlyExpense = Money(45_900_00L),
                monthlyIncome = Money.ZERO,
            )
        setScreen(
            uiState = RecurringListUiState(isLoading = false, overview = overview, today = today),
            density = Density(density = 1f, fontScale = 2f),
        )

        composeRule.onNodeWithText(str(R.string.recurring_monthly_expense)).assertIsDisplayed()
        scrollToComposed("Energía")
        composeRule.onNodeWithText("Energía").assertExists()
        scrollToComposed(str(R.string.recurring_confirm_action))
        composeRule.onNodeWithText(str(R.string.recurring_confirm_action)).assertExists()
        composeRule.onNodeWithText(str(R.string.recurring_skip_action)).assertExists()
        scrollToComposed("Netflix")
        composeRule.onNodeWithText("Netflix").assertExists()
    }
}
