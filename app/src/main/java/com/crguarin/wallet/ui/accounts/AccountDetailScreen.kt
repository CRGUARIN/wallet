package com.crguarin.wallet.ui.accounts

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionListEntry
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.InstallmentStatus
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.TransactionDayHeader
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.components.groupedItemBackground
import com.crguarin.wallet.ui.motion.Keycap
import com.crguarin.wallet.ui.motion.LocalNavAnimatedVisibilityScope
import com.crguarin.wallet.ui.motion.LocalSharedTransitionScope
import com.crguarin.wallet.ui.motion.StaggerClock
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.motion.rememberStaggerClock
import com.crguarin.wallet.ui.motion.staggeredEnter
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import com.crguarin.wallet.ui.transactions.rememberEntriesPagingItems
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AccountDetailRoute(
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onTransferClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel? = safeHiltViewModel(),
    onPayCardClick: (fromAccountId: Long?, toAccountId: Long, initialAmountMinor: Long?) -> Unit = { _, _, _ -> },
    onReconcileClick: (accountId: Long) -> Unit = {},
    onRechargeClick: (fromAccountId: Long?, toAccountId: Long) -> Unit = { _, _ -> },
    onNewTransactionClick: (accountId: Long, type: TransactionType) -> Unit = { _, _ -> },
) {
    if (viewModel == null) {
        AccountDetailScreen(
            uiState = AccountDetailUiState(isLoading = false),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = onNavigateBack,
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = onTransactionClick,
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountDetailEvent.NavigateBack -> onNavigateBack()
                is AccountDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
                is AccountDetailEvent.TripRegistered -> {
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
            }
        }
    }

    AccountDetailScreen(
        uiState = uiState,
        pagingItems = pagingItems,
        onNavigateBack = onNavigateBack,
        onEditClick = { onEditClick(viewModel.accountId) },
        onTransferClick = { onTransferClick(viewModel.accountId) },
        onTransactionClick = onTransactionClick,
        onViewAllTransactions = { onViewAllTransactions(viewModel.accountId) },
        onArchiveClick = viewModel::archiveAccount,
        onUnarchiveClick = viewModel::unarchiveAccount,
        onDeleteClick = viewModel::showDeleteDialog,
        onConfirmDelete = viewModel::deleteAccount,
        onConfirmArchiveInsteadOfDelete = viewModel::confirmArchiveInsteadOfDelete,
        onDismissDialogs = viewModel::dismissDialogs,
        onPayCardClick = {
            onPayCardClick(uiState.payFromAccountId, viewModel.accountId, uiState.payAmountMinor)
        },
        onReconcileClick = { onReconcileClick(viewModel.accountId) },
        onRegisterTripClick = viewModel::onRegisterTripClick,
        onRechargeClick = { onRechargeClick(uiState.rechargeFromAccountId, viewModel.accountId) },
        onNewTransactionClick = { type -> onNewTransactionClick(viewModel.accountId, type) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    uiState: AccountDetailUiState,
    pagingItems: LazyPagingItems<TransactionListEntry>,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onTransferClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllTransactions: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmArchiveInsteadOfDelete: () -> Unit,
    onDismissDialogs: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onPayCardClick: () -> Unit = {},
    onReconcileClick: () -> Unit = {},
    onRegisterTripClick: () -> Unit = {},
    onRechargeClick: () -> Unit = {},
    onNewTransactionClick: (TransactionType) -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val account = uiState.accountWithBalance?.account
    val spacing = MaterialTheme.walletSpacing

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.account_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (account != null) {
                        // Editing is the most common reason to open the menu, so it gets its own icon.
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.account_detail_menu_edit),
                            )
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (!account.isArchived) {
                                if (uiState.canTransfer) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.account_detail_menu_transfer)) },
                                        onClick = {
                                            menuExpanded = false
                                            onTransferClick()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.account_detail_menu_archive)) },
                                    onClick = {
                                        menuExpanded = false
                                        onArchiveClick()
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.account_detail_menu_unarchive)) },
                                    onClick = {
                                        menuExpanded = false
                                        onUnarchiveClick()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.account_detail_menu_delete),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (account == null) {
            // The account was deleted (or the id never existed): explain it and let the user leave.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = stringResource(R.string.account_detail_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                )
            }
            return@Scaffold
        }
        // One stagger clock for the whole detail (§8.3): actions (0), card section (1),
        // installments/statements headings (2), movements heading (3) and first block (4).
        val stagger = rememberAccountDetailStagger(account.id)
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            // The side gutter is applied by each item: the day headers bring their own.
            contentPadding = PaddingValues(top = spacing.s, bottom = spacing.xxxl),
        ) {
            val gutter = Modifier.padding(horizontal = spacing.screenGutter)
            val balance = uiState.accountWithBalance.balance
            val cardOverview = uiState.cardOverview
            // The idle loops (hero float, sheen) stop while the user scrolls (§4.7).
            val idleInterrupted = listState.isScrollInProgress
            if (account.type == AccountType.CREDIT_CARD && cardOverview != null) {
                item(key = "header") {
                    CardSummaryCard(
                        overview = cardOverview,
                        today = uiState.today,
                        account = account,
                        modifier = gutter,
                        actions = {
                            CardActions(
                                canAddExpense = !account.isArchived,
                                onPayCardClick = onPayCardClick,
                                onReconcileClick = onReconcileClick,
                                onNewTransactionClick = onNewTransactionClick,
                            )
                        },
                        staggerClock = stagger,
                        idleInterrupted = idleInterrupted,
                    )
                }
                if (cardOverview.activeInstallments.isNotEmpty()) {
                    item(key = "installments_header") {
                        DetailSectionTitle(
                            text = stringResource(R.string.card_installments_title),
                            modifier = gutter.staggeredEnter(stagger, 2),
                        )
                    }
                    itemsIndexed(
                        items = cardOverview.activeInstallments,
                        // Prefixed: the same transaction also appears in the movements list below.
                        key = { _, installment -> "installment_${installment.transactionId}" },
                    ) { index, installment ->
                        InstallmentRow(
                            installment = installment,
                            onClick = { onTransactionClick(installment.transactionId) },
                            modifier = gutter.groupedItemBackground(index, cardOverview.activeInstallments.size),
                        )
                    }
                }
                if (uiState.statements.isNotEmpty()) {
                    item(key = "statements_header") {
                        DetailSectionTitle(
                            text = stringResource(R.string.card_statements_history),
                            modifier = gutter.staggeredEnter(stagger, 2),
                        )
                    }
                    itemsIndexed(
                        items = uiState.statements,
                        key = { _, statement -> "statement_${statement.id}" },
                    ) { index, statement ->
                        StatementRow(
                            statement = statement,
                            adjustmentAmount = uiState.adjustmentAmounts[statement.id],
                            modifier = gutter.groupedItemBackground(index, uiState.statements.size),
                        )
                    }
                }
            } else if (account.type == AccountType.TRANSIT_CARD) {
                item(key = "header") {
                    TransitCardSummaryCard(
                        account = account,
                        balance = balance,
                        monthSummary = uiState.monthSummary,
                        modifier = gutter,
                        actions =
                            if (account.isArchived) {
                                null
                            } else {
                                {
                                    TransitActions(
                                        canRegisterTrip = uiState.canRegisterTrip,
                                        canRecharge = uiState.canTransfer,
                                        onRegisterTripClick = onRegisterTripClick,
                                        onRechargeClick = onRechargeClick,
                                    )
                                }
                            },
                        staggerClock = stagger,
                        idleInterrupted = idleInterrupted,
                    )
                }
            } else {
                item(key = "header") {
                    AccountHeaderCard(
                        account = account,
                        balance = balance,
                        monthSummary = uiState.monthSummary,
                        modifier = gutter,
                        actions =
                            if (account.isArchived) {
                                null
                            } else {
                                {
                                    CashActions(
                                        canTransfer = uiState.canTransfer,
                                        onNewTransactionClick = onNewTransactionClick,
                                    )
                                }
                            },
                        staggerClock = stagger,
                        idleInterrupted = idleInterrupted,
                    )
                }
            }

            item(key = "transactions_header") {
                Row(
                    modifier =
                        gutter
                            .fillMaxWidth()
                            .padding(top = spacing.l)
                            .staggeredEnter(stagger, 3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.account_detail_transactions_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics { heading() },
                    )
                    TextButton(onClick = onViewAllTransactions) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }

            val isLoading =
                pagingItems.loadState.refresh is LoadState.Loading &&
                    pagingItems.itemCount == 0

            val refreshError = pagingItems.loadState.refresh as? LoadState.Error

            when {
                isLoading -> {
                    item(key = "transactions_loading") {
                        LoadingState(fillMaxSize = false, modifier = gutter.fillMaxWidth())
                    }
                }

                refreshError != null && pagingItems.itemCount == 0 -> {
                    item(key = "transactions_error") {
                        ErrorState(
                            message = stringResource(R.string.transactions_load_error),
                            actionLabel = stringResource(R.string.action_retry),
                            onActionClick = { pagingItems.retry() },
                            modifier = gutter,
                        )
                    }
                }

                pagingItems.itemCount == 0 -> {
                    item(key = "transactions_empty") {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = stringResource(R.string.account_detail_transactions_empty_title),
                            message = stringResource(R.string.account_detail_transactions_empty_message),
                            modifier = gutter.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    items(
                        count = pagingItems.itemCount,
                        key =
                            pagingItems.itemKey { entry ->
                                when (entry) {
                                    is TransactionListEntry.DayHeader -> "day_header_${entry.date.toEpochDay()}"
                                    is TransactionListEntry.Item -> entry.details.transaction.id
                                }
                            },
                        contentType =
                            pagingItems.itemContentType { entry ->
                                when (entry) {
                                    is TransactionListEntry.DayHeader -> "day_header"
                                    is TransactionListEntry.Item -> "transaction_item"
                                }
                            },
                    ) { index ->
                        // Only the first block of rows enters staggered (index 4); the rest
                        // is already in place. The entrance only redraws, never detaches.
                        val firstBlock =
                            if (index == 0) {
                                Modifier.staggeredEnter(stagger, 4)
                            } else {
                                Modifier
                            }
                        when (val entry = pagingItems[index]) {
                            is TransactionListEntry.DayHeader -> {
                                TransactionDayHeader(
                                    date = entry.date,
                                    net = uiState.dailyTotals[entry.date] ?: Money.ZERO,
                                    today = uiState.today,
                                    modifier = firstBlock,
                                )
                            }
                            is TransactionListEntry.Item -> {
                                // The rows of one day share a tonal layer: the first has the top
                                // corners rounded and the last the bottom ones.
                                val previous = if (index > 0) pagingItems.peek(index - 1) else null
                                val next = if (index < pagingItems.itemCount - 1) pagingItems.peek(index + 1) else null
                                val first = previous == null || previous is TransactionListEntry.DayHeader
                                val last = next == null || next is TransactionListEntry.DayHeader
                                TransactionListItem(
                                    details = entry.details,
                                    perspectiveAccountId = account.id,
                                    onClick = { onTransactionClick(entry.details.transaction.id) },
                                    modifier =
                                        when {
                                            first && last -> gutter.then(firstBlock).groupedItemBackground(0, 1)
                                            first -> gutter.then(firstBlock).groupedItemBackground(0, 2)
                                            last -> gutter.then(firstBlock).groupedItemBackground(1, 2)
                                            else -> gutter.then(firstBlock).groupedItemBackground(1, 3)
                                        },
                                )
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    // Delete Confirm Dialog
    if (uiState.showDeleteConfirmDialog && account != null) {
        ConfirmDialog(
            title = stringResource(R.string.account_detail_delete_title),
            message = stringResource(R.string.account_detail_delete_message, account.name),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDialogs,
        )
    }

    // Offer Archive Dialog (when deleting failed because account has transactions)
    if (uiState.showOfferArchiveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.account_detail_cannot_delete_has_transactions_title),
            message = stringResource(R.string.account_detail_cannot_delete_has_transactions_message),
            confirmText = stringResource(R.string.action_archive),
            onConfirm = onConfirmArchiveInsteadOfDelete,
            onDismiss = onDismissDialogs,
        )
    }
}

/** Heading of a block of the detail (installments, statements). */
@Composable
private fun DetailSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            modifier
                .padding(top = MaterialTheme.walletSpacing.l, bottom = MaterialTheme.walletSpacing.s)
                .semantics { heading() },
    )
}

/**
 * Stagger clock of the detail (§8.3): with a card flight the blocks start landing at 350 ms;
 * without one (bare tests, flat arrivals) there is no delay. The scopes only say a flight is
 * possible, not where this entry came from, so a non-flight 3D entry also waits 350 ms.
 */
@Composable
private fun rememberAccountDetailStagger(accountId: Long): StaggerClock {
    val flown = LocalSharedTransitionScope.current != null && LocalNavAnimatedVisibilityScope.current != null
    val startDelay = if (flown && depthOn()) 350 else 0
    return rememberStaggerClock("account-detail-$accountId", startDelayMillis = startDelay)
}

/** Height of the main action face; the key edge hangs below it. */
private val PRIMARY_ACTION_HEIGHT = 52.dp

/** Height of the secondary actions: the 48 dp touch target. */
private val SECONDARY_ACTION_HEIGHT = 48.dp

/** Corner radius of the action keys. */
private val ACTION_CORNER = 18.dp

/** Text of the main action key: 15 sp, weight 800, over `primary`. */
private val PRIMARY_ACTION_TEXT_SIZE = 15.sp

/** Main action: a 52 dp key face in `primary` over its edge, with an optional leading icon. */
@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val depth = MaterialTheme.walletDepth.keycapDepth
    Keycap(
        onClick = onClick,
        face = colors.primary,
        shape = RoundedCornerShape(ACTION_CORNER),
        depth = depth,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(PRIMARY_ACTION_HEIGHT + depth),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.walletSpacing.s))
            }
            Text(
                text = text,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = PRIMARY_ACTION_TEXT_SIZE,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                color = colors.onPrimary,
            )
        }
    }
}

