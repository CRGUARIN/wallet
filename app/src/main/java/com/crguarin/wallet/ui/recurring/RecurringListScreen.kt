package com.crguarin.wallet.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurringBlockReason
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringOverview
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.NotificationPermissionState
import com.crguarin.wallet.ui.components.rememberNotificationPermissionState
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.LocalDate

/**
 * Provisional route signature (plus defaulted parameters): the navigation graph calls
 * [RecurringListRoute] with [onNavigateBack], [onAddClick] and [onPaymentClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListRoute(
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onPaymentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringListViewModel? = safeHiltViewModel(),
    permissionState: NotificationPermissionState? = null,
) {
    if (viewModel == null) {
        RecurringListScreen(
            uiState = RecurringListUiState(isLoading = false),
            notificationsBlocked = false,
            onNavigateBack = onNavigateBack,
            onAddClick = onAddClick,
            onPaymentClick = onPaymentClick,
            onConfirmClick = {},
            onSkipClick = {},
            onConfirmAmountChanged = {},
            onOpenConfirmDatePicker = {},
            onDismissConfirmDatePicker = {},
            onConfirmDateSelected = {},
            onConfirmRecord = {},
            onDismissConfirm = {},
            onConfirmSkip = {},
            onDismissSkip = {},
            onEnableNotifications = {},
            modifier = modifier,
        )
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val permission = permissionState ?: rememberNotificationPermissionState()
    val notificationsBlocked =
        !permission.granted &&
            rememberNotifiable(uiState) {
                it.payment.reminderDaysBefore != null || it.payment.mode == RecurringMode.CONFIRM
            }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecurringListEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    RecurringListScreen(
        uiState = uiState,
        notificationsBlocked = notificationsBlocked,
        onNavigateBack = onNavigateBack,
        onAddClick = onAddClick,
        onPaymentClick = onPaymentClick,
        onConfirmClick = viewModel::openConfirm,
        onSkipClick = viewModel::openSkip,
        onConfirmAmountChanged = viewModel::setConfirmAmountText,
        onOpenConfirmDatePicker = viewModel::openConfirmDatePicker,
        onDismissConfirmDatePicker = viewModel::dismissConfirmDatePicker,
        onConfirmDateSelected = viewModel::setConfirmDate,
        onConfirmRecord = viewModel::confirmRecord,
        onDismissConfirm = viewModel::dismissConfirm,
        onConfirmSkip = viewModel::confirmSkip,
        onDismissSkip = viewModel::dismissSkip,
        onEnableNotifications = permission::request,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** True when any pending or active payment matches [predicate]. */
