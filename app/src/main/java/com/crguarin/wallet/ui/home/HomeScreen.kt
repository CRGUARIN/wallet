package com.crguarin.wallet.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.LoadingState
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
    onUpcomingRecurringClick: (Long) -> Unit = {},
    onUpcomingPendingClick: () -> Unit = {},
    onUpcomingCardClick: (Long) -> Unit = {},
    onViewAllRecurring: () -> Unit = {},
    onViewDebts: () -> Unit = {},
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
            onUpcomingRecurringClick = onUpcomingRecurringClick,
            onUpcomingPendingClick = onUpcomingPendingClick,
            onUpcomingCardClick = onUpcomingCardClick,
            onViewAllRecurring = onViewAllRecurring,
            onViewDebts = onViewDebts,
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
        onUpcomingRecurringClick = onUpcomingRecurringClick,
        onUpcomingPendingClick = onUpcomingPendingClick,
        onUpcomingCardClick = onUpcomingCardClick,
        onViewAllRecurring = onViewAllRecurring,
        onViewDebts = onViewDebts,
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
    onUpcomingRecurringClick: (Long) -> Unit = {},
    onUpcomingPendingClick: () -> Unit = {},
    onUpcomingCardClick: (Long) -> Unit = {},
    onViewAllRecurring: () -> Unit = {},
    onViewDebts: () -> Unit = {},
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

            if (uiState.upcomingPayments.isNotEmpty()) {
                item(key = "upcoming_payments") {
                    UpcomingPaymentsCard(
                        payments = uiState.upcomingPayments,
                        today = uiState.today,
                        onUpcomingRecurringClick = onUpcomingRecurringClick,
                        onUpcomingPendingClick = onUpcomingPendingClick,
                        onUpcomingCardClick = onUpcomingCardClick,
                        onViewAllRecurring = onViewAllRecurring,
                    )
                }
            }

            val debts = uiState.debts
            if (debts != null && debts.hasActive) {
                item(key = "debts") {
                    HomeDebtsCard(
                        debts = debts,
                        onViewDebts = onViewDebts,
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
