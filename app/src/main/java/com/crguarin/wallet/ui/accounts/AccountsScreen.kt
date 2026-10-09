package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.HeroAmountText
import com.crguarin.wallet.ui.components.HeroCard
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.components.groupedItemBackground
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate

@Composable
fun AccountsRoute(
    onAccountClick: (Long) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCategories: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenDebts: () -> Unit = {},
    viewModel: AccountsViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        AccountsScreen(
            uiState = AccountsUiState(),
            onAccountClick = onAccountClick,
            onNewAccountClick = onNewAccountClick,
            onToggleReordering = {},
            onMoveAccountUp = {},
            onMoveAccountDown = {},
            onToggleArchivedExpanded = {},
            modifier = modifier,
            onOpenCategories = onOpenCategories,
            onOpenRecurring = onOpenRecurring,
            onOpenDebts = onOpenDebts,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountsScreen(
        uiState = uiState,
        onAccountClick = onAccountClick,
        onNewAccountClick = onNewAccountClick,
        onToggleReordering = viewModel::toggleReordering,
        onMoveAccountUp = viewModel::moveAccountUp,
        onMoveAccountDown = viewModel::moveAccountDown,
        onToggleArchivedExpanded = viewModel::toggleArchivedExpanded,
        modifier = modifier,
        onOpenCategories = onOpenCategories,
        onOpenRecurring = onOpenRecurring,
        onOpenDebts = onOpenDebts,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    uiState: AccountsUiState,
    onAccountClick: (Long) -> Unit,
    onNewAccountClick: () -> Unit,
    onToggleReordering: () -> Unit,
    onMoveAccountUp: (Long) -> Unit,
    onMoveAccountDown: (Long) -> Unit,
    onToggleArchivedExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCategories: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenDebts: () -> Unit = {},
) {
    val spacing = MaterialTheme.walletSpacing
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.accounts_title)) },
                actions = {
                    if (uiState.activeAccounts.size >= 2) {
                        if (uiState.isReordering) {
                            TextButton(onClick = onToggleReordering) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(spacing.xs))
                                    Text(text = stringResource(R.string.accounts_reorder_done))
                                }
                            }
                        } else {
                            IconButton(onClick = onToggleReordering) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = stringResource(R.string.accounts_reorder),
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.isReordering) {
                AddFab(
                    contentDescription = stringResource(R.string.account_edit_new_title),
                    onClick = onNewAccountClick,
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
        } else if (uiState.isEmpty) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = stringResource(R.string.accounts_empty_title),
                    message = stringResource(R.string.accounts_empty_message),
                    actionLabel = stringResource(R.string.account_edit_new_title),
                    onActionClick = onNewAccountClick,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentPadding =
                    PaddingValues(
                        start = spacing.screenGutter,
                        end = spacing.screenGutter,
                        top = spacing.s,
                        bottom = FAB_CLEARANCE,
                    ),
            ) {
                // Total balance
                item(key = "total_balance_card") {
                    val totalText = uiState.totalBalance.displayText(locale)
                    HeroCard {
                        Text(
                            text = stringResource(R.string.accounts_total_balance),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(spacing.s))
                        HeroAmountText(text = totalText, color = LocalContentColor.current)
                    }
                }

                // Shortcuts to the screens that used to live only in Settings
                item(key = "shortcuts") {
                    AccountsShortcuts(
                        onOpenDebts = onOpenDebts,
                        onOpenRecurring = onOpenRecurring,
                        onOpenCategories = onOpenCategories,
                        modifier = Modifier.padding(top = spacing.m),
                    )
                }

                // Active Accounts Header
                if (uiState.archivedAccounts.isNotEmpty()) {
                    item(key = "active_header") {
                        FormSectionTitle(
                            text = stringResource(R.string.accounts_active_header),
                            modifier = Modifier.padding(top = spacing.l, bottom = spacing.s),
                        )
                    }
                }

                if (uiState.activeAccounts.isEmpty()) {
                    // Only archived accounts exist: a compact hint keeps the archived section reachable.
                    item(key = "no_active_accounts") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.l),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.accounts_no_active_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.accounts_no_active_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Active Accounts List: one tonal layer, like the accounts of Home
                itemsIndexed(
                    items = uiState.activeAccounts,
                    key = { _, item -> item.account.id },
                ) { index, item ->
                    AccountItemRow(
                        item = item,
                        isReordering = uiState.isReordering,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.activeAccounts.size - 1,
                        onMoveUp = { onMoveAccountUp(item.account.id) },
                        onMoveDown = { onMoveAccountDown(item.account.id) },
                        onClick = { onAccountClick(item.account.id) },
                        cardOverview = uiState.cardOverviews[item.account.id],
                        today = uiState.today,
                        modifier =
                            Modifier
                                .padding(top = if (index == 0 && uiState.archivedAccounts.isEmpty()) spacing.l else 0.dp)
                                .groupedItemBackground(index, uiState.activeAccounts.size),
                    )
                }

                // Archived Accounts Section
                if (uiState.archivedAccounts.isNotEmpty()) {
                    item(key = "archived_header") {
                        Surface(
                            onClick = onToggleArchivedExpanded,
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = spacing.l),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = ARCHIVED_HEADER_MIN_HEIGHT)
                                        .padding(horizontal = spacing.l, vertical = spacing.s),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.accounts_archived_count,
                                            uiState.archivedAccounts.size,
                                        ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    imageVector =
                                        if (uiState.isArchivedExpanded) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                    contentDescription =
                                        if (uiState.isArchivedExpanded) {
                                            stringResource(R.string.action_collapse)
                                        } else {
                                            stringResource(R.string.action_expand)
                                        },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (uiState.isArchivedExpanded) {
                        itemsIndexed(
                            items = uiState.archivedAccounts,
                            key = { _, item -> "archived_${item.account.id}" },
                        ) { index, item ->
                            AccountItemRow(
                                item = item,
                                isReordering = false,
                                canMoveUp = false,
                                canMoveDown = false,
                                onMoveUp = {},
                                onMoveDown = {},
                                onClick = { onAccountClick(item.account.id) },
                                isArchived = true,
                                cardOverview = uiState.cardOverviews[item.account.id],
                                today = uiState.today,
                                modifier =
                                    Modifier
                                        .padding(top = if (index == 0) spacing.s else 0.dp)
                                        .groupedItemBackground(index, uiState.archivedAccounts.size),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Debts, recurring payments and categories: one touch from the accounts, besides Settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsShortcuts(
    onOpenDebts: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        TonalPill(
            icon = Icons.Default.Payments,
            label = stringResource(R.string.debts_title),
            onClick = onOpenDebts,
        )
        TonalPill(
            icon = Icons.Default.Repeat,
            label = stringResource(R.string.recurring_title),
            onClick = onOpenRecurring,
        )
        TonalPill(
            icon = Icons.Default.Category,
            label = stringResource(R.string.categories_title),
            onClick = onOpenCategories,
        )
    }
}

/** From this font scale up the balance of a row moves under the name, so neither is squeezed. */
private const val STACKED_FONT_SCALE = 1.3f

/**
 * One account of the list, the same language as the accounts of Home: a pebble with the account
 * ring (or the warm attention ring), the name, a status line and the balance at the end. With big
 * fonts the balance drops under the name.
 */
@Composable
internal fun AccountItemRow(
    item: AccountWithBalance,
    isReordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
    cardOverview: CardOverview? = null,
    today: LocalDate = LocalDate.now(),
) {
    val spacing = MaterialTheme.walletSpacing
    val account = item.account
    val stacked = LocalDensity.current.fontScale >= STACKED_FONT_SCALE
    val isCard = account.type == AccountType.CREDIT_CARD && cardOverview != null
    val isTransit = account.type == AccountType.TRANSIT_CARD
    val showReminder =
        isCard && showPaymentReminder(cardOverview.paymentStatus, cardOverview.pendingStatementDueDate, today)
    val transitStatus = if (isTransit) transitStatus(item.balance, account.fare) else null
    val needsAttention = !isArchived && (showReminder || (transitStatus?.isWarning == true))
    val nameColor = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = !isReordering, role = Role.Button, onClick = onClick)
                .padding(horizontal = spacing.l, vertical = spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(account.icon),
            color = Color(account.color),
            size = ROW_AVATAR_SIZE,
            showRing = !isArchived,
            needsAttention = needsAttention,
        )

        Spacer(modifier = Modifier.width(spacing.m))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = nameColor,
            )
            val typeString = accountTypeLabel(account.type)
            Text(
                text =
                    if (!account.includeInTotal) {
                        "$typeString • " + stringResource(R.string.accounts_not_included_in_total)
                    } else {
                        typeString
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isCard) {
                Text(
                    text = cardAvailableText(cardOverview),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showReminder) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    PaymentReminderChip(
                        status = cardOverview.paymentStatus!!,
                        dueDate = cardOverview.pendingStatementDueDate!!,
                        today = today,
                    )
                }
            }
            if (transitStatus != null) {
                Text(
                    text = transitStatus.text,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (transitStatus.isWarning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            if (stacked && !isReordering) {
                Spacer(modifier = Modifier.height(spacing.xs))
                AccountRowBalance(
                    item = item,
                    isArchived = isArchived,
                    cardOverview = cardOverview,
                    horizontalAlignment = Alignment.Start,
                )
            }
        }

        if (isReordering) {
            Spacer(modifier = Modifier.width(spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.accounts_move_up),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.accounts_move_down),
                    )
                }
            }
        } else if (!stacked) {
            Spacer(modifier = Modifier.width(spacing.m))
            AccountRowBalance(
                item = item,
                isArchived = isArchived,
                cardOverview = cardOverview,
                horizontalAlignment = Alignment.End,
            )
        }
    }
}

