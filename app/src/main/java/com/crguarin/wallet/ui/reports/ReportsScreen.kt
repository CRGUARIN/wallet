package com.crguarin.wallet.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.YearMonth

/**
 * Stateless Reports screen: it renders [uiState] and reports every user action through a lambda.
 */
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onModeChange: (ReportMode) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelected: (Long?) -> Unit,
    onSelectSlice: (SliceKey) -> Unit,
    onDrillInto: (Long) -> Unit,
    onDrillUp: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onViewTransactions: (categoryId: Long?, accountId: Long?, type: TransactionType, yearMonth: YearMonth) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TODO("Implemented by subagent D2")
}

/**
 * Navigation entry point of the Reports screen.
 *
 * [onViewTransactions] receives the active filters so the leader can navigate to Movements.
 */
@Composable
fun ReportsRoute(
    onViewTransactions: (categoryId: Long?, accountId: Long?, type: TransactionType, yearMonth: YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel? = safeHiltViewModel(),
) {
    TODO("Implemented by subagent D2")
}
