package com.crguarin.wallet.ui.accounts

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.crguarin.wallet.ui.motion.LocalCardFlightRoute
import com.crguarin.wallet.ui.motion.LocalNavAnimatedVisibilityScope
import com.crguarin.wallet.ui.motion.LocalSharedTransitionScope
import com.crguarin.wallet.ui.motion.StaggerClock
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.motion.rememberStaggerClock
import com.crguarin.wallet.ui.motion.staggeredEnter
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
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
                title = {
                    Text(
                        // The title is the account name, as in the mock; the generic one while loading.
                        text = account?.name ?: stringResource(R.string.account_detail_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.testTag(ACCOUNT_DETAIL_TITLE_TAG),
                    )
                },
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
        val rowSurface = raisedColor()
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            // The side gutter is applied by each item: the day headers bring their own.
            contentPadding = PaddingValues(top = spacing.l, bottom = spacing.xxxl),
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
                        status = accountStatus(uiState.accountWithBalance, cardOverview, uiState.today, showTypeLabel = false),
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
                            .padding(top = DETAIL_SECTION_GAP, bottom = spacing.s)
                            .staggeredEnter(stagger, 3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.account_detail_transactions_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
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
                        // Only the first block enters staggered (index 4); the rest is already in
                        // place. The entrance only redraws, never detaches.
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
                                // The rows of one day form one card: the first carries its top
                                // corners and the last its bottom ones.
                                val previous = if (index > 0) pagingItems.peek(index - 1) else null
                                val next = if (index < pagingItems.itemCount - 1) pagingItems.peek(index + 1) else null
                                TransactionListItem(
                                    details = entry.details,
                                    perspectiveAccountId = account.id,
                                    onClick = { onTransactionClick(entry.details.transaction.id) },
                                    modifier =
                                        gutter
                                            .then(firstBlock)
                                            .movementRow(
                                                top = previous == null || previous is TransactionListEntry.DayHeader,
                                                bottom = next == null || next is TransactionListEntry.DayHeader,
                                                color = rowSurface,
                                            ).testTag(MOVEMENT_ROW_TAG),
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
 * Stagger clock of the detail (§8.3): on a card flight from Home the blocks start landing at
 * 350 ms; any other entry (the Accounts tab, a notification, bare tests) has no delay. The delay
 * is captured on the first composition of the entry, so it does not move while the entry leaves.
 */
@Composable
private fun rememberAccountDetailStagger(accountId: Long): StaggerClock {
    val flown =
        LocalSharedTransitionScope.current != null &&
            LocalNavAnimatedVisibilityScope.current != null &&
            LocalCardFlightRoute.current
    val deep = depthOn()
    val startDelay = remember { if (flown && deep) 350 else 0 }
    return rememberStaggerClock("account-detail-$accountId", startDelayMillis = startDelay)
}

/** Height of an action key's face; its edge hangs below, [keycapDepth] deep. */
private val ACTION_FACE_HEIGHT = 52.dp

/** Corner radius of the action keys. */
private val ACTION_CORNER = 18.dp

/** Space between the action keys, across and down. */
private val ACTION_GAP = 12.dp

/** Test tag of every action key of the detail. */
internal const val ACTION_KEY_TAG = "account-action-key"

/** Test tag of the title of the detail (the account name). */
internal const val ACCOUNT_DETAIL_TITLE_TAG = "account-detail-title"

/** Test tag of every row of the movements card. */
internal const val MOVEMENT_ROW_TAG = "account-movement-row"

/** Role of an action key: the first one is the main action, the rest are secondary. */
internal enum class ActionRole { Primary, Secondary }

/** Semantics key with the [ActionRole] of a key, for tests. Screen readers do not read it. */
internal val ActionRoleKey = SemanticsPropertyKey<ActionRole>("ActionRole")

/** One key of the action grid: its text and what it does. */
private class ActionSpec(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * The actions of the detail as relief keys, two per row. The first one is the main action (filled
 * with `primary`); the others use `secondaryContainer`. An odd last key spans the full row.
 */
@Composable
private fun ActionKeyGrid(actions: List<ActionSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(ACTION_GAP)) {
        actions.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ACTION_GAP)) {
                row.forEachIndexed { column, action ->
                    ActionKey(
                        text = action.text,
                        role = if (rowIndex == 0 && column == 0) ActionRole.Primary else ActionRole.Secondary,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** A relief key of 52 dp with its label centered; it sinks when pressed (see [Keycap]). */
@Composable
private fun ActionKey(
    text: String,
    role: ActionRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val primary = role == ActionRole.Primary
    Keycap(
        onClick = onClick,
        face = if (primary) colors.primary else colors.secondaryContainer,
        shape = RoundedCornerShape(ACTION_CORNER),
        modifier =
            modifier
                .height(ACTION_FACE_HEIGHT + MaterialTheme.walletDepth.keycapDepth)
                .testTag(ACTION_KEY_TAG)
                .semantics { this[ActionRoleKey] = role },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = if (primary) colors.onPrimary else colors.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.walletSpacing.s),
        )
    }
}

/** Expense, income and transfer from this account: the three things done most with cash and savings. */
@Composable
private fun ColumnScope.CashActions(
    canTransfer: Boolean,
    onNewTransactionClick: (TransactionType) -> Unit,
) {
    val actions =
        buildList {
            add(ActionSpec(stringResource(R.string.type_expense)) { onNewTransactionClick(TransactionType.EXPENSE) })
            add(ActionSpec(stringResource(R.string.type_income)) { onNewTransactionClick(TransactionType.INCOME) })
            if (canTransfer) {
                add(ActionSpec(stringResource(R.string.type_transfer)) { onNewTransactionClick(TransactionType.TRANSFER) })
            }
        }
    ActionKeyGrid(actions)
}

/** Pay is the main action of a card; an expense and the statement reconciliation follow it. */
@Composable
private fun ColumnScope.CardActions(
    canAddExpense: Boolean,
    onPayCardClick: () -> Unit,
    onReconcileClick: () -> Unit,
    onNewTransactionClick: (TransactionType) -> Unit,
) {
    val actions =
        buildList {
            add(ActionSpec(stringResource(R.string.card_action_pay), onPayCardClick))
            if (canAddExpense) {
                add(ActionSpec(stringResource(R.string.type_expense)) { onNewTransactionClick(TransactionType.EXPENSE) })
            }
            add(ActionSpec(stringResource(R.string.card_action_reconcile), onReconcileClick))
        }
    ActionKeyGrid(actions)
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
    val actions =
        buildList {
            if (canRegisterTrip) {
                add(ActionSpec(stringResource(R.string.transit_action_trip), onRegisterTripClick))
            }
            if (canRecharge) {
                add(ActionSpec(stringResource(R.string.transit_action_recharge), onRechargeClick))
            }
        }
    ActionKeyGrid(actions)
}

/** Surface of the raised blocks (the movements card rows), as in `raisedSurface`. */
@Composable
private fun raisedColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (MaterialTheme.walletColors.isDark) scheme.surfaceContainer else scheme.surfaceContainerLowest
}

/** Corner radius of the movements card, on its first and last rows. */
private val MOVEMENTS_CORNER = 24.dp

/** Padding of the movements card above its first row and below its last one. */
private val MOVEMENTS_EDGE_PADDING = 8.dp

/**
 * A row of the movements card: the surface color, with the card's rounded corners and edge
 * padding on the first ([top]) and last ([bottom]) rows, so the rows read as one container.
 */
private fun Modifier.movementRow(
    top: Boolean,
    bottom: Boolean,
    color: Color,
): Modifier {
    val topCorner = if (top) MOVEMENTS_CORNER else 0.dp
    val bottomCorner = if (bottom) MOVEMENTS_CORNER else 0.dp
    val shape =
        RoundedCornerShape(
            topStart = topCorner,
            topEnd = topCorner,
            bottomEnd = bottomCorner,
            bottomStart = bottomCorner,
        )
    return clip(shape)
        .background(color)
        .padding(
            top = if (top) MOVEMENTS_EDGE_PADDING else 0.dp,
            bottom = if (bottom) MOVEMENTS_EDGE_PADDING else 0.dp,
        )
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
