package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant

@Composable
fun AccountsRoute(
    onAccountClick: (Long) -> Unit,
    onNewAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
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
) {
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
                                    Spacer(modifier = Modifier.width(4.dp))
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
                FloatingActionButton(onClick = onNewAccountClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.account_edit_new_title),
                    )
                }
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Total Balance Card
                item(key = "total_balance_card") {
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
                                amount = uiState.totalBalance,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                // Active Accounts Header
                if (uiState.archivedAccounts.isNotEmpty()) {
                    item(key = "active_header") {
                        Text(
                            text = stringResource(R.string.accounts_active_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .padding(top = 8.dp, bottom = 4.dp)
                                    .semantics { heading() },
                        )
                    }
                }

                if (uiState.activeAccounts.isEmpty()) {
                    // Only archived accounts exist: a compact hint keeps the archived section reachable.
                    item(key = "no_active_accounts") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

                // Active Accounts List
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
                    )
                }

                // Archived Accounts Section
                if (uiState.archivedAccounts.isNotEmpty()) {
                    item(key = "archived_header") {
                        OutlinedCard(
                            onClick = onToggleArchivedExpanded,
                            shape = RoundedCornerShape(12.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
                        ) { _, item ->
                            AccountItemRow(
                                item = item,
                                isReordering = false,
                                canMoveUp = false,
                                canMoveDown = false,
                                onMoveUp = {},
                                onMoveDown = {},
                                onClick = { onAccountClick(item.account.id) },
                                isArchived = true,
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountItemRow(
    item: AccountWithBalance,
    isReordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    Card(
        onClick =
            if (!isReordering) {
                onClick
            } else {
                {}
            },
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isArchived) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isArchived) 0.dp else 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(item.account.color).copy(alpha = if (isArchived) 0.5f else 1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconCatalog.get(item.account.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (isArchived) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Spacer(modifier = Modifier.height(2.dp))
                val typeString =
                    when (item.account.type) {
                        AccountType.CASH -> stringResource(R.string.account_type_cash)
                        AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
                    }
                Text(
                    text =
                        if (!item.account.includeInTotal) {
                            "$typeString • " + stringResource(R.string.accounts_not_included_in_total)
                        } else {
                            typeString
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isReordering) {
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
            } else {
                MoneyText(
                    amount = item.balance,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isArchived) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

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