/** Secondary action: a key face in `secondaryContainer`, at least 48 dp tall. */
@Composable
private fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    Keycap(
        onClick = onClick,
        face = colors.secondaryContainer,
        shape = RoundedCornerShape(ACTION_CORNER),
        enabled = enabled,
        modifier = modifier.heightIn(min = SECONDARY_ACTION_HEIGHT),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.walletSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.walletSpacing.s))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSecondaryContainer,
            )
        }
    }
}

/** Expense, income and transfer from this account: the three things done most with cash and savings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CashActions(
    canTransfer: Boolean,
    onNewTransactionClick: (TransactionType) -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        SecondaryAction(
            text = stringResource(R.string.type_expense),
            onClick = { onNewTransactionClick(TransactionType.EXPENSE) },
        )
        SecondaryAction(
            text = stringResource(R.string.type_income),
            onClick = { onNewTransactionClick(TransactionType.INCOME) },
        )
        if (canTransfer) {
            SecondaryAction(
                text = stringResource(R.string.type_transfer),
                onClick = { onNewTransactionClick(TransactionType.TRANSFER) },
            )
        }
    }
}

/** Pay is the main action of a card; an expense and the statement reconciliation sit beside it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CardActions(
    canAddExpense: Boolean,
    onPayCardClick: () -> Unit,
    onReconcileClick: () -> Unit,
    onNewTransactionClick: (TransactionType) -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    PrimaryAction(text = stringResource(R.string.card_action_pay), onClick = onPayCardClick)
    Spacer(modifier = Modifier.height(spacing.s))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        if (canAddExpense) {
            SecondaryAction(
                text = stringResource(R.string.type_expense),
                onClick = { onNewTransactionClick(TransactionType.EXPENSE) },
            )
        }
        SecondaryAction(text = stringResource(R.string.card_action_reconcile), onClick = onReconcileClick)
    }
}

/**
 * Registering a trip is the main action of a transit card; top up is beside it. While the credit
 * trip is owed there is no trip button, so top up becomes the main action alone.
 */
