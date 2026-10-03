package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.ui.reports.charts.CumulativeLineChart
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.time.YearMonth
import java.util.Locale

/**
 * Month pace: cumulative spending (or income) of the selected month against the same days of the
 * previous month, with a one-line summary. Only shown in MONTH mode.
 */
@Composable
internal fun SpendingPaceCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier,
) {
    val pace = uiState.pace ?: return
    val locale = LocalConfiguration.current.locales[0]
    val semanticColors = MaterialTheme.walletSemanticColors
    val currentColor =
        when (uiState.type) {
            TransactionType.INCOME -> semanticColors.income
            else -> semanticColors.expense
        }
    val axisMax =
        remember(pace) {
            val seriesMax =
                maxOf(
                    pace.current.maxOfOrNull { it.minorUnits } ?: 0L,
                    pace.previous.maxOfOrNull { it.minorUnits } ?: 0L,
                )
            AxisMath.niceAxisMax(seriesMax)
        }
    val (current, previous) = remember(pace, axisMax) { ReportsChartMapper.cumulativeFractions(pace, axisMax) }
    val xLabels = remember(pace) { listOf(1, 15, pace.daysInMonth).distinct().map { day -> day to day.toString() } }
    val phrase = pacePhrase(uiState, pace, locale)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reports_spending_pace),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            CumulativeLineChart(
                current = current,
                previous = previous,
                daysInMonth = pace.daysInMonth,
                currentColor = currentColor,
                xLabels = xLabels,
                yLabels = ReportsChartMapper.yAxisMinorUnits(axisMax).map { compactMoney(it) },
                contentDescription = stringResource(R.string.reports_cd_line, phrase),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = phrase,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Summary phrase: current month ("so far") or past month ("you ended the month with ..."). */
@Composable
private fun pacePhrase(
    uiState: ReportsUiState,
    pace: SpendingPace,
    locale: Locale,
): String {
    val isCurrentMonth = uiState.yearMonth == YearMonth.from(uiState.today)
    val amount = formattedMoney(pace.currentAtDay, locale)
    val change = pace.changePercent
    return when {
        change == null || change == 0 ->
            stringResource(
                if (isCurrentMonth) R.string.reports_pace_current_plain else R.string.reports_pace_past_plain,
                amount,
            )
        change > 0 ->
            stringResource(
                if (isCurrentMonth) R.string.reports_pace_current_more else R.string.reports_pace_past_more,
                amount,
                change,
            )
        else ->
            stringResource(
                if (isCurrentMonth) R.string.reports_pace_current_less else R.string.reports_pace_past_less,
                amount,
                -change,
            )
    }
}
