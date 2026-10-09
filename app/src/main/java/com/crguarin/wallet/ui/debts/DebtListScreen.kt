package com.crguarin.wallet.ui.debts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.ui.accounts.AddFab
import com.crguarin.wallet.ui.accounts.TonalCard
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate

/**
 * Debts list. The navigation graph calls [DebtListRoute] with [onNavigateBack], [onAddClick]
 * and [onDebtClick].
 */
@Composable
fun DebtListRoute(
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onDebtClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtListViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        DebtListScreen(
            uiState = DebtListUiState(isLoading = false),
            onNavigateBack = onNavigateBack,
            onAddClick = onAddClick,
            onDebtClick = onDebtClick,
            onToggleSettled = {},
            modifier = modifier,
        )
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DebtListScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddClick = onAddClick,
        onDebtClick = onDebtClick,
        onToggleSettled = viewModel::toggleSettledExpanded,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    uiState: DebtListUiState,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onToggleSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.debts_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AddFab(
                contentDescription = stringResource(R.string.debts_add),
                onClick = onAddClick,
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val overview = uiState.overview
        if (overview == null || overview.isEmpty) {
            EmptyState(
                icon = Icons.Default.Payments,
                title = stringResource(R.string.debts_empty_title),
                message = stringResource(R.string.debts_empty_message),
                actionLabel = stringResource(R.string.debts_add),
                onActionClick = onAddClick,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            val spacing = MaterialTheme.walletSpacing
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = spacing.screenGutter),
            ) {
                item(key = "summary") {
                    DebtsSummaryCard(
                        overview = overview,
                        modifier = Modifier.padding(vertical = spacing.s),
                    )
                }
                if (overview.owedToMe.isNotEmpty()) {
                    item(key = "owed_header") {
                        SectionHeader(
                            text = stringResource(R.string.debts_total_owed_to_me),
                            modifier = Modifier.padding(top = spacing.l, bottom = spacing.s),
                        )
                    }
                    debtRows(
                        keyPrefix = "owed",
                        summaries = overview.owedToMe,
                        today = uiState.today,
                        onDebtClick = onDebtClick,
                    )
                }
                if (overview.iOwe.isNotEmpty()) {
                    item(key = "i_owe_header") {
                        SectionHeader(
                            text = stringResource(R.string.debts_total_i_owe),
                            modifier = Modifier.padding(top = spacing.l, bottom = spacing.s),
                        )
                    }
                    debtRows(
                        keyPrefix = "i_owe",
                        summaries = overview.iOwe,
                        today = uiState.today,
                        onDebtClick = onDebtClick,
                    )
                }
                if (overview.settled.isNotEmpty()) {
                    item(key = "settled_header") {
                        SettledHeader(
                            expanded = uiState.settledExpanded,
                            onClick = onToggleSettled,
                            modifier = Modifier.padding(top = spacing.l, bottom = spacing.s),
                        )
                    }
                    if (uiState.settledExpanded) {
                        debtRows(
                            keyPrefix = "settled",
                            summaries = overview.settled,
                            today = uiState.today,
                            onDebtClick = onDebtClick,
                        )
                    }
                }
                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(FAB_CLEARANCE))
                }
            }
        }
    }
}