@Composable
private fun ColumnScope.TransitActions(
    canRegisterTrip: Boolean,
    canRecharge: Boolean,
    onRegisterTripClick: () -> Unit,
    onRechargeClick: () -> Unit,
) {
    if (canRegisterTrip) {
        PrimaryAction(
            text = stringResource(R.string.transit_action_trip),
            onClick = onRegisterTripClick,
            icon = Icons.Default.DirectionsBus,
        )
        if (canRecharge) {
            Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.s))
            SecondaryAction(
                text = stringResource(R.string.transit_action_recharge),
                onClick = onRechargeClick,
                icon = Icons.Default.Add,
            )
        }
    } else if (canRecharge) {
        PrimaryAction(
            text = stringResource(R.string.transit_action_recharge),
            onClick = onRechargeClick,
            icon = Icons.Default.Add,
        )
    }
}

@Composable
private fun InstallmentRow(
    installment: InstallmentStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = spacing.l, vertical = spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = installment.note?.takeIf { it.isNotBlank() } ?: installment.date.formatMedium(),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text =
                    stringResource(
                        R.string.card_installment_progress,
                        // The installment billed next: plans are listed only while unfinished.
                        (installment.billedInstallments + 1).coerceAtMost(installment.installments),
                        installment.installments,
                        MoneyFormatter.format(installment.remainingAmount, locale),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(spacing.m))
        Text(
            text = MoneyFormatter.format(installment.amount, locale),
            style = MaterialTheme.walletTypography.rowAmount,
        )
    }
}

