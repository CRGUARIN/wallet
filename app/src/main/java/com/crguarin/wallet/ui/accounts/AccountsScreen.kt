package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.HeroAmountText
import com.crguarin.wallet.ui.components.HeroCard
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.groupedItemBackground
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant

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
                    AccountListRow(
                        item = item,
                        onClick = { onAccountClick(item.account.id) },
                        today = uiState.today,
                        cardOverview = uiState.cardOverviews[item.account.id],
                        showTypeLabel = true,
                        enabled = !uiState.isReordering,
                        trailing =
                            if (uiState.isReordering) {
                                {
                                    ReorderArrows(
                                        canMoveUp = index > 0,
                                        canMoveDown = index < uiState.activeAccounts.size - 1,
                                        onMoveUp = { onMoveAccountUp(item.account.id) },
                                        onMoveDown = { onMoveAccountDown(item.account.id) },
                                    )
                                }
                            } else {
                                null
                            },
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
                            AccountListRow(
                                item = item,
                                onClick = { onAccountClick(item.account.id) },
                                today = uiState.today,
                                cardOverview = uiState.cardOverviews[item.account.id],
                                showTypeLabel = true,
                                isArchived = true,
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

/** Above this font scale the shortcuts stack as full-width rows instead of three tiles. */
private const val STACKED_SHORTCUT_FONT_SCALE = 1.3f
private val SHORTCUT_TILE_MIN_HEIGHT = 72.dp
private val SHORTCUT_ROW_MIN_HEIGHT = 56.dp

/** One shortcut of the Accounts tab: its icon, its label and what it opens. */
private data class AccountShortcut(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Debts, recurring payments and categories in one row of equal tonal tiles, one touch from the
 * accounts. With large fonts the tiles become full-width rows, so no label is cut.
 */
@Composable
private fun AccountsShortcuts(
    onOpenDebts: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    val shortcuts =
        listOf(
            AccountShortcut(Icons.Default.Payments, stringResource(R.string.debts_title), onOpenDebts),
            AccountShortcut(Icons.Default.Repeat, stringResource(R.string.recurring_title), onOpenRecurring),
            AccountShortcut(Icons.Default.Category, stringResource(R.string.categories_title), onOpenCategories),
        )
    if (LocalDensity.current.fontScale > STACKED_SHORTCUT_FONT_SCALE) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            shortcuts.forEach { shortcut -> ShortcutRow(shortcut = shortcut) }
        }
    } else {
        // Same height for the three tiles, even when one label takes two lines.
        Row(
            modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            shortcuts.forEach { shortcut ->
                ShortcutTile(shortcut = shortcut, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** Shortcut as a tonal tile: the icon above the label, centered. */
@Composable
private fun ShortcutTile(
    shortcut: AccountShortcut,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Surface(
        onClick = shortcut.onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.heightIn(min = SHORTCUT_TILE_MIN_HEIGHT),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s, vertical = spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Shortcut as a full-width tonal row, for large fonts: the icon and the label side by side. */
@Composable
private fun ShortcutRow(shortcut: AccountShortcut) {
    val spacing = MaterialTheme.walletSpacing
    Surface(
        onClick = shortcut.onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().heightIn(min = SHORTCUT_ROW_MIN_HEIGHT),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(spacing.m))
            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Reorder arrows of the Accounts tab, shown in place of the balance while reordering. */
@Composable
private fun ReorderArrows(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
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
}

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
