package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.report.ReportFocus
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.accounts.WalletFilterChip
import com.crguarin.wallet.ui.accounts.WalletSegmentedControl
import com.crguarin.wallet.ui.components.PeriodSelector
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.formatMonthYear

/** From this font scale up the mode and the account filter stack, so neither gets a clipped label. */
private const val STACKED_FONT_SCALE = 1.3f

/**
 * Top block of the Reports screen, three compact rows: the mode (month/year) beside the account
 * filter, the period selector and the focus (expenses/income/balance). Every control reports
 * through a lambda; the selection itself lives in the state.
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
    val spacing = MaterialTheme.walletSpacing
    val stacked = LocalDensity.current.fontScale >= STACKED_FONT_SCALE
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val modeControl: @Composable (Modifier) -> Unit = { controlModifier ->
            WalletSegmentedControl(
                options = ReportMode.entries,
                selected = uiState.mode,
                onSelect = onModeChange,
                label = { mode -> stringResource(mode.labelRes()) },
                modifier = controlModifier,
            )
        }
        val accountFilter: @Composable (Modifier) -> Unit = { chipModifier ->
            WalletFilterChip(
                selected = uiState.accountId != null,
                onClick = onOpenAccountSheet,
                label = uiState.selectedAccountName ?: stringResource(R.string.transactions_filter_all_accounts),
                modifier = chipModifier,
            )
        }
        if (stacked) {
            modeControl(Modifier)
            accountFilter(Modifier)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                modeControl(Modifier.weight(1f))
                accountFilter(Modifier.weight(1f))
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

        WalletSegmentedControl(
            options = ReportFocus.entries,
            selected = uiState.focus,
            onSelect = onFocusChange,
            label = { focus -> stringResource(focus.labelRes()) },
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