@Composable
private fun StatementRow(
    statement: CardStatement,
    adjustmentAmount: Money?,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.l, vertical = spacing.m),
    ) {
        Text(
            text =
                stringResource(
                    R.string.card_statement_row,
                    statement.closingDate.formatMedium(),
                    MoneyFormatter.format(statement.statementBalance, locale),
                ),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text =
                if (statement.adjustmentTransactionId != null && adjustmentAmount != null) {
                    stringResource(
                        R.string.card_statement_adjusted,
                        MoneyFormatter.format(adjustmentAmount, locale),
                    )
                } else {
                    stringResource(R.string.card_statement_matched)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Account Detail Light", showBackground = true)
@Composable
private fun AccountDetailPreviewLight() {
    WalletTheme(darkTheme = false) {
        val sampleAccount =
            Account(
                id = 1L,
                name = "Efectivo",
                type = AccountType.CASH,
                currencyCode = APP_CURRENCY,
                initialBalance = Money(150_000L),
                color = -16711936,
                icon = "wallet",
                includeInTotal = true,
                isArchived = false,
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        AccountDetailScreen(
            uiState =
                AccountDetailUiState(
                    accountWithBalance = AccountWithBalance(sampleAccount, Money(450_000L)),
                    monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L)),
                ),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = {},
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
        )
    }
}

@Preview(name = "Account Detail Dark", showBackground = true)
@Composable
private fun AccountDetailPreviewDark() {
    WalletTheme(darkTheme = true) {
        val sampleAccount =
            Account(
                id = 1L,
                name = "Cuenta de Ahorros",
                type = AccountType.SAVINGS,
                currencyCode = APP_CURRENCY,
                initialBalance = Money(1_000_000L),
                color = -16711936,
                icon = "savings",
                includeInTotal = true,
                isArchived = true,
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        AccountDetailScreen(
            uiState =
                AccountDetailUiState(
                    accountWithBalance = AccountWithBalance(sampleAccount, Money(1_500_000L)),
                    monthSummary = MonthSummary(income = Money(800_000L), expense = Money(300_000L)),
                ),
            pagingItems = rememberEntriesPagingItems(emptyList()),
            onNavigateBack = {},
            onEditClick = {},
            onTransferClick = {},
            onTransactionClick = {},
            onViewAllTransactions = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onConfirmArchiveInsteadOfDelete = {},
            onDismissDialogs = {},
        )
    }
}
