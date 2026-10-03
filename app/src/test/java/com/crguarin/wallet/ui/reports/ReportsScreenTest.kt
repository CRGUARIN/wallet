package com.crguarin.wallet.ui.reports

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rCO-w1000dp-h3000dp")
class ReportsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15)
    private val month = YearMonth.of(2026, 10)

    private fun slice(
        key: SliceKey,
        name: String?,
        systemKey: String?,
        amount: Long,
        perMille: Int,
        hasChildren: Boolean = false,
    ) = CategorySlice(
        key = key,
        name = name,
        systemKey = systemKey,
        color = 0xFFEF5350.toInt(),
        icon = "restaurant",
        amount = Money(amount),
        perMille = perMille,
        hasChildren = hasChildren,
    )

    private fun breakdownWithData() =
        CategoryBreakdown(
            total = Money(250_000_000L),
            slices =
                listOf(
                    slice(SliceKey.Category(10L), "Alimentación", "food", 112_500_000L, 450, hasChildren = true),
                    slice(SliceKey.Category(20L), "Transporte", "transport", 50_000_000L, 200),
                    slice(SliceKey.Others, null, null, 87_500_000L, 350),
                ),
            parentId = null,
        )

    private fun monthsWithData(): List<MonthTotals> =
        (5..10).map { monthNumber ->
            MonthTotals(
                yearMonth = YearMonth.of(2026, monthNumber),
                income = Money(18_000_000L),
                expense = Money(12_000_000L),
            )
        }

    private fun monthPace() =
        SpendingPace(
            yearMonth = month,
            current = (1..15).map { Money(it * 8_000_000L) },
            previous = (1..30).map { Money(it * 7_000_000L) },
            daysInMonth = 31,
            comparisonDay = 15,
            currentAtDay = Money(120_000_000L),
            previousAtDay = Money(105_000_000L),
            changePercent = 14,
        )

    private fun state(
        mode: ReportMode = ReportMode.MONTH,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: Long? = null,
        isLoading: Boolean = false,
        breakdown: CategoryBreakdown = breakdownWithData(),
        monthlyTotals: List<MonthTotals> = monthsWithData(),
        pace: SpendingPace? = monthPace(),
        selectedSliceKey: SliceKey? = null,
        drillDownParent: CategorySlice? = null,
    ) = ReportsUiState(
        isLoading = isLoading,
        mode = mode,
        yearMonth = month,
        today = today,
        type = type,
        accountId = accountId,
        selectedAccountName = null,
        accounts = emptyList(),
        summary = PeriodSummary(Money(250_000_000L), Money(200_000_000L), 25, Money(16_666_666L)),
        breakdown = breakdown,
        drillDownParent = drillDownParent,
        selectedSliceKey = selectedSliceKey,
        monthlyTotals = monthlyTotals,
        pace = if (mode == ReportMode.MONTH) pace else null,
        canGoNext = false,
    )

    private fun setScreen(
        uiState: ReportsUiState,
        onSelectSlice: (SliceKey) -> Unit = {},
        onDrillInto: (Long) -> Unit = {},
        onViewTransactions: (Long?, Long?, TransactionType, YearMonth) -> Unit = { _, _, _, _ -> },
    ) {
        composeRule.setContent {
            WalletTheme {
                ReportsScreen(
                    uiState = uiState,
                    onModeChange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onTypeChange = {},
                    onOpenAccountSheet = {},
                    onDismissAccountSheet = {},
                    onAccountSelected = {},
                    onSelectSlice = onSelectSlice,
                    onDrillInto = onDrillInto,
                    onDrillUp = {},
                    onSelectMonth = {},
                    onViewTransactions = onViewTransactions,
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun `shows title controls and the three cards in month mode`() {
        setScreen(state())

        composeRule.onNodeWithText("Reportes").assertIsDisplayed()
        composeRule.onNodeWithText("Mes").assertIsDisplayed()
        composeRule.onNodeWithText("Año").assertIsDisplayed()
        composeRule.onNodeWithText("Gasto").assertIsDisplayed()
        composeRule.onNodeWithText("Ingreso").assertIsDisplayed()
        composeRule.onNodeWithText("Todas las cuentas").assertIsDisplayed()
        composeRule.onNodeWithText("vs. mes anterior").assertIsDisplayed()
        composeRule.onNodeWithText("Promedio diario").assertIsDisplayed()
        composeRule.onNodeWithText("Por categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Evolución mensual").assertIsDisplayed()
        composeRule.onNodeWithText("Ritmo del mes").assertIsDisplayed()
    }

    @Test
    fun `year mode hides the month pace card`() {
        setScreen(state(mode = ReportMode.YEAR))

        composeRule.onNodeWithText("Por categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Evolución mensual").assertIsDisplayed()
        composeRule.onNodeWithText("Ritmo del mes").assertDoesNotExist()
        composeRule.onNodeWithText("vs. año anterior").assertIsDisplayed()
    }

    @Test
    fun `legend shows name amount and percentage and selecting a row calls selectSlice`() {
        var selected: SliceKey? = null
        setScreen(state(), onSelectSlice = { selected = it })

        composeRule.onNodeWithText("Alimentación").assertIsDisplayed()
        composeRule.onNodeWithText("$ 1.125.000").assertIsDisplayed()
        composeRule.onNodeWithText("45,0 %").assertIsDisplayed()

        composeRule.onNodeWithText("Alimentación").performClick()
        assertThat(selected).isEqualTo(SliceKey.Category(10L))
    }

    @Test
    fun `view subcategories calls drillInto with the category id`() {
        var drilledParent: Long? = null
        setScreen(state(), onDrillInto = { drilledParent = it })

        composeRule.onNodeWithContentDescription("Ver subcategorías").performClick()

        assertThat(drilledParent).isEqualTo(10L)
    }

    @Test
    fun `view transactions passes the report filters`() {
        var categoryId: Long? = null
        var accountId: Long? = null
        var type: TransactionType? = null
        var yearMonth: YearMonth? = null
        setScreen(
            state(accountId = 7L),
            onViewTransactions = { category, account, transactionType, selectedMonth ->
                categoryId = category
                accountId = account
                type = transactionType
                yearMonth = selectedMonth
            },
        )

        composeRule.onAllNodesWithContentDescription("Ver movimientos").onFirst().performClick()

        assertThat(categoryId).isEqualTo(10L)
        assertThat(accountId).isEqualTo(7L)
        assertThat(type).isEqualTo(TransactionType.EXPENSE)
        assertThat(yearMonth).isEqualTo(month)
    }

    @Test
    fun `shows the loading state`() {
        setScreen(state(isLoading = true))

        composeRule.onNodeWithContentDescription("Cargando").assertIsDisplayed()
        composeRule.onNodeWithText("Por categoría").assertDoesNotExist()
    }

    @Test
    fun `shows the global empty state below the controls`() {
        setScreen(state(monthlyTotals = (5..10).map { MonthTotals(YearMonth.of(2026, it), Money.ZERO, Money.ZERO) }))

        composeRule.onNodeWithText("No hay movimientos en este período").assertIsDisplayed()
        composeRule.onNodeWithText("Mes").assertIsDisplayed()
        composeRule.onNodeWithText("Por categoría").assertDoesNotExist()
    }

    @Test
    fun `shows the compact donut empty state when the type has no data`() {
        setScreen(state(breakdown = CategoryBreakdown(Money.ZERO, emptyList(), null)))

        composeRule.onNodeWithText("No hay gastos en este período").assertIsDisplayed()
        composeRule.onNodeWithText("Evolución mensual").assertIsDisplayed()
        composeRule.onNodeWithText("Ritmo del mes").assertIsDisplayed()
    }

    @Test
    fun `donut accessibility description contains the category names`() {
        setScreen(state())

        composeRule.onNodeWithContentDescription("Alimentación", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Gráfico de dona", substring = true).assertExists()
    }

    @Test
    @Config(qualifiers = "en-rUS-w1000dp-h3000dp")
    fun `english locale shows english labels`() {
        setScreen(state())

        composeRule.onNodeWithText("Reports").assertIsDisplayed()
        composeRule.onNodeWithText("Month").assertIsDisplayed()
        composeRule.onNodeWithText("By category").assertIsDisplayed()
        composeRule.onNodeWithText("Monthly evolution").assertIsDisplayed()
        composeRule.onNodeWithText("Month pace").assertIsDisplayed()
        composeRule.onNodeWithText("45.0%").assertIsDisplayed()
    }

    @Test
    fun `summary change label follows the selected period`() {
        var change by mutableStateOf(12)
        composeRule.setContent {
            WalletTheme {
                ReportsSummaryCard(
                    uiState =
                        state().copy(
                            summary = PeriodSummary(Money(250_000_000L), Money(200_000_000L), change, Money(16_666_666L)),
                        ),
                )
            }
        }

        composeRule.onNodeWithText("▲ 12 %").assertIsDisplayed()
        change = 25
        composeRule.onNodeWithText("▲ 25 %").assertIsDisplayed()
    }

    @Test
    fun `change value exposes an accessible up description`() {
        setScreen(state())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val percent = context.getString(R.string.reports_percent, "25")
        val expected = context.getString(R.string.reports_cd_change_up, percent)
        composeRule.onNodeWithContentDescription(expected).assertIsDisplayed()
    }
}
