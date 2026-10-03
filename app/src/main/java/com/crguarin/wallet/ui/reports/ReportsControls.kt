package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.report.ReportFocus
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.components.PeriodSelector
import com.crguarin.wallet.ui.util.formatMonthYear

/**
 * Top block of the Reports screen: mode (month/year), period selector, focus
 * (expenses/income/balance) and account filter. Every control reports through a lambda; the
 * selection itself lives in the state.
 */
@Composable
internal fun ReportsControls(
    uiState: ReportsUiState,
    onModeChange: (ReportMode) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onFocusChange: (ReportFocus) -> Unit,
    onOpenAccountSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReportMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = uiState.mode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportMode.entries.size),
                    label = { Text(text = stringResource(mode.labelRes())) },
                )
            }
        }

        PeriodSelector(
            label =
                when (uiState.mode) {
                    ReportMode.MONTH -> uiState.yearMonth.formatMonthYear(locale)
                    ReportMode.YEAR -> uiState.yearMonth.year.toString()
                },
            canGoPrevious = true,
            canGoNext = uiState.canGoNext,
            onPrevious = onPreviousPeriod,
            onNext = onNextPeriod,
            previousContentDescription =
                stringResource(
                    if (uiState.mode == ReportMode.MONTH) {
                        R.string.transactions_previous_month
                    } else {
                        R.string.reports_previous_year
                    },
                ),
            nextContentDescription =
                stringResource(
                    if (uiState.mode == ReportMode.MONTH) {
                        R.string.transactions_next_month
                    } else {
                        R.string.reports_next_year
                    },
                ),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReportFocus.entries.forEachIndexed { index, focus ->
                SegmentedButton(
                    selected = uiState.focus == focus,
                    onClick = { onFocusChange(focus) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportFocus.entries.size),
                    label = {
                        Text(
                            text = stringResource(focus.labelRes()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        FilterChip(
            selected = uiState.accountId != null,
            onClick = onOpenAccountSheet,
            label = {
                Text(
                    text = uiState.selectedAccountName ?: stringResource(R.string.transactions_filter_all_accounts),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

private fun ReportMode.labelRes(): Int =
    when (this) {
        ReportMode.MONTH -> R.string.reports_mode_month
        ReportMode.YEAR -> R.string.reports_mode_year
    }

private fun ReportFocus.labelRes(): Int =
    when (this) {
        ReportFocus.EXPENSE -> R.string.type_expense
        ReportFocus.INCOME -> R.string.type_income
        ReportFocus.BALANCE -> R.string.reports_focus_balance
    }