/** One card per debt. Keys are prefixed so a debt listed twice (never the case) would not clash. */
private fun LazyListScope.debtRows(
    keyPrefix: String,
    summaries: List<DebtSummary>,
    today: LocalDate,
    onDebtClick: (Long) -> Unit,
) {
    items(items = summaries, key = { summary -> "${keyPrefix}_${summary.debt.id}" }) { summary ->
        DebtRow(
            summary = summary,
            today = today,
            onClick = { onDebtClick(summary.debt.id) },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/** Room under the list so the last row is not covered by the add button. */
private val FAB_CLEARANCE = 88.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebtsSummaryCard(
    overview: DebtsOverview,
    modifier: Modifier = Modifier,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val net = overview.net
    val netType: TransactionType? =
        when {
            net.isPositive -> TransactionType.INCOME
            net.isNegative -> TransactionType.EXPENSE
            else -> null
        }
    SectionCard(modifier = modifier) {
        // FlowRow so the three amounts wrap instead of overlapping with large font scales.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.s),
        ) {
            SummaryAmount(
                label = stringResource(R.string.debts_total_owed_to_me),
                amount = overview.totalOwedToMe,
                color = semanticColors.income,
            )
            SummaryAmount(
                label = stringResource(R.string.debts_total_i_owe),
                amount = overview.totalIOwe,
                color = semanticColors.expense,
            )
            // The balance is signed by its type: "+" when debts owed to me outweigh mine.
            SummaryAmount(
                label = stringResource(R.string.debts_total_net),
                amount = if (net.isNegative) -net else net,
                type = netType,
            )
        }
    }
}

@Composable
private fun SummaryAmount(
    label: String,
    amount: Money,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    type: TransactionType? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            color = color,
            style = MaterialTheme.walletTypography.rowAmount,
        )
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/** Opens and closes the settled section, which starts collapsed. */
@Composable
private fun SettledHeader(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.debts_section_settled),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription =
                stringResource(
                    if (expanded) R.string.action_collapse else R.string.action_expand,
                ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DebtRow(
    summary: DebtSummary,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val debt = summary.debt
    TonalCard(onClick = onClick, modifier = modifier) {
        Column {
            Text(
                text = debt.personName,
                style = MaterialTheme.typography.titleMedium,
            )
            debt.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (summary.isSettled) {
                // Nothing is left to pay, so the row shows what the debt was and that it is closed.
                MoneyText(
                    amount = debt.amount,
                    style = MaterialTheme.walletTypography.rowAmount,
                )
                Text(
                    text = stringResource(R.string.debts_settled_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                MoneyText(
                    amount = summary.remaining,
                    style = MaterialTheme.walletTypography.rowAmount,
                )
                if (summary.paid.isPositive) {
                    Text(
                        text =
                            stringResource(
                                R.string.debts_remaining_of,
                                summary.remaining.formatDisplay(locale),
                                debt.amount.formatDisplay(locale),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            debt.dueDate?.let { dueDate ->
                if (summary.isOverdue(today)) {
                    Text(
                        text = stringResource(R.string.debts_overdue),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.debts_due_on, dueDate.formatMedium()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val previewInstant: Instant = Instant.EPOCH
private val previewToday: LocalDate = LocalDate.of(2026, 10, 4)

private fun previewDebtSummary(
    id: Long,
    name: String,
    direction: DebtDirection,
    amount: Long,
    paid: Long = 0L,
    dueDate: LocalDate? = null,
    note: String? = null,
): DebtSummary =
    DebtSummary(
        debt =
            Debt(
                id = id,
                personName = name,
                direction = direction,
                amount = Money(amount),
                date = LocalDate.of(2026, 9, 1),
                dueDate = dueDate,
                note = note,
                createdAt = previewInstant,
                updatedAt = previewInstant,
            ),
        paid = Money(paid),
    )

private fun previewDebtListState(): DebtListUiState =
    DebtListUiState(
        isLoading = false,
        today = previewToday,
        overview =
            DebtsOverview(
                owedToMe =
                    listOf(
                        previewDebtSummary(1L, "Ana", DebtDirection.OWED_TO_ME, 5_000_000L, note = "Préstamo"),
                    ),
                iOwe =
                    listOf(
                        previewDebtSummary(
                            id = 2L,
                            name = "Luis",
                            direction = DebtDirection.I_OWE,
                            amount = 2_000_000L,
                            paid = 500_000L,
                            dueDate = LocalDate.of(2026, 9, 30),
                        ),
                    ),
                settled =
                    listOf(
                        previewDebtSummary(3L, "Marta", DebtDirection.OWED_TO_ME, 300_000L, paid = 300_000L),
                    ),
                totalOwedToMe = Money(5_000_000L),
                totalIOwe = Money(1_500_000L),
            ),
    )

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun DebtListScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        DebtListScreen(
            uiState = previewDebtListState(),
            onNavigateBack = {},
            onAddClick = {},
            onDebtClick = {},
            onToggleSettled = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DebtListScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        DebtListScreen(
            uiState = previewDebtListState(),
            onNavigateBack = {},
            onAddClick = {},
            onDebtClick = {},
            onToggleSettled = {},
        )
    }
}
