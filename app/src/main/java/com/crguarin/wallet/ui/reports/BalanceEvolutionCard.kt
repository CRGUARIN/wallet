package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceGranularity
import com.crguarin.wallet.domain.model.report.BalancePoint
import com.crguarin.wallet.domain.model.report.ReportFocus
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.reports.charts.BalanceLineChart
import com.crguarin.wallet.ui.theme.WalletTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Balance over time: the closing balance of each day (MONTH mode) or each month (YEAR mode),
 * with the opening/closing amounts and a tappable point. The last point is selected by default.
 */
@Composable
internal fun BalanceEvolutionCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier,
) {
    val evolution = uiState.balanceEvolution ?: return
    val locale = LocalConfiguration.current.locales[0]
    val points = evolution.points
    // The period of the drawn data: while a new selection loads (isRefreshing) the state still
    // holds the previous evolution, so slots and labels must not come from uiState.yearMonth.
    val dataMonth = points.firstOrNull()?.date?.let(YearMonth::from) ?: uiState.yearMonth
    val selectionKey =
        "${uiState.mode.name}:${uiState.yearMonth}:${uiState.accountId}:${uiState.focus.name}"
    // NO_SELECTION follows the last point, also when the new period's data arrives after the key
    // changed.
    var selectedIndex by rememberSaveable(selectionKey) { mutableIntStateOf(NO_SELECTION) }
    val lastIndex = (points.size - 1).coerceAtLeast(0)
    val safeIndex = if (selectedIndex == NO_SELECTION) lastIndex else selectedIndex.coerceIn(0, lastIndex)
    val selectedPoint = points.getOrNull(safeIndex)

    val slotCount =
        when (evolution.granularity) {
            BalanceGranularity.DAILY -> dataMonth.lengthOfMonth()
            BalanceGranularity.MONTHLY -> 12
        }
    val range = remember(evolution) { ReportsChartMapper.balanceAxisRange(evolution) }
    val values = remember(points, range) { ReportsChartMapper.balancePositions(points, range) }
    val zeroFraction = if (range.minMinorUnits < 0L) range.fractionOf(0L) else null
    val xLabels =
        remember(evolution.granularity, dataMonth, locale) {
            balanceXLabels(evolution.granularity, dataMonth, slotCount, locale)
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reports_balance_evolution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                if (selectedPoint != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.reports_balance_point,
                                pointDateLabel(selectedPoint.date, evolution.granularity, locale),
                                formattedMoney(selectedPoint.balance, locale),
                            ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OpeningClosingColumn(uiState = uiState, evolution = evolution, locale = locale)
            }
            Spacer(modifier = Modifier.height(12.dp))
            BalanceLineChart(
                values = values,
                slotCount = slotCount,
                zeroFraction = zeroFraction,
                xLabels = xLabels,
                yLabels = range.ticksMinorUnits.map { compactMoney(it) },
                lineColor = MaterialTheme.colorScheme.primary,
                selectedIndex = selectedPoint?.let { safeIndex },
                onPointSelected = { selectedIndex = it },
                contentDescription = balanceDescription(evolution, locale),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
            )
        }
    }
}

@Composable
private fun OpeningClosingColumn(
    uiState: ReportsUiState,
    evolution: BalanceEvolution,
    locale: Locale,
) {
    val isCurrentPeriod =
        when (uiState.mode) {
            ReportMode.MONTH -> uiState.yearMonth == YearMonth.from(uiState.today)
            ReportMode.YEAR -> uiState.yearMonth.year == uiState.today.year
        }
    val closingRes =
        if (isCurrentPeriod) R.string.reports_balance_current else R.string.reports_balance_closing
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "${stringResource(R.string.reports_balance_opening)} ${formattedMoney(evolution.opening, locale)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${stringResource(closingRes)} ${formattedMoney(evolution.closing, locale)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

private const val NO_SELECTION = -1

/**
 * X axis labels as (slot index, label): days 1, 15 and last in DAILY mode, the narrow month
 * letter of every month in MONTHLY mode.
 */
internal fun balanceXLabels(
    granularity: BalanceGranularity,
    yearMonth: YearMonth,
    slotCount: Int,
    locale: Locale,
): List<Pair<Int, String>> =
    when (granularity) {
        BalanceGranularity.DAILY ->
            listOf(0 to "1", 14 to "15", (slotCount - 1).coerceAtLeast(0) to "$slotCount").distinctBy { it.first }
        BalanceGranularity.MONTHLY ->
            (1..12).map { month -> (month - 1) to monthNarrowLetter(YearMonth.of(yearMonth.year, month), locale) }
    }

/** Point date label: short day ("3 oct") in DAILY mode, the month name in MONTHLY mode. */
internal fun pointDateLabel(
    date: LocalDate,
    granularity: BalanceGranularity,
    locale: Locale,
): String =
    when (granularity) {
        // Some JDK/ICU versions abbreviate Spanish months with a trailing dot ("oct."); drop it
        // so the label matches the rest of the app on every runtime.
        BalanceGranularity.DAILY ->
            "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")}"
        BalanceGranularity.MONTHLY -> monthFullName(YearMonth.from(date), locale)
    }

/** Accessibility summary of the balance chart: opening, closing, minimum and maximum. */
@Composable
private fun balanceDescription(
    evolution: BalanceEvolution,
    locale: Locale,
): String {
    val balances = listOf(evolution.opening.minorUnits) + evolution.points.map { it.balance.minorUnits }
    return stringResource(
        R.string.reports_cd_balance_line,
        formattedMoney(evolution.opening, locale),
        formattedMoney(evolution.closing, locale),
        formattedMoney(Money(balances.minOrNull() ?: 0L), locale),
        formattedMoney(Money(balances.maxOrNull() ?: 0L), locale),
    )
}

private fun balancePreviewState(): ReportsUiState {
    val yearMonth = YearMonth.of(2026, 10)
    val points =
        (1..15).map { day ->
            BalancePoint(
                date = LocalDate.of(2026, 10, day),
                balance = Money(50_000_000L + day * 2_000_000L),
            )
        }
    return ReportsUiState(
        isLoading = false,
        mode = ReportMode.MONTH,
        yearMonth = yearMonth,
        today = LocalDate.of(2026, 10, 15),
        focus = ReportFocus.BALANCE,
        balanceEvolution =
            BalanceEvolution(
                granularity = BalanceGranularity.DAILY,
                opening = Money(50_000_000L),
                points = points,
            ),
    )
}

@Preview(name = "Balance Evolution Light", showBackground = true)
@Composable
private fun BalanceEvolutionCardPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        BalanceEvolutionCard(uiState = balancePreviewState())
    }
}

@Preview(name = "Balance Evolution Dark", showBackground = true)
@Composable
private fun BalanceEvolutionCardPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        BalanceEvolutionCard(uiState = balancePreviewState())
    }
}

@Preview(name = "Balance Evolution Font Scale 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun BalanceEvolutionCardPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        BalanceEvolutionCard(uiState = balancePreviewState())
    }
}
