package com.crguarin.wallet.ui.debts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.recurring.FieldLabel
import com.crguarin.wallet.ui.recurring.RecurringDatePickerDialog
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.LocalDate

private val DEBT_DIRECTION_OPTIONS = listOf(DebtDirection.OWED_TO_ME, DebtDirection.I_OWE)

/**
 * Creates or edits a debt. The navigation graph calls [DebtEditRoute] with [onNavigateBack]
 * (after saving or leaving) and [onDeleted] (after deleting, which may need a different target).
 */
@Composable
fun DebtEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = onNavigateBack,
    viewModel: DebtEditViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        DebtEditScreen(
            uiState = DebtEditUiState(isLoading = false),
            onNavigateBack = onNavigateBack,
            onDirectionSelected = {},
            onPersonNameChanged = {},
            onAmountChanged = {},
            onOpenStartDatePicker = {},
            onDismissStartDatePicker = {},
            onStartDateSelected = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onDueDateSelected = {},
            onClearDueDate = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            modifier = modifier,
        )
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DebtEditEvent.Saved -> onNavigateBack()
                is DebtEditEvent.Deleted -> onDeleted()
            }
        }
    }

    DebtEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDirectionSelected = viewModel::setDirection,
        onPersonNameChanged = viewModel::setPersonName,
        onAmountChanged = viewModel::setAmountText,
        onOpenStartDatePicker = viewModel::openStartDatePicker,
        onDismissStartDatePicker = viewModel::dismissStartDatePicker,
        onStartDateSelected = viewModel::setDate,
        onOpenDueDatePicker = viewModel::openDueDatePicker,
        onDismissDueDatePicker = viewModel::dismissDueDatePicker,
        onDueDateSelected = viewModel::setDueDate,
        onClearDueDate = viewModel::clearDueDate,
        onNoteChanged = viewModel::setNote,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtEditScreen(
    uiState: DebtEditUiState,
    onNavigateBack: () -> Unit,
    onDirectionSelected: (DebtDirection) -> Unit,
    onPersonNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onDismissStartDatePicker: () -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    onOpenDueDatePicker: () -> Unit,
    onDismissDueDatePicker: () -> Unit,
    onDueDateSelected: (LocalDate) -> Unit,
    onClearDueDate: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (uiState.isEditMode) R.string.debts_edit else R.string.debts_new),
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
                    if (uiState.isEditMode && !uiState.isLoading && !uiState.notFound) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.debts_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
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
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DEBT_DIRECTION_OPTIONS.forEachIndexed { index, direction ->
                            SegmentedButton(
                                selected = uiState.direction == direction,
                                onClick = { onDirectionSelected(direction) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = DEBT_DIRECTION_OPTIONS.size),
                                label = { Text(text = stringResource(direction.labelRes())) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.personName,
                        onValueChange = onPersonNameChanged,
                        label = { Text(text = stringResource(R.string.debts_person_label)) },
                        isError = uiState.personNameErrorRes != null,
                        supportingText =
                            uiState.personNameErrorRes?.let { res ->
                                { Text(text = stringResource(res)) }
                            },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AmountTextField(
                        value = uiState.amountText,
                        onValueChange = onAmountChanged,
                        label = stringResource(R.string.debts_amount_label),
                        errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel(text = stringResource(R.string.debts_date_label))
                    OutlinedButton(
                        onClick = onOpenStartDatePicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = uiState.date.formatMedium())
                    }
                    if (uiState.dateErrorRes != null) {
                        Text(
                            text = stringResource(uiState.dateErrorRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldLabel(text = stringResource(R.string.debts_due_date_label))
                    OutlinedButton(
                        onClick = onOpenDueDatePicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.dueDate?.formatMedium() ?: stringResource(R.string.debts_due_date_none),
                        )
                    }
                    if (uiState.dueDate != null) {
                        TextButton(onClick = onClearDueDate) {
                            Text(text = stringResource(R.string.debts_due_date_clear))
                        }
                    }
                    if (uiState.dueDateErrorRes != null) {
                        Text(
                            text = stringResource(uiState.dueDateErrorRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChanged,
                        label = { Text(text = stringResource(R.string.debts_note_label)) },
                        isError = uiState.noteErrorRes != null,
                        supportingText =
                            uiState.noteErrorRes?.let { res ->
                                { Text(text = stringResource(res)) }
                            },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.debts_save))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (uiState.showStartDatePicker) {
        RecurringDatePickerDialog(
            initialDate = uiState.date,
            onDismiss = onDismissStartDatePicker,
            onDateSelected = onStartDateSelected,
        )
    }
    if (uiState.showDueDatePicker) {
        RecurringDatePickerDialog(
            initialDate = uiState.dueDate ?: uiState.date,
            onDismiss = onDismissDueDatePicker,
            onDateSelected = onDueDateSelected,
        )
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.debts_delete_title),
            message = stringResource(R.string.debts_delete_message, uiState.personName),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@StringRes
private fun DebtDirection.labelRes(): Int =
    when (this) {
        DebtDirection.OWED_TO_ME -> R.string.debts_direction_owed_to_me
        DebtDirection.I_OWE -> R.string.debts_direction_i_owe
    }

private fun previewDebtEditState(): DebtEditUiState =
    DebtEditUiState(
        isLoading = false,
        isEditMode = true,
        direction = DebtDirection.I_OWE,
        personName = "Luis",
        amountText = "20000",
        date = LocalDate.of(2026, 9, 1),
        dueDate = LocalDate.of(2026, 10, 30),
        note = "Préstamo para la moto",
    )

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun DebtEditScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        DebtEditScreen(
            uiState = previewDebtEditState(),
            onNavigateBack = {},
            onDirectionSelected = {},
            onPersonNameChanged = {},
            onAmountChanged = {},
            onOpenStartDatePicker = {},
            onDismissStartDatePicker = {},
            onStartDateSelected = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onDueDateSelected = {},
            onClearDueDate = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DebtEditScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        DebtEditScreen(
            uiState = previewDebtEditState(),
            onNavigateBack = {},
            onDirectionSelected = {},
            onPersonNameChanged = {},
            onAmountChanged = {},
            onOpenStartDatePicker = {},
            onDismissStartDatePicker = {},
            onStartDateSelected = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onDueDateSelected = {},
            onClearDueDate = {},
            onNoteChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
        )
    }
}
