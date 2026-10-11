package com.crguarin.wallet.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.motion.navEnterExit
import com.crguarin.wallet.ui.motion.raisedSurface
import com.crguarin.wallet.ui.motion.rememberStaggerClock
import com.crguarin.wallet.ui.motion.staggeredEnter
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate

@SuppressLint("LocalContextGetResourceValueCall")
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
    onOpenSettings: () -> Unit = {},
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
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.TripRegistered -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.transit_trip_registered),
                            actionLabel = context.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoTrip(event.transactionId)
                    }
                }
                is HomeEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

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
        onOpenSettings = onOpenSettings,
        onTripClick = viewModel::onTripClick,
        onTripCardSelected = viewModel::onTripCardSelected,
        onDismissTripChooser = viewModel::dismissTripChooser,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Home: the total balance first, then the shortcuts (expense, income, transfer, trip), every
 * account at a glance, what is coming up, the month and the latest movements.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onOpenSettings: () -> Unit = {},
    onTripClick: () -> Unit = {},
    onTripCardSelected: (Long) -> Unit = {},
    onDismissTripChooser: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = MaterialTheme.walletSpacing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            // Lifted above the floating "new movement" button, which would hide the undo action.
            SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = FabClearance))
        },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        // In 3D mode the accounts and the total become the deck; otherwise this is the Home of today.
        val deckMode = depthOn() && uiState.accounts.isNotEmpty()
        val clock = rememberStaggerClock("home")
        val listState = rememberLazyListState()
        val depth = MaterialTheme.walletDepth
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = spacing.screenGutter,
                    end = spacing.screenGutter,
                    top = spacing.screenGutter,
                    // Room for the floating "new movement" button, so the last movement stays reachable.
                    bottom = FabClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            if (deckMode) {
                item(key = "accounts_deck") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                        SectionHeader(
                            title = stringResource(R.string.accounts_title),
                            modifier = Modifier.padding(horizontal = spacing.xs),
                        )
                        AccountDeck(
                            accounts = uiState.accounts,
                            totalBalance = uiState.totalBalance,
                            onAccountClick = onAccountClick,
                            cardOverviews = uiState.cardOverviews,
                            today = uiState.today,
                            scrolling = listState.isScrollInProgress,
                        )
                    }
                }
            } else {
                item(key = "total_balance") {
                    TotalBalanceCard(
                        totalBalance = uiState.totalBalance,
                        modifier = Modifier.navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    )
                }
            }

            item(key = "quick_actions") {
                QuickActions(
                    canTransfer = uiState.canTransfer,
                    canRegisterTrip = uiState.canRegisterTrip,
                    onQuickAction = onQuickAction,
                    onTripClick = onTripClick,
                    modifier =
                        Modifier
                            .staggeredEnter(clock, 0)
                            .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                )
            }

            if (!deckMode && uiState.accounts.isNotEmpty()) {
                item(key = "accounts") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                        SectionHeader(
                            title = stringResource(R.string.accounts_title),
                            modifier = Modifier.padding(horizontal = spacing.xs),
                        )
                        AccountsList(
                            accounts = uiState.accounts,
                            onAccountClick = onAccountClick,
                            cardOverviews = uiState.cardOverviews,
                            today = uiState.today,
                            modifier = Modifier.navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                        )
                    }
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
                        modifier =
                            Modifier
                                .animateItem()
                                .staggeredEnter(clock, 1)
                                .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    )
                }
            }

            item(key = "month_summary") {
                MonthSummaryCard(
                    monthSummary = uiState.monthSummary,
                    onViewReports = onViewReports,
                    modifier =
                        Modifier
                            .staggeredEnter(clock, 2)
                            .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                )
            }

            val debts = uiState.debts
            if (debts != null && debts.hasActive) {
                item(key = "debts") {
                    HomeDebtsCard(
                        debts = debts,
                        onViewDebts = onViewDebts,
                        modifier =
                            Modifier
                                .animateItem()
                                .staggeredEnter(clock, 3)
                                .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    )
                }
            }

            item(key = "recent_header") {
                // FlowRow: with large fonts "Ver todos" moves under the title instead of splitting its words.
                FlowRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .staggeredEnter(clock, 4)
                            .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(
                        title = stringResource(R.string.home_recent_transactions),
                        modifier = Modifier.padding(horizontal = spacing.xs),
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .staggeredEnter(clock, 4)
                                .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    )
                }
            } else {
                item(key = "recent_transactions") {
                    RecentTransactions(
                        transactions = uiState.recentTransactions,
                        onTransactionClick = onTransactionClick,
                        modifier =
                            Modifier
                                .staggeredEnter(clock, 4)
                                .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS),
                    )
                }
            }
        }
    }

    if (uiState.showTripChooser) {
        TripCardSheet(
            cards = uiState.transitCards,
            onCardSelected = onTripCardSelected,
            onDismissRequest = onDismissTripChooser,
        )
    }
}

/**
 * Latest movements on one raised surface, one row after another with no divider lines and no
 * background per row.
 */
@Composable
private fun RecentTransactions(
    transactions: List<TransactionDetails>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .raisedSurface(shape)
                .clip(shape)
                .padding(vertical = MaterialTheme.walletSpacing.s),
    ) {
        transactions.forEach { details ->
            key(details.transaction.id) {
                TransactionListItem(
                    details = details,
                    onClick = { onTransactionClick(details.transaction.id) },
                )
            }
        }
    }
}

/** Exit of the Home blocks towards the account detail. */
private const val CONTENT_EXIT_MILLIS = 250

/** Space the floating action button takes at the bottom end, kept free of snackbars and content. */
private val FabClearance = 72.dp

private val previewInstant: Instant = Instant.EPOCH
private val previewDate: LocalDate = LocalDate.of(2026, 9, 28)

private fun previewAccount(
    id: Long,
    name: String,
    balance: Money,
    type: AccountType = AccountType.CASH,
    icon: String = "wallet",
    color: Int = 0xFF66BB6A.toInt(),
    fare: Money? = null,
): AccountWithBalance =
    AccountWithBalance(
        account =
            Account(
                id = id,
                name = name,
                type = type,
                initialBalance = balance,
                color = color,
                icon = icon,
                sortOrder = id.toInt(),
                fare = fare,
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

private fun previewState(totalBalance: Money = Money(12_500_000L)): HomeUiState =
    HomeUiState(
        isLoading = false,
        totalBalance = totalBalance,
        monthSummary = MonthSummary(income = Money(5_000_000L), expense = Money(2_300_000L)),
        accounts =
            listOf(
                previewAccount(1L, "Efectivo", Money(4_500_000L)),
                previewAccount(2L, "Ahorros", Money(8_000_000L), icon = "savings", color = 0xFF42A5F5.toInt()),
                previewAccount(
                    3L,
                    "Cívica",
                    Money(1_205_000L),
                    type = AccountType.TRANSIT_CARD,
                    icon = "directions_bus",
                    color = 0xFFFFA726.toInt(),
                    fare = Money(355_000L),
                ),
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
            uiState = previewState(totalBalance = Money(99_999_999_999_900L)),
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
