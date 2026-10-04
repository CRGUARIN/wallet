package com.crguarin.wallet.ui.recurring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.NotificationPermissionState
import com.crguarin.wallet.ui.components.TransactionListItem
import com.crguarin.wallet.ui.components.displayName
import com.crguarin.wallet.ui.components.rememberNotificationPermissionState
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.LocalDate

/**
 * Provisional route signature (plus defaulted parameters): the navigation graph calls
 * [RecurringEditRoute] with [onNavigateBack]. The leader wires [onTransactionClick]
 * to open the tapped generated transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTransactionClick: (Long) -> Unit = {},
    viewModel: RecurringEditViewModel? = safeHiltViewModel(),
    permissionState: NotificationPermissionState? = null,
) {
    if (viewModel == null) {
        RecurringEditScreen(
            uiState = RecurringEditUiState(isLoading = false),
            onNavigateBack = onNavigateBack,
            onTypeSelected = {},
            onNameChanged = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onFrequencySelected = {},
            onIntervalChanged = {},
            onOpenNextDatePicker = {},
            onDismissNextDatePicker = {},
            onNextDateSelected = {},
            onEndNeverSelected = {},
            onEndOnDateSelected = {},
            onOpenEndDatePicker = {},
            onDismissEndDatePicker = {},
            onEndDateSelected = {},
            onModeSelected = {},
            onReminderSelected = {},
            onNoteChanged = {},
            onActiveChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onTransactionClick = onTransactionClick,
            modifier = modifier,
        )
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val permission = permissionState ?: rememberNotificationPermissionState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecurringEditEvent.Saved -> {
                    if (event.needsPermissionRequest && !permission.granted) {
                        permission.request()
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.recurring_saved))
                    onNavigateBack()
                }
                is RecurringEditEvent.Deleted -> onNavigateBack()
            }
        }
    }

    RecurringEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTypeSelected = viewModel::setType,
        onNameChanged = viewModel::setName,
        onAmountChanged = viewModel::setAmountText,
        onOpenAccountSheet = viewModel::openAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelected = viewModel::setAccountId,
        onOpenCategorySheet = viewModel::openCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onCategorySelected = viewModel::setCategoryId,
        onFrequencySelected = viewModel::setFrequency,
        onIntervalChanged = viewModel::setIntervalText,
        onOpenNextDatePicker = viewModel::openNextDatePicker,
        onDismissNextDatePicker = viewModel::dismissNextDatePicker,
        onNextDateSelected = viewModel::setNextDate,
        onEndNeverSelected = { viewModel.setEndNever(true) },
        onEndOnDateSelected = { viewModel.setEndNever(false) },
        onOpenEndDatePicker = viewModel::openEndDatePicker,
        onDismissEndDatePicker = viewModel::dismissEndDatePicker,
        onEndDateSelected = viewModel::setEndDate,
        onModeSelected = viewModel::setMode,
        onReminderSelected = viewModel::setReminderDaysBefore,
        onNoteChanged = viewModel::setNote,
        onActiveChanged = viewModel::setActive,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onTransactionClick = onTransactionClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringEditScreen(
    uiState: RecurringEditUiState,
    onNavigateBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelected: (Long) -> Unit,
    onOpenCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onOpenNextDatePicker: () -> Unit,
    onDismissNextDatePicker: () -> Unit,
    onNextDateSelected: (LocalDate) -> Unit,
    onEndNeverSelected: () -> Unit,
    onEndOnDateSelected: () -> Unit,
    onOpenEndDatePicker: () -> Unit,
    onDismissEndDatePicker: () -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onModeSelected: (RecurringMode) -> Unit,
    onReminderSelected: (Int?) -> Unit,
    onNoteChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            stringResource(
                                if (uiState.isEditMode) R.string.recurring_edit else R.string.recurring_new,
                            ),
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
                    if (uiState.isEditMode) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }
            uiState.notFound -> {
                ErrorState(
                    message = stringResource(R.string.error_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TypeSelector(
                        selected = uiState.type,
                        onTypeSelected = onTypeSelected,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChanged,
                        label = { Text(text = stringResource(R.string.category_name_label)) },
                        placeholder = { Text(text = stringResource(R.string.recurring_name_hint)) },
                        isError = uiState.nameErrorRes != null,
                        supportingText =
                            uiState.nameErrorRes?.let { res ->
                                { Text(text = stringResource(res)) }
                            },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AmountTextField(
                        value = uiState.amountText,
                        onValueChange = onAmountChanged,
                        errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectorRow(
                        label = stringResource(R.string.transaction_edit_account_label),
                        value =
                            uiState.accounts
                                .firstOrNull { it.account.id == uiState.accountId }
                                ?.account
                                ?.name,
                        errorRes = uiState.accountErrorRes,
                        onClick = onOpenAccountSheet,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectorRow(
                        label = stringResource(R.string.transaction_edit_category_label),
                        value = uiState.selectedCategory?.displayName(),
                        errorRes = uiState.categoryErrorRes,
                        onClick = onOpenCategorySheet,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FrequencySelector(
                        frequency = uiState.frequency,
                        intervalText = uiState.intervalText,
                        intervalErrorRes = uiState.intervalErrorRes,
                        onFrequencySelected = onFrequencySelected,
                        onIntervalChanged = onIntervalChanged,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel(text = stringResource(R.string.recurring_next_date_label))
                    OutlinedButton(
                        onClick = onOpenNextDatePicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = (uiState.nextDate ?: uiState.today).formatMedium())
                    }
                    if (uiState.dateErrorRes != null) {
                        Text(
                            text = stringResource(uiState.dateErrorRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = stringResource(R.string.recurring_next_date_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EndsSelector(
                        endNever = uiState.endNever,
                        endDate = uiState.endDate,
                        endErrorRes = uiState.endErrorRes,
                        onEndNeverSelected = onEndNeverSelected,
                        onEndOnDateSelected = onEndOnDateSelected,
                        onOpenEndDatePicker = onOpenEndDatePicker,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ModeSelector(
                        mode = uiState.mode,
                        onModeSelected = onModeSelected,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReminderSelector(
                        selected = uiState.reminderDaysBefore,
                        onReminderSelected = onReminderSelected,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChanged,
                        label = { Text(text = stringResource(R.string.transaction_edit_note_label)) },
                        isError = uiState.noteErrorRes != null,
                        supportingText =
                            uiState.noteErrorRes?.let { res ->
                                { Text(text = stringResource(res)) }
                            },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.isEditMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = uiState.isActive,
                                        role = Role.Switch,
                                        onClick = { onActiveChanged(!uiState.isActive) },
                                    ),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.recurring_active_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Switch(
                                checked = uiState.isActive,
                                onCheckedChange = null,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.recurring_history_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (uiState.history.isEmpty()) {
                            Text(
                                text = stringResource(R.string.recurring_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column {
                                uiState.history.forEach { details ->
                                    TransactionListItem(
                                        details = details,
                                        onClick = { onTransactionClick(details.transaction.id) },
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.generalErrorRes != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(uiState.generalErrorRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.action_save))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (uiState.showAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            onAccountSelected = { onAccountSelected(it.account.id) },
            today = uiState.today,
            onDismissRequest = onDismissAccountSheet,
        )
    }
    if (uiState.showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categoryNodes,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { onCategorySelected(it.id) },
            onNewCategoryClick = {},
            showNewCategoryAction = false,
            onDismissRequest = onDismissCategorySheet,
        )
    }
    if (uiState.showNextDatePicker) {
        RecurringDatePickerDialog(
            initialDate = uiState.nextDate ?: uiState.today,
            onDismiss = onDismissNextDatePicker,
            onDateSelected = onNextDateSelected,
        )
    }
    if (uiState.showEndDatePicker) {
        RecurringDatePickerDialog(
            initialDate = uiState.endDate ?: uiState.nextDate ?: uiState.today,
            onDismiss = onDismissEndDatePicker,
            onDateSelected = onEndDateSelected,
        )
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.recurring_delete_title),
            message = stringResource(R.string.recurring_delete_message),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeSelector(
    selected: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == TransactionType.EXPENSE,
            onClick = { onTypeSelected(TransactionType.EXPENSE) },
            label = { Text(text = stringResource(R.string.type_expense)) },
        )
        FilterChip(
            selected = selected == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) },
            label = { Text(text = stringResource(R.string.type_income)) },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SelectorRow(
    label: String,
    value: String?,
    errorRes: Int?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = label)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = value ?: label)
        }
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencySelector(
    frequency: RecurrenceFrequency,
    intervalText: String,
    intervalErrorRes: Int?,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalChanged: (String) -> Unit,
) {
    val resources = LocalContext.current.resources
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_frequency_label))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.WEEKLY,
                label = stringResource(R.string.recurring_frequency_weekly),
                onClick = { onFrequencySelected(RecurrenceFrequency.WEEKLY) },
            )
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.MONTHLY,
                label = stringResource(R.string.recurring_frequency_monthly),
                onClick = { onFrequencySelected(RecurrenceFrequency.MONTHLY) },
            )
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.YEARLY,
                label = stringResource(R.string.recurring_frequency_yearly),
                onClick = { onFrequencySelected(RecurrenceFrequency.YEARLY) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel(text = stringResource(R.string.recurring_interval_label))
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = intervalText,
                onValueChange = onIntervalChanged,
                isError = intervalErrorRes != null,
                supportingText =
                    intervalErrorRes?.let { res ->
                        { Text(text = stringResource(res)) }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        val interval = intervalText.toIntOrNull() ?: 0
        if (interval in 1..12) {
            val pluralRes =
                when (frequency) {
                    RecurrenceFrequency.WEEKLY -> R.plurals.recurring_interval_weeks
                    RecurrenceFrequency.MONTHLY -> R.plurals.recurring_interval_months
                    RecurrenceFrequency.YEARLY -> R.plurals.recurring_interval_years
                }
            Text(
                text = resources.getQuantityString(pluralRes, interval, interval),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
    )
}

@Composable
private fun EndsSelector(
    endNever: Boolean,
    endDate: LocalDate?,
    endErrorRes: Int?,
    onEndNeverSelected: () -> Unit,
    onEndOnDateSelected: () -> Unit,
    onOpenEndDatePicker: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_end_label))
        EndOptionRow(
            selected = endNever,
            label = stringResource(R.string.recurring_end_never),
            onClick = onEndNeverSelected,
        )
        EndOptionRow(
            selected = !endNever,
            label = stringResource(R.string.recurring_end_on_date),
            onClick = onEndOnDateSelected,
        )
        if (!endNever) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onOpenEndDatePicker,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = endDate?.formatMedium() ?: stringResource(R.string.recurring_end_on_date))
            }
        }
        if (endErrorRes != null) {
            Text(
                text = stringResource(endErrorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EndOptionRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ModeSelector(
    mode: RecurringMode,
    onModeSelected: (RecurringMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_mode_label))
        ModeOptionRow(
            selected = mode == RecurringMode.AUTO,
            title = stringResource(R.string.recurring_mode_auto),
            description = stringResource(R.string.recurring_mode_auto_desc),
            onClick = { onModeSelected(RecurringMode.AUTO) },
        )
        ModeOptionRow(
            selected = mode == RecurringMode.CONFIRM,
            title = stringResource(R.string.recurring_mode_confirm),
            description = stringResource(R.string.recurring_mode_confirm_desc),
            onClick = { onModeSelected(RecurringMode.CONFIRM) },
        )
    }
}

@Composable
private fun ModeOptionRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSelector(
    selected: Int?,
    onReminderSelected: (Int?) -> Unit,
) {
    val resources = LocalContext.current.resources
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_reminder_label))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RECURRING_REMINDER_OPTIONS.forEach { days ->
                val label =
                    if (days == null) {
                        stringResource(R.string.recurring_reminder_none)
                    } else {
                        resources.getQuantityString(R.plurals.recurring_reminder_days_before, days, days)
                    }
                FilterChip(
                    selected = selected == days,
                    onClick = { onReminderSelected(days) },
                    label = { Text(text = label) },
                )
            }
        }
    }
}