/** The figure at the end of an account row: the debt of a card, or the balance of any other account. */
@Composable
private fun AccountRowBalance(
    item: AccountWithBalance,
    isArchived: Boolean,
    cardOverview: CardOverview?,
    horizontalAlignment: Alignment.Horizontal,
) {
    val color = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    if (item.account.type == AccountType.CREDIT_CARD && cardOverview != null) {
        CardDebtBlock(overview = cardOverview, horizontalAlignment = horizontalAlignment)
    } else {
        MoneyText(
            amount = item.balance,
            style = MaterialTheme.walletTypography.rowAmount,
            color = color,
        )
    }
}

private val ROW_AVATAR_SIZE = 44.dp
private val ARCHIVED_HEADER_MIN_HEIGHT = 56.dp

/** Room under the list so the last row is not covered by the add button. */
private val FAB_CLEARANCE = 96.dp

@Preview(name = "Accounts Light", showBackground = true)
@Composable
private fun AccountsPreviewLight() {
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
        AccountsScreen(
            uiState =
                AccountsUiState(
                    totalBalance = Money(500_000L),
                    activeAccounts = listOf(AccountWithBalance(sampleAccount, Money(500_000L))),
                ),
            onAccountClick = {},
            onNewAccountClick = {},
            onToggleReordering = {},
            onMoveAccountUp = {},
            onMoveAccountDown = {},
            onToggleArchivedExpanded = {},
        )
    }
}

@Preview(name = "Accounts Dark", showBackground = true)
@Composable
private fun AccountsPreviewDark() {
    WalletTheme(darkTheme = true) {
        val sampleAccount =
            Account(
                id = 1L,
                name = "Ahorros",
                type = AccountType.SAVINGS,
                currencyCode = APP_CURRENCY,
                initialBalance = Money(150_000L),
                color = -16711936,
                icon = "savings",
                includeInTotal = true,
                isArchived = false,
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        AccountsScreen(
            uiState =
                AccountsUiState(
                    totalBalance = Money(1_500_000L),
                    activeAccounts = listOf(AccountWithBalance(sampleAccount, Money(1_500_000L))),
                ),
            onAccountClick = {},
            onNewAccountClick = {},
            onToggleReordering = {},
            onMoveAccountUp = {},
            onMoveAccountDown = {},
            onToggleArchivedExpanded = {},
        )
    }
}
