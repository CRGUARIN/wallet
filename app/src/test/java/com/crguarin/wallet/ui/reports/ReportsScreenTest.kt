package com.crguarin.wallet.ui.reports

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.BalanceGranularity
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
import java.util.Locale

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

    @Composable
    private fun ScreenContent(
        uiState: ReportsUiState,
        onSelectSlice: (SliceKey) -> Unit,
        onDrillInto: (Long) -> Unit,
        onViewTransactions: (Long?, Long?, TransactionType, YearMonth) -> Unit,
        onModeChange: (ReportMode) -> Unit,
        onRetry: () -> Unit,
    ) {
        WalletTheme {
            ReportsScreen(
                uiState = uiState,
                onModeChange = onModeChange,
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
                onRetry = onRetry,
            )
        }
    }

    private fun setScreen(
        uiState: ReportsUiState,
        onSelectSlice: (SliceKey) -> Unit = {},
        onDrillInto: (Long) -> Unit = {},
        onViewTransactions: (Long?, Long?, TransactionType, YearMonth) -> Unit = { _, _, _, _ -> },
        onModeChange: (ReportMode) -> Unit = {},
        onRetry: () -> Unit = {},
        density: Density? = null,
    ) {
        composeRule.setContent {
            if (density == null) {
                ScreenContent(uiState, onSelectSlice, onDrillInto, onViewTransactions, onModeChange, onRetry)
            } else {
                CompositionLocalProvider(LocalDensity provides density) {
                    ScreenContent(uiState, onSelectSlice, onDrillInto, onViewTransactions, onModeChange, onRetry)
                }
            }
        }
    }

    private fun clickDonutTop() {
        val node = composeRule.onNodeWithContentDescription("Gráfico de dona", substring = true)
        val size = node.fetchSemanticsNode().size
        node.performTouchInput { click(Offset(size.width * 0.5f, size.height * 0.1f)) }
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

    @Test
    fun `month mode selects the month and dims only future bars`() {
        val (selected, dimmed) =
            evolutionSelection(monthsWithData(), ReportMode.MONTH, YearMonth.of(2026, 10), YearMonth.of(2026, 7))

        assertThat(selected).isEqualTo(5)
        assertThat(dimmed).containsExactly(false, false, false, true, true, true).inOrder()
    }

    @Test
    fun `year mode selects nothing and dims nothing`() {
        val (selected, dimmed) =
            evolutionSelection(monthsWithData(), ReportMode.YEAR, YearMonth.of(2026, 10), YearMonth.of(2026, 10))

        assertThat(selected).isNull()
        assertThat(dimmed).containsExactly(false, false, false, false, false, false).inOrder()
    }

    @Test
    fun `narrow month letter is a single capital`() {
        val locale = Locale("es", "CO")

        assertThat(monthNarrowLetter(YearMonth.of(2026, 1), locale)).isEqualTo("E")
        assertThat(monthNarrowLetter(YearMonth.of(2026, 10), locale)).isEqualTo("O")
    }

    @Test
    fun `bars description uses the full month name`() {
        setScreen(state())

        composeRule.onNodeWithContentDescription("octubre", substring = true).assertExists()
    }

    @Test
    fun `cards derive their axes from axisScale`() {
        val months = monthsWithData()
        val dataMax = months.maxOf { maxOf(it.income.minorUnits, it.expense.minorUnits) }
        val evolution = evolutionAxisScale(months)
        assertThat(evolution).isEqualTo(AxisMath.axisRange(0L, dataMax))
        assertThat(evolution.minMinorUnits).isEqualTo(0L)
        assertThat(evolution.maxMinorUnits).isAtLeast(dataMax)
        assertThat(evolution.ticksMinorUnits.first()).isEqualTo(0L)
        assertThat(evolution.ticksMinorUnits.last()).isEqualTo(evolution.maxMinorUnits)

        val pace = monthPace()
        val paceMax = maxOf(pace.current.maxOf { it.minorUnits }, pace.previous.maxOf { it.minorUnits })
        val paceScale = paceAxisScale(pace)
        assertThat(paceScale).isEqualTo(AxisMath.axisScale(paceMax))
        assertThat(paceScale.maxMinorUnits).isAtLeast(paceMax)
        assertThat(paceScale.ticksMinorUnits.first()).isEqualTo(0L)
        assertThat(paceScale.ticksMinorUnits.last()).isEqualTo(paceScale.maxMinorUnits)
    }

    @Test
    fun `donut center shows a huge total at double font scale`() {
        val huge =
            CategoryBreakdown(
                total = Money(12_345_678_900L),
                slices =
                    listOf(
                        slice(SliceKey.Category(10L), "Alimentación", "food", 12_000_000_000L, 972),
                        slice(SliceKey.Category(20L), "Transporte", "transport", 345_678_900L, 28),
                    ),
                parentId = null,
            )
        setScreen(state(breakdown = huge), density = Density(density = 1f, fontScale = 2f))

        val center = composeRule.onNodeWithText("$ 123.456.789")
        center.assertIsDisplayed()
        // The amount shrinks to fit instead of wrapping over the ring.
        val layouts = mutableListOf<TextLayoutResult>()
        center
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        assertThat(layouts.single().lineCount).isEqualTo(1)
    }

    @Test
    fun `legend amount columns line up with and without trailing actions`() {
        setScreen(state())

        val foodRight =
            composeRule
                .onNodeWithText("$ 1.125.000", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.right
        val transportRight =
            composeRule
                .onNodeWithText("$ 500.000", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.right
        val othersRight =
            composeRule
                .onNodeWithText("$ 875.000", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.right
        assertThat(foodRight).isWithin(1f).of(transportRight)
        assertThat(foodRight).isWithin(1f).of(othersRight)
    }

    @Test
    fun `pace legend explains both lines`() {
        setScreen(state())

        composeRule.onNodeWithText("Este mes").assertIsDisplayed()
        composeRule.onNodeWithText("Mes anterior").assertIsDisplayed()
    }

    @Test
    fun `pace legend hides the previous month without comparison data`() {
        setScreen(state(pace = monthPace().copy(previous = List(30) { Money.ZERO })))

        composeRule.onNodeWithText("Este mes").assertIsDisplayed()
        composeRule.onNodeWithText("Mes anterior").assertDoesNotExist()
    }

    @Test
    fun `drill-down general row has no view-transactions action`() {
        val parent = slice(SliceKey.Category(10L), "Alimentación", "food", 200_000_000L, 800, hasChildren = true)
        val breakdown =
            CategoryBreakdown(
                total = Money(250_000_000L),
                slices =
                    listOf(
                        CategorySlice(
                            key = SliceKey.ParentDirect(10L),
                            name = "Alimentación",
                            systemKey = "food",
                            color = 0xFFEF5350.toInt(),
                            icon = "restaurant",
                            amount = Money(50_000_000L),
                            perMille = 200,
                            hasChildren = false,
                        ),
                        CategorySlice(
                            key = SliceKey.Category(11L),
                            name = "Mercado",
                            systemKey = null,
                            color = 0xFFEF5350.toInt(),
                            icon = "restaurant",
                            amount = Money(200_000_000L),
                            perMille = 800,
                            hasChildren = false,
                        ),
                    ),
                parentId = 10L,
            )
        setScreen(state(breakdown = breakdown, drillDownParent = parent))

        composeRule.onNodeWithText("Alimentación (general)").assertIsDisplayed()
        composeRule.onNodeWithText("Mercado").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Ver movimientos", substring = true).assertCountEquals(1)
    }

    @Test
    fun `error state shows the message and retries`() {
        var retried = false
        setScreen(state().copy(hasError = true), onRetry = { retried = true })

        composeRule.onNodeWithText("No se pudieron cargar los reportes").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()
        assertThat(retried).isTrue()
    }

    @Test
    fun `refreshing disables card actions but keeps controls and content`() {
        var selected: SliceKey? = null
        var viewed = false
        var modeChanged: ReportMode? = null
        setScreen(
            state().copy(isRefreshing = true),
            onSelectSlice = { selected = it },
            onViewTransactions = { _, _, _, _ -> viewed = true },
            onModeChange = { modeChanged = it },
        )

        composeRule.onNodeWithText("Por categoría").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cargando").assertDoesNotExist()
        composeRule.onAllNodesWithContentDescription("Ver movimientos", substring = true).onFirst().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Ver subcategorías", substring = true).assertIsNotEnabled()
        clickDonutTop()
        assertThat(selected).isNull()
        assertThat(viewed).isFalse()

        composeRule.onNodeWithText("Año").performClick()
        assertThat(modeChanged).isEqualTo(ReportMode.YEAR)
    }

    @Test
    fun `donut tap selects a slice when not refreshing`() {
        var selected: SliceKey? = null
        setScreen(state(), onSelectSlice = { selected = it })

        clickDonutTop()

        assertThat(selected).isEqualTo(SliceKey.Category(10L))
    }

    @Test
    fun `monthly evolution legend shows the net entry`() {
        setScreen(state())

        composeRule.onAllNodesWithText("Neto").assertCountEquals(1)
    }

    @Test
    fun `signed money carries an explicit sign`() {
        val locale = Locale("es", "CO")

        assertThat(signedMoney(600_000_000L, locale)).isEqualTo("+ $ 6.000.000")
        assertThat(signedMoney(-200_000_000L, locale)).isEqualTo("− $ 2.000.000")
        assertThat(signedMoney(0L, locale)).isEqualTo("$ 0")
    }

    @Test
    fun `balance x labels use days and month letters`() {
        val locale = Locale("es", "CO")

        assertThat(balanceXLabels(BalanceGranularity.DAILY, YearMonth.of(2026, 10), 31, locale))
            .containsExactly(0 to "1", 14 to "15", 30 to "31")
            .inOrder()
        val monthly = balanceXLabels(BalanceGranularity.MONTHLY, YearMonth.of(2026, 10), 12, locale)
        assertThat(monthly).hasSize(12)
        assertThat(monthly[0]).isEqualTo(0 to "E")
        assertThat(monthly[9]).isEqualTo(9 to "O")
    }

    @Test
    fun `balance point date uses short day and full month`() {
        val locale = Locale("es", "CO")

        assertThat(pointDateLabel(LocalDate.of(2026, 10, 3), BalanceGranularity.DAILY, locale)).isEqualTo("3 oct")
        assertThat(pointDateLabel(LocalDate.of(2026, 10, 15), BalanceGranularity.MONTHLY, locale)).isEqualTo("octubre")
    }
}
