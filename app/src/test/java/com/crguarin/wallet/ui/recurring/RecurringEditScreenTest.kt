package com.crguarin.wallet.ui.recurring

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
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
class RecurringEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun str(res: Int): String = app.getString(res)

    private val today = LocalDate.of(2026, 10, 4)

    private val account =
        AccountWithBalance(
            account =
                Account(
                    id = 1L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(1_000_000_00L),
                    color = 1,
                    icon = "savings",
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(1_000_000_00L),
        )

    private val category =
        Category(id = 10L, name = "Suscripciones", type = CategoryType.EXPENSE, color = 1, icon = "movie")

    private fun baseState() =
        RecurringEditUiState(
            isLoading = false,
            accounts = listOf(account),
            accountId = 1L,
            today = today,
            nextDate = today,
        )

    private fun setScreen(
        uiState: RecurringEditUiState,
        onSaveClick: () -> Unit = {},
        onTransactionClick: (Long) -> Unit = {},
        density: Density? = null,
    ) {
        composeRule.setContent {
            val content: @Composable () -> Unit = {
                WalletTheme {
                    RecurringEditScreen(
                        uiState = uiState,
                        onNavigateBack = {},
                        onTypeSelected = {},
                        onNameChanged = {},
                        onAmountChanged = {},
                        onOpenAccountSheet = {},
                        onDismissAccountSheet = {},
                        onAccountSelected = {},
                        onOpenCategorySheet = {},
                        onDismissCategorySheet = {},
                        onCategorySelected = {},
                        onFrequencySelected = {},
                        onIntervalChanged = {},
                        onOpenNextDatePicker = {},
                        onDismissNextDatePicker = {},
                        onNextDateSelected = {},
                        onEndNeverSelected = {},
                        onEndOnDateSelected = {},
                        onOpenEndDatePicker = {},
                        onDismissEndDatePicker = {},
                        onEndDateSelected = {},
                        onModeSelected = {},
                        onReminderSelected = {},
                        onNoteChanged = {},
                        onActiveChanged = {},
                        onSaveClick = onSaveClick,
                        onDeleteClick = {},
                        onDismissDeleteDialog = {},
                        onConfirmDelete = {},
                        onTransactionClick = onTransactionClick,
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

    @Test
    fun `form shows all fields in order`() {
        setScreen(baseState())

        composeRule.onNodeWithText(str(R.string.type_expense)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.type_income)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.category_name_label)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_frequency_label)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_frequency_weekly)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_frequency_monthly)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_frequency_yearly)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_next_date_label)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_next_date_hint)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_end_never)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_end_on_date)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_mode_auto)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_mode_confirm)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_reminder_none)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_save)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `interval shows unit plural for two months`() {
        setScreen(baseState().copy(frequency = RecurrenceFrequency.MONTHLY, intervalText = "2"))

        val plural = app.resources.getQuantityString(R.plurals.recurring_interval_months, 2, 2)
        assertThat(plural).isEqualTo("meses")
        composeRule.onNodeWithText("meses").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `field errors are shown`() {
        setScreen(
            baseState().copy(
                nameErrorRes = R.string.error_name_empty,
                amountErrorRes = R.string.error_amount_invalid,
            ),
        )

        composeRule.onNodeWithText(str(R.string.error_name_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.error_amount_invalid)).assertIsDisplayed()
    }

    @Test
    fun `history shows generated transactions and opens them`() {
        var openedId = -1L
        val details =
            TransactionDetails(
                transaction =
                    Transaction(
                        id = 100L,
                        type = TransactionType.EXPENSE,
                        amount = Money(45_900_00L),
                        accountId = 1L,
                        categoryId = 10L,
                        date = LocalDate.of(2026, 9, 12),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        recurringId = 5L,
                    ),
                accountName = "Ahorros",
                accountColor = 1,
                accountIcon = "savings",
                categoryName = "Suscripciones",
                categoryColor = 1,
                categoryIcon = "movie",
            )
        setScreen(
            baseState().copy(isEditMode = true, history = listOf(details)),
            onTransactionClick = { openedId = it },
        )

        composeRule.onNodeWithText(str(R.string.recurring_history_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Suscripciones").performScrollTo().performClick()
        assertThat(openedId).isEqualTo(100L)
    }

    @Test
    fun `history empty message when no records yet`() {
        setScreen(baseState().copy(isEditMode = true))

        composeRule.onNodeWithText(str(R.string.recurring_history_empty)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `save button calls save`() {
        var saved = false
        setScreen(baseState(), onSaveClick = { saved = true })

        composeRule.onNodeWithText(str(R.string.action_save)).performScrollTo().performClick()
        assertThat(saved).isTrue()
    }

    @Test
    fun `form renders at double font scale`() {
        setScreen(baseState(), density = Density(density = 1f, fontScale = 2f))

        composeRule.onNodeWithText(str(R.string.type_expense)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_frequency_weekly)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recurring_reminder_none)).performScrollTo().assertExists()
        composeRule.onNodeWithText(str(R.string.action_save)).performScrollTo().assertExists()
    }
}
