package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.text.NumberFormat

/**
 * Summary of the selected period: the total, its change against the previous period and the
 * average (per day in MONTH mode, per month in YEAR mode).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportsSummaryCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary
    val semanticColors = MaterialTheme.walletSemanticColors
    val totalColor =
        when (uiState.type) {
            TransactionType.INCOME -> semanticColors.income
            TransactionType.EXPENSE -> semanticColors.expense
            TransactionType.TRANSFER -> semanticColors.transfer
        }
    val change = summary.changePercent
    // Rising expenses are bad and rising income is good; falling is the opposite.
    val changeColor =
        when {
            change == null || change == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
            (change > 0) == (uiState.type == TransactionType.EXPENSE) -> semanticColors.expense
            else -> semanticColors.income
        }

    SectionCard(modifier = modifier) {
        Column {
            MoneyText(
                amount = summary.total,
                style = MaterialTheme.typography.headlineMedium,
                color = totalColor,
                sign = MoneySign.NONE,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // FlowRow so the two data points wrap instead of overlapping with large font scales.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryColumn(
                    label =
                        stringResource(
                            if (uiState.mode == ReportMode.MONTH) {
                                R.string.reports_vs_previous_month
                            } else {
                                R.string.reports_vs_previous_year
                            },
                        ),
                ) {
                    if (change == null) {
                        Text(
                            text = stringResource(R.string.reports_no_previous_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val changeDescription = changeContentDescription(change)
                        Text(
                            text = changeLabel(change),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = changeColor,
                            modifier = Modifier.semantics { contentDescription = changeDescription },
                        )
                    }
                }
                SummaryColumn(
                    label =
                        stringResource(
                            if (uiState.mode == ReportMode.MONTH) {
                                R.string.reports_average_daily
                            } else {
                                R.string.reports_average_monthly
                            },
                        ),
                ) {
                    MoneyText(
                        amount = summary.average,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        sign = MoneySign.NONE,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    value: @Composable () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        value()
    }
}

/** "12 %": localized percent body of a change value, without the arrow. */
@Composable
private fun formattedChangePercent(change: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = remember(locale, change) { NumberFormat.getIntegerInstance(locale).format(kotlin.math.abs(change)) }
    return stringResource(R.string.reports_percent, formatted)
}

/** "▲ 12 %" or "▼ 3 %", using the localized percent format; no arrow when nothing changed. */
@Composable
private fun changeLabel(change: Int): String {
    val percent = formattedChangePercent(change)
    val arrow =
        when {
            change > 0 -> stringResource(R.string.reports_change_up)
            change < 0 -> stringResource(R.string.reports_change_down)
            else -> null
        }
    return if (arrow == null) percent else "$arrow $percent"
}

/**
 * Accessibility description of a change ("sube 12 %" / "baja 3 %"), so TalkBack does not read
 * the arrow glyph. With 0 % it is just the percent.
 */
@Composable
private fun changeContentDescription(change: Int): String {
    val percent = formattedChangePercent(change)
    val up = stringResource(R.string.reports_cd_change_up, percent)
    val down = stringResource(R.string.reports_cd_change_down, percent)
    return when {
        change > 0 -> up
        change < 0 -> down
        else -> percent
    }
}
