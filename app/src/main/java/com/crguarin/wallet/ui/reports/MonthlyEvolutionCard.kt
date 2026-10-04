package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.reports.charts.BarGroup
import com.crguarin.wallet.ui.reports.charts.GroupedBarChart
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Monthly evolution: income and expense bars per month with a dashed average-expense line. Tapping
 * a group selects that month through [onSelectMonth].
 */
@Composable
internal fun MonthlyEvolutionCard(
    uiState: ReportsUiState,
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val semanticColors = MaterialTheme.walletSemanticColors
    val months = uiState.monthlyTotals
    val refreshing = uiState.isRefreshing
    val todayMonth = YearMonth.from(uiState.today)
    // The range covers bars and nets; positions come from the chart mapper.
    val range = remember(months) { ReportsChartMapper.evolutionAxisRange(months) }
    val fractions = remember(months, range) { ReportsChartMapper.barPositions(months, range) }
    val baseline = range.fractionOf(0L)
    // Future months are drawn empty and dimmed; they do not count for the average either.
    val lastRelevantMonth = months.lastOrNull()?.yearMonth?.let { minOf(it, todayMonth) }
    val lineValues =
        remember(months, range, lastRelevantMonth) {
            ReportsChartMapper.netLinePositions(months, range, lastRelevantMonth)
        }
    val linePointColors =
        remember(months, semanticColors) {
            months.map { month ->
                if (month.income.minorUnits - month.expense.minorUnits < 0L) {
                    semanticColors.expense
                } else {
                    semanticColors.income
                }
            }
        }
    val average =
        remember(months, range, lastRelevantMonth) {
            lastRelevantMonth?.let { ReportsChartMapper.averageExpensePosition(months, range, it) }
        }
    val (selectedIndex, dimmedFlags) =
        remember(months, uiState.mode, uiState.yearMonth, todayMonth) {
            evolutionSelection(months, uiState.mode, uiState.yearMonth, todayMonth)
        }
    val groups =
        remember(months, fractions, dimmedFlags, locale, semanticColors) {
            months.mapIndexed { index, month ->
                BarGroup(
                    label = monthAbbreviation(month.yearMonth, locale),
                    narrowLabel = monthNarrowLetter(month.yearMonth, locale),
                    values = fractions.getOrElse(index) { listOf(0f, 0f) },
                    colors = listOf(semanticColors.income, semanticColors.expense),
                    dimmed = dimmedFlags.getOrElse(index) { false },
                )
            }
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reports_monthly_evolution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(4.dp))
            MonthlyLegend(
                incomeColor = semanticColors.income,
                expenseColor = semanticColors.expense,
                netColor = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GroupedBarChart(
                groups = groups,
                selectedIndex = selectedIndex,
                averageFraction = average,
                yLabels = range.ticksMinorUnits.map { compactMoney(it) },
                baselineFraction = baseline,
                lineValues = lineValues,
                lineColor = MaterialTheme.colorScheme.onSurface,
                linePointColors = linePointColors,
                onGroupClick = { index ->
                    if (!refreshing) {
                        months
                            .getOrNull(index)
                            ?.takeIf { it.yearMonth <= todayMonth }
                            ?.let { onSelectMonth(it.yearMonth) }
                    }
                },
                contentDescription = barsDescription(months, locale),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyLegend(
    incomeColor: Color,
    expenseColor: Color,
    netColor: Color,
) {
    // FlowRow: with four entries and large font scales the legend wraps instead of squeezing the
    // last entry into a one-letter-wide column.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(color = incomeColor, label = stringResource(R.string.account_detail_income))
        LegendDot(color = expenseColor, label = stringResource(R.string.account_detail_expense))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(netColor),
                )
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(netColor),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.reports_net),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.reports_average),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Accessibility summary of the bar chart, one entry per month, with the signed net. */
@Composable
private fun barsDescription(
    months: List<MonthTotals>,
    locale: Locale,
): String {
    val separator = stringResource(R.string.reports_list_separator)
    val incomeLabel = stringResource(R.string.account_detail_income)
    val expenseLabel = stringResource(R.string.account_detail_expense)
    val netLabel = stringResource(R.string.reports_net)
    val parts = ArrayList<String>(months.size)
    for (month in months) {
        val income = stringResource(R.string.reports_cd_item, incomeLabel, formattedMoney(month.income, locale))
        val expense = stringResource(R.string.reports_cd_item, expenseLabel, formattedMoney(month.expense, locale))
        val net =
            stringResource(
                R.string.reports_cd_item,
                netLabel,
                signedMoney(month.income.minorUnits - month.expense.minorUnits, locale),
            )
        parts +=
            stringResource(
                R.string.reports_cd_bars_group_net,
                monthFullName(month.yearMonth, locale),
                income,
                expense,
                net,
            )
    }
    return stringResource(R.string.reports_cd_bars, parts.joinToString(separator))
}

/** Compact label of an amount for chart axes, e.g. `12 mil $` / `$12K`. */
@Composable
internal fun compactMoney(minorUnits: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val thousand = stringResource(R.string.reports_compact_thousands)
    val million = stringResource(R.string.reports_compact_millions)
    val billion = stringResource(R.string.reports_compact_billions)
    return remember(minorUnits, locale, thousand, million, billion) {
        CompactMoneyFormatter.formatCompact(minorUnits, locale, thousand, million, billion)
    }
}

/** "oct" / "Oct": abbreviated month name with a capital first letter. */
internal fun monthAbbreviation(
    yearMonth: YearMonth,
    locale: Locale,
): String {
    val name = yearMonth.month.getDisplayName(TextStyle.SHORT, locale)
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** "E" / "F": narrow standalone month letter for crowded year labels, with a capital letter. */
internal fun monthNarrowLetter(
    yearMonth: YearMonth,
    locale: Locale,
): String {
    val name = yearMonth.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** "octubre": full standalone month name, used in accessibility descriptions. */
internal fun monthFullName(
    yearMonth: YearMonth,
    locale: Locale,
): String = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

/**
 * Selected bar and per-group dimming of the monthly evolution.
 *
 * In MONTH mode the selected month is highlighted and the other bars are dimmed; future months are
 * dimmed too. In YEAR mode there is no selected month, so no bar is dimmed.
 *
 * Returns the selected index (null when none) and one dimmed flag per month.
 */
internal fun evolutionSelection(
    months: List<MonthTotals>,
    mode: ReportMode,
    selectedMonth: YearMonth,
    todayMonth: YearMonth,
): Pair<Int?, List<Boolean>> {
    if (mode == ReportMode.YEAR) {
        return null to List(months.size) { false }
    }
    val selectedIndex = months.indexOfFirst { it.yearMonth == selectedMonth }.takeIf { it >= 0 }
    return selectedIndex to months.map { it.yearMonth > todayMonth }
}

internal fun formattedMoney(
    money: Money,
    locale: Locale,
): String =
    MoneyFormatter
        .format(money, locale)
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')

/**
 * Signed amount for accessibility text ("+ $ 6.000" / "− $ 2.000"), with the same minus sign
 * [com.crguarin.wallet.ui.components.MoneyText] uses. Zero has no sign.
 */
internal fun signedMoney(
    minorUnits: Long,
    locale: Locale,
): String {
    val formatted = formattedMoney(Money(kotlin.math.abs(minorUnits)), locale)
    return when {
        minorUnits > 0L -> "+ $formatted"
        minorUnits < 0L -> "− $formatted"
        else -> formatted
    }
}
