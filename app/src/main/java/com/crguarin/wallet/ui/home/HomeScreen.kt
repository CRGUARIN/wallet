package com.crguarin.wallet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.accounts.CardRowBalance
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate

@Composable
fun HomeRoute(
    onAccountClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: () -> Unit,
    onQuickAction: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    onViewReports: () -> Unit = {},
    viewModel: HomeViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        HomeScreen(
            uiState = HomeUiState(isLoading = false),
            onAccountClick = onAccountClick,
            onTransactionClick = onTransactionClick,
            onViewAllTransactions = onViewAllTransactions,
            onQuickAction = onQuickAction,
            onViewReports = onViewReports,
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAccountClick = onAccountClick,
        onTransactionClick = onTransactionClick,
        onViewAllTransactions = onViewAllTransactions,
        onQuickAction = onQuickAction,
        onViewReports = onViewReports,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAccountClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: () -> Unit,
    onQuickAction: (TransactionType) -> Unit,
    onViewReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.home_title)) })
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "total_balance") {
                TotalBalanceCard(totalBalance = uiState.totalBalance)
            }

            item(key = "month_summary") {
                MonthSummaryCard(
                    monthSummary = uiState.monthSummary,
                    onViewReports = onViewReports,
                )
            }

            if (uiState.accounts.isNotEmpty()) {
                item(key = "accounts_header") {
                    SectionHeader(title = stringResource(R.string.accounts_title))
                }
                item(key = "accounts_list") {
                    AccountsRow(
                        accounts = uiState.accounts,
                        onAccountClick = onAccountClick,
                        cardOverviews = uiState.cardOverviews,
                        today = uiState.today,
                    )
                }
            }

            item(key = "quick_actions") {
                QuickActions(
                    canTransfer = uiState.canTransfer,
                    onQuickAction = onQuickAction,
                )
            }

            item(key = "recent_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader(
                        title = stringResource(R.string.home_recent_transactions),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }

            if (uiState.hasNoTransactions) {
                item(key = "recent_empty") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(
                    items = uiState.recentTransactions,
                    key = { it.transaction.id },
                    contentType = { "home_transaction" },
                ) { details ->
                    TransactionListItem(
                        details = details,
                        onClick = { onTransactionClick(details.transaction.id) },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(totalBalance: Money) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.accounts_total_balance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            MoneyText(
                amount = totalBalance,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthSummaryCard(
    monthSummary: MonthSummary,
    onViewReports: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.account_detail_month_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            // FlowRow so the three amounts wrap instead of overlapping with large font scales.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryColumn(
                    label = stringResource(R.string.account_detail_income),
                    amount = monthSummary.income,
                    type = TransactionType.INCOME,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_expense),
                    amount = monthSummary.expense,
                    type = TransactionType.EXPENSE,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_net),
                    amount = monthSummary.net,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onViewReports,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.home_view_reports))
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: Money,
    type: TransactionType? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
internal fun AccountsRow(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
) {
    // A plain scrollable Row (few accounts) so every tile takes the height of the tallest one:
    // card tiles show an extra "available" line.
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min),
    ) {
        accounts.forEach { item ->
            val cardOverview = cardOverviews[item.account.id]
            OutlinedCard(
                onClick = { onAccountClick(item.account.id) },
                shape = RoundedCornerShape(16.dp),
                modifier =
                    Modifier
                        .width(170.dp)
                        .fillMaxHeight(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(item.account.color)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = IconCatalog.get(item.account.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (item.account.type == AccountType.CREDIT_CARD && cardOverview != null) {
                        CardRowBalance(
                            overview = cardOverview,
                            today = today,
                            horizontalAlignment = Alignment.Start,
                        )
                    } else {
                        MoneyText(
                            amount = item.balance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private const val STACKED_FONT_SCALE = 1.3f

@Composable
private fun QuickActions(
    canTransfer: Boolean,
    onQuickAction: (TransactionType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.home_quick_actions))
        // Large font scales do not fit three buttons side by side: stack them instead.
        val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
        val buttons: @Composable (Modifier) -> Unit = { buttonModifier ->
            QuickActionButton(
                label = stringResource(R.string.type_expense),
                icon = Icons.Default.ArrowDownward,
                onClick = { onQuickAction(TransactionType.EXPENSE) },
                modifier = buttonModifier,
            )
            QuickActionButton(
                label = stringResource(R.string.type_income),
                icon = Icons.Default.ArrowUpward,
                onClick = { onQuickAction(TransactionType.INCOME) },
                modifier = buttonModifier,
            )
            if (canTransfer) {
                QuickActionButton(
                    label = stringResource(R.string.type_transfer),
                    icon = Icons.Default.SwapHoriz,
                    onClick = { onQuickAction(TransactionType.TRANSFER) },
                    modifier = buttonModifier,
                )
            }
        }
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val previewInstant: Instant = Instant.EPOCH
private val previewDate: LocalDate = LocalDate.of(2026, 9, 28)

private fun previewAccount(
    id: Long,
    name: String,
    balance: Money,
): AccountWithBalance =
    AccountWithBalance(
        account =
            Account(
                id = id,
                name = name,
                type = AccountType.CASH,
                initialBalance = balance,
                color = 0xFF66BB6A.toInt(),
                icon = "wallet",
                sortOrder = id.toInt(),
                createdAt = previewInstant,
            ),
        balance = balance,
    )

private fun previewTransaction(
    id: Long,
    type: TransactionType,
    systemKey: String?,
): TransactionDetails =
    TransactionDetails(
        transaction =
            Transaction(
                id = id,
                type = type,
                amount = Money(4_550_000L),
                accountId = 1L,
                categoryId = if (type == TransactionType.TRANSFER) null else 10L,
                date = previewDate,
                createdAt = previewInstant,
                updatedAt = previewInstant,
            ),
        accountName = "Efectivo",
        accountColor = 0xFF66BB6A.toInt(),
        accountIcon = "wallet",
        toAccountName = if (type == TransactionType.TRANSFER) "Ahorros" else null,
        categorySystemKey = systemKey,
        categoryColor = if (type == TransactionType.TRANSFER) null else 0xFFEF5350.toInt(),
        categoryIcon = if (type == TransactionType.TRANSFER) null else "restaurant",
    )

private fun previewState(): HomeUiState =
    HomeUiState(
        isLoading = false,
        totalBalance = Money(12_500_000L),
        monthSummary = MonthSummary(income = Money(5_000_000L), expense = Money(2_300_000L)),
        accounts =
            listOf(
                previewAccount(1L, "Efectivo", Money(4_500_000L)),
                previewAccount(2L, "Ahorros", Money(8_000_000L)),
            ),
        recentTransactions =
            listOf(
                previewTransaction(1L, TransactionType.EXPENSE, "food"),
                previewTransaction(2L, TransactionType.INCOME, "salary"),
                previewTransaction(3L, TransactionType.TRANSFER, null),
            ),
    )

@Preview(name = "Home Light", showBackground = true)
@Composable
private fun HomeScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        HomeScreen(
            uiState = previewState(),
            onAccountClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onQuickAction = {},
            onViewReports = {},
        )
    }
}

@Preview(name = "Home Font Scale 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun HomeScreenPreviewLargeFont() {
    WalletTheme(darkTheme = false) {
        HomeScreen(
            uiState = previewState(),
            onAccountClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onQuickAction = {},
            onViewReports = {},
        )
    }
}

@Preview(name = "Home Dark", showBackground = true)
@Composable
private fun HomeScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        HomeScreen(
            uiState = previewState(),
            onAccountClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onQuickAction = {},
            onViewReports = {},
        )
    }
}
