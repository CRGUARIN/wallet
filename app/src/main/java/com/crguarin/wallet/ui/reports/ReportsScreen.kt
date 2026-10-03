package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Navigation entry point of the Reports screen. */
@Composable
fun ReportsRoute(
    onViewTransactions: (categoryId: Long?, accountId: Long?, type: TransactionType, yearMonth: YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        ReportsScreen(
            uiState = ReportsUiState(isLoading = false),
            onModeChange = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onTypeChange = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onSelectSlice = {},
            onDrillInto = {},
            onDrillUp = {},
            onSelectMonth = {},
            onViewTransactions = onViewTransactions,
            onRetry = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReportsScreen(
        uiState = uiState,
        onModeChange = viewModel::setMode,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onTypeChange = viewModel::setType,
        onOpenAccountSheet = viewModel::openAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelected = viewModel::setAccount,
        onSelectSlice = viewModel::selectSlice,
        onDrillInto = viewModel::drillInto,
        onDrillUp = viewModel::drillUp,
        onSelectMonth = viewModel::selectMonth,
        onViewTransactions = onViewTransactions,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Stateless Reports screen: controls on top and then the summary, the category donut, the monthly
 * bars and the month pace (MONTH mode only). The empty state is shown below the controls so the
 * period can still be changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.reports_title)) })
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "controls", contentType = "controls") {
                ReportsControls(
                    uiState = uiState,
                    onModeChange = onModeChange,
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod,
                    onTypeChange = onTypeChange,
                    onOpenAccountSheet = onOpenAccountSheet,
                )
            }

            when {
                uiState.isLoading -> {
                    item(key = "loading", contentType = "status") {
                        LoadingState(fillMaxSize = false, modifier = Modifier.fillMaxWidth())
                    }
                }

                uiState.hasError -> {
                    item(key = "error", contentType = "status") {
                        ErrorState(
                            message = stringResource(R.string.reports_load_error),
                            actionLabel = stringResource(R.string.action_retry),
                            onActionClick = onRetry,
                        )
                    }
                }

                uiState.isEmpty -> {
                    item(key = "empty", contentType = "status") {
                        EmptyState(
                            icon = Icons.Default.PieChart,
                            title = stringResource(R.string.reports_empty_period_title),
                            message = stringResource(R.string.reports_empty_period_message),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    item(key = "summary", contentType = "card") {
                        ReportsSummaryCard(uiState = uiState)
                    }
                    item(key = "breakdown", contentType = "card") {
                        CategoryBreakdownCard(
                            uiState = uiState,
                            onSelectSlice = onSelectSlice,
                            onDrillInto = onDrillInto,
                            onDrillUp = onDrillUp,
                            onViewTransactions = onViewTransactions,
                        )
                    }
                    item(key = "evolution", contentType = "card") {
                        MonthlyEvolutionCard(
                            uiState = uiState,
                            onSelectMonth = onSelectMonth,
                        )
                    }
                    if (uiState.mode == ReportMode.MONTH && uiState.pace != null) {
                        item(key = "pace", contentType = "card") {
                            SpendingPaceCard(uiState = uiState)
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            onAccountSelected = { onAccountSelected(it.account.id) },
            onDismissRequest = onDismissAccountSheet,
            allOptionLabel = stringResource(R.string.transactions_filter_all_accounts),
            onAllOptionSelected = { onAccountSelected(null) },
        )
    }
}

private val previewInstant: Instant = Instant.EPOCH
private val previewToday: LocalDate = LocalDate.of(2026, 10, 15)

private fun previewState(): ReportsUiState {
    val month = YearMonth.of(2026, 10)
    val slices =
        listOf(
            CategorySlice(
                key = SliceKey.Category(10L),
                name = "Alimentación",
                systemKey = "food",
                color = 0xFFEF5350.toInt(),
                icon = "restaurant",
                amount = Money(1_125_000L),
                perMille = 450,
                hasChildren = true,
            ),
            CategorySlice(
                key = SliceKey.Category(20L),
                name = "Transporte",
                systemKey = "transport",
                color = 0xFF42A5F5.toInt(),
                icon = "directions_bus",
                amount = Money(500_000L),
                perMille = 200,
                hasChildren = false,
            ),
            CategorySlice(
                key = SliceKey.Others,
                name = null,
                systemKey = null,
                color = null,
                icon = null,
                amount = Money(875_000L),
                perMille = 350,
                hasChildren = false,
            ),
        )
    val months =
        listOf(5, 6, 7, 8, 9, 10).map { monthNumber ->
            MonthTotals(
                yearMonth = YearMonth.of(2026, monthNumber),
                income = Money(2_800_000L + monthNumber * 100_000L),
                expense = Money(2_100_000L + monthNumber * 50_000L),
            )
        }
    val pace =
        SpendingPace(
            yearMonth = month,
            current = (1..15).map { Money(it * 80_000L) },
            previous = (1..30).map { Money(it * 70_000L) },
            daysInMonth = 31,
            comparisonDay = 15,
            currentAtDay = Money(1_200_000L),
            previousAtDay = Money(1_050_000L),
            changePercent = 14,
        )
    return ReportsUiState(
        isLoading = false,
        mode = ReportMode.MONTH,
        yearMonth = month,
        today = previewToday,
        type = TransactionType.EXPENSE,
        accountId = 1L,
        selectedAccountName = "Efectivo",
        accounts =
            listOf(
                AccountWithBalance(
                    account =
                        Account(
                            id = 1L,
                            name = "Efectivo",
                            type = AccountType.CASH,
                            initialBalance = Money(0),
                            color = 0xFF66BB6A.toInt(),
                            icon = "wallet",
                            sortOrder = 0,
                            createdAt = previewInstant,
                        ),
                    balance = Money(4_500_000L),
                ),
            ),
        summary = PeriodSummary(Money(2_500_000L), Money(2_000_000L), 25, Money(166_666L)),
        breakdown = CategoryBreakdown(total = Money(2_500_000L), slices = slices, parentId = null),
        monthlyTotals = months,
        pace = pace,
        canGoNext = false,
    )
}

@Preview(name = "Reports Light", showBackground = true)
@Composable
private fun ReportsScreenPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        ReportsScreenPreview()
    }
}

@Preview(name = "Reports Dark", showBackground = true)
@Composable
private fun ReportsScreenPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        ReportsScreenPreview()
    }
}

@Preview(name = "Reports Font Scale 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun ReportsScreenPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        ReportsScreenPreview()
    }
}

@Composable
private fun ReportsScreenPreview() {
    ReportsScreen(
        uiState = previewState(),
        onModeChange = {},
        onPreviousPeriod = {},
        onNextPeriod = {},
        onTypeChange = {},
        onOpenAccountSheet = {},
        onDismissAccountSheet = {},
        onAccountSelected = {},
        onSelectSlice = {},
        onDrillInto = {},
        onDrillUp = {},
        onSelectMonth = {},
        onViewTransactions = { _, _, _, _ -> },
        onRetry = {},
    )
}