@Composable
private fun rememberNotifiable(
    uiState: RecurringListUiState,
    predicate: (RecurringItem) -> Boolean,
): Boolean {
    val overview = uiState.overview ?: return false
    return remember(overview) {
        (overview.pending + overview.active).any(predicate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    uiState: RecurringListUiState,
    notificationsBlocked: Boolean,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onPaymentClick: (Long) -> Unit,
    onConfirmClick: (RecurringItem) -> Unit,
    onSkipClick: (RecurringItem) -> Unit,
    onConfirmAmountChanged: (String) -> Unit,
    onOpenConfirmDatePicker: () -> Unit,
    onDismissConfirmDatePicker: () -> Unit,
    onConfirmDateSelected: (LocalDate) -> Unit,
    onConfirmRecord: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmSkip: () -> Unit,
    onDismissSkip: () -> Unit,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.recurring_title)) },
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
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val overview = uiState.overview
        val isEmpty =
            overview == null || (overview.pending.isEmpty() && overview.active.isEmpty() && overview.inactive.isEmpty())
        if (isEmpty) {
            EmptyState(
                icon = Icons.Default.Repeat,
                title = stringResource(R.string.recurring_empty_title),
                message = stringResource(R.string.recurring_empty_message),
                actionLabel = stringResource(R.string.action_add),
                onActionClick = onAddClick,
                modifier = Modifier.padding(innerPadding),
            )
        } else if (overview != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                item(key = "summary") {
                    MonthlySummary(
                        overview = uiState.overview,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (notificationsBlocked) {
                    item(key = "permission") {
                        PermissionBanner(
                            onEnableClick = onEnableNotifications,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                if (overview.pending.isNotEmpty()) {
                    item(key = "pending_header") {
                        SectionHeader(
                            text = stringResource(R.string.recurring_section_pending),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    overview.pending.forEach { item ->
                        item(key = "pending_${item.payment.id}") {
                            PendingRow(
                                item = item,
                                onConfirmClick = { onConfirmClick(item) },
                                onSkipClick = { onSkipClick(item) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (overview.active.isNotEmpty()) {
                    item(key = "active_header") {
                        SectionHeader(
                            text = stringResource(R.string.recurring_section_active),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    overview.active.forEach { item ->
                        item(key = "active_${item.payment.id}") {
                            ActiveRow(
                                item = item,
                                dimmed = false,
                                onClick = { onPaymentClick(item.payment.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (overview.inactive.isNotEmpty()) {
                    item(key = "inactive_header") {
                        SectionHeader(
                            text = stringResource(R.string.recurring_section_inactive),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    overview.inactive.forEach { item ->
                        item(key = "inactive_${item.payment.id}") {
                            ActiveRow(
                                item = item,
                                dimmed = true,
                                onClick = { onPaymentClick(item.payment.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }

    uiState.confirmItem?.let { item ->
        RecordConfirmDialog(
            item = item,
            amountText = uiState.confirmAmountText,
            amountErrorRes = uiState.confirmAmountErrorRes,
            date = uiState.confirmDate ?: item.payment.nextDueDate,
            isProcessing = uiState.isProcessing,
            onAmountChanged = onConfirmAmountChanged,
            onOpenDatePicker = onOpenConfirmDatePicker,
            onConfirm = onConfirmRecord,
            onDismiss = onDismissConfirm,
        )
    }
    if (uiState.showConfirmDatePicker && uiState.confirmItem != null) {
        RecurringDatePickerDialog(
            initialDate = uiState.confirmDate ?: uiState.confirmItem.payment.nextDueDate,
            onDismiss = onDismissConfirmDatePicker,
            onDateSelected = onConfirmDateSelected,
        )
    }
    uiState.skipItem?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.recurring_skip_title),
            message = stringResource(R.string.recurring_skip_message, item.payment.nextDueDate.formatMedium()),
            confirmText = stringResource(R.string.recurring_skip_action),
            onConfirm = onConfirmSkip,
            onDismiss = onDismissSkip,
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
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun MonthlySummary(
    overview: RecurringOverview?,
    modifier: Modifier = Modifier,
) {
    if (overview == null) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryLine(
                label = stringResource(R.string.recurring_monthly_expense),
                amount = overview.monthlyExpense,
                type = TransactionType.EXPENSE,
            )
            if (overview.monthlyIncome.minorUnits > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                SummaryLine(
                    label = stringResource(R.string.recurring_monthly_income),
                    amount = overview.monthlyIncome,
                    type = TransactionType.INCOME,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.recurring_monthly_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    amount: Money,
    type: TransactionType,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun PermissionBanner(
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recurring_notifications_blocked),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onEnableClick) {
                Text(text = stringResource(R.string.recurring_notifications_enable))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingRow(
    item: RecurringItem,
    onConfirmClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.payment.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = item.payment.nextDueDate.formatMedium(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.pendingCount > 1) {
                        Text(
                            text =
                                resources.getQuantityString(
                                    R.plurals.recurring_pending_count,
                                    item.pendingCount,
                                    item.pendingCount,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                MoneyText(
                    amount = item.payment.amount,
                    type = item.payment.type,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onConfirmClick) {
                    Text(text = stringResource(R.string.recurring_confirm_action))
                }
                TextButton(onClick = onSkipClick) {
                    Text(text = stringResource(R.string.recurring_skip_action))
                }
            }
        }
    }
}

@Composable
private fun ActiveRow(
    item: RecurringItem,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payment = item.payment
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val schedule =
        remember(payment.frequency, payment.interval, payment.startDate, locale) {
            scheduleText(
                resources = context.resources,
                frequency = payment.frequency,
                interval = payment.interval,
                startDate = payment.startDate,
                locale = locale,
            )
        }
    val category = item.category
    val icon = IconCatalog.get(category?.icon.orEmpty())
    val iconTint = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (dimmed) 0.6f else 1f)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconTint)
                            .padding(8.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    MoneyText(
                        amount = payment.amount,
                        type = payment.type,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = schedule,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.recurring_next_on, payment.nextDueDate.formatMedium()),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (payment.endDate != null) {
                Text(
                    text = stringResource(R.string.recurring_ends_on, payment.endDate.formatMedium()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.accountName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ModeBadge(mode = payment.mode)
            if (dimmed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        stringResource(
                            if (payment.isFinished) {
                                R.string.recurring_finished
                            } else {
                                R.string.recurring_paused
                            },
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.blockReason?.let { reason ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        stringResource(
                            when (reason) {
                                RecurringBlockReason.ACCOUNT_ARCHIVED -> R.string.recurring_blocked_account
                                RecurringBlockReason.CATEGORY_ARCHIVED -> R.string.recurring_blocked_category
                            },
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ModeBadge(mode: RecurringMode) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text =
                    stringResource(
                        when (mode) {
                            RecurringMode.AUTO -> R.string.recurring_badge_auto
                            RecurringMode.CONFIRM -> R.string.recurring_badge_confirm
                        },
                    ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordConfirmDialog(
    item: RecurringItem,
    amountText: String,
    amountErrorRes: Int?,
    date: LocalDate,
    isProcessing: Boolean,
    onAmountChanged: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.recurring_confirm_title, item.payment.name))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.recurring_confirm_message))
                Spacer(modifier = Modifier.height(12.dp))
                AmountTextField(
                    value = amountText,
                    onValueChange = onAmountChanged,
                    errorMessage = amountErrorRes?.let { stringResource(it) },
                    enabled = !isProcessing,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onOpenDatePicker, enabled = !isProcessing) {
                    Text(text = date.formatMedium())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isProcessing) {
                Text(text = stringResource(R.string.recurring_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}
