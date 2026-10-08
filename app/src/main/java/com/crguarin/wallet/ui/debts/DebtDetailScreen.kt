package com.crguarin.wallet.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtDetail
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.recurring.FieldLabel
import com.crguarin.wallet.ui.recurring.RecurringDatePickerDialog
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate

/**
 * Debt detail. The navigation graph calls [DebtDetailRoute] with [onNavigateBack] and [onEditClick]
 * (the debt id). A deleted debt leaves the screen through [onNavigateBack].
 */
@Composable
fun DebtDetailRoute(
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtDetailViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        DebtDetailScreen(
            uiState = DebtDetailUiState(isLoading = false),
            onNavigateBack = onNavigateBack,
            onEditClick = onEditClick,
            onAddPaymentClick = {},
            onPaymentAmountChanged = {},
            onOpenPaymentDatePicker = {},
            onDismissPaymentDatePicker = {},
            onPaymentDateSelected = {},
            onPaymentNoteChanged = {},
            onConfirmPayment = {},
            onDismissPaymentDialog = {},
            onSettleClick = {},
            onConfirmSettle = {},
            onDismissSettle = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onDeletePaymentClick = {},
            onConfirmDeletePayment = {},
            onDismissDeletePayment = {},
            modifier = modifier,
        )
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DebtDetailEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(resources.getString(event.messageRes))
                }
                is DebtDetailEvent.Deleted -> onNavigateBack()
            }
        }
    }

    DebtDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditClick = onEditClick,
        onAddPaymentClick = viewModel::openPaymentDialog,
        onPaymentAmountChanged = viewModel::setPaymentAmountText,
        onOpenPaymentDatePicker = viewModel::openPaymentDatePicker,
        onDismissPaymentDatePicker = viewModel::dismissPaymentDatePicker,
        onPaymentDateSelected = viewModel::setPaymentDate,
        onPaymentNoteChanged = viewModel::setPaymentNote,
        onConfirmPayment = viewModel::confirmPayment,
        onDismissPaymentDialog = viewModel::dismissPaymentDialog,
        onSettleClick = viewModel::openSettleDialog,
        onConfirmSettle = viewModel::confirmSettle,
        onDismissSettle = viewModel::dismissSettleDialog,
        onDeleteClick = viewModel::openDeleteDebt,
        onConfirmDelete = viewModel::confirmDeleteDebt,
        onDismissDelete = viewModel::dismissDeleteDebt,
        onDeletePaymentClick = viewModel::openDeletePayment,
        onConfirmDeletePayment = viewModel::confirmDeletePayment,
        onDismissDeletePayment = viewModel::dismissDeletePayment,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    uiState: DebtDetailUiState,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onAddPaymentClick: () -> Unit,
    onPaymentAmountChanged: (String) -> Unit,
    onOpenPaymentDatePicker: () -> Unit,
    onDismissPaymentDatePicker: () -> Unit,
    onPaymentDateSelected: (LocalDate) -> Unit,
    onPaymentNoteChanged: (String) -> Unit,
    onConfirmPayment: () -> Unit,
    onDismissPaymentDialog: () -> Unit,
    onSettleClick: () -> Unit,
    onConfirmSettle: () -> Unit,
    onDismissSettle: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onDeletePaymentClick: (DebtPayment) -> Unit,
    onConfirmDeletePayment: () -> Unit,
    onDismissDeletePayment: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val detail = uiState.detail
    val debt = detail?.summary?.debt
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = debt?.personName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (debt != null) {
                        IconButton(onClick = { onEditClick(debt.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.debts_edit),
                            )
                        }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }
            detail == null -> {
                ErrorState(
                    message = stringResource(R.string.error_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        DebtHeaderCard(detail = detail, today = uiState.today)
                    }
                    if (!detail.summary.isSettled) {
                        item(key = "actions") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onAddPaymentClick,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = stringResource(R.string.debts_add_payment))
                                }
                                OutlinedButton(
                                    onClick = onSettleClick,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = stringResource(R.string.debts_settle))
                                }
                            }
                        }
                    }
                    item(key = "payments_header") {
                        DetailSectionTitle(text = stringResource(R.string.debts_payments_title))
                    }
                    if (detail.payments.isEmpty()) {
                        item(key = "payments_empty") {
                            Text(
                                text = stringResource(R.string.debts_payments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(items = detail.payments, key = { payment -> "payment_${payment.id}" }) { payment ->
                            PaymentRow(
                                payment = payment,
                                onDeleteClick = { onDeletePaymentClick(payment) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showPaymentDialog) {
        PaymentDialog(
            uiState = uiState,
            onAmountChanged = onPaymentAmountChanged,
            onOpenDatePicker = onOpenPaymentDatePicker,
            onNoteChanged = onPaymentNoteChanged,
            onConfirm = onConfirmPayment,
            onDismiss = onDismissPaymentDialog,
        )
    }
    if (uiState.showPaymentDatePicker) {
        RecurringDatePickerDialog(
            initialDate = uiState.paymentDate,
            onDismiss = onDismissPaymentDatePicker,
            onDateSelected = onPaymentDateSelected,
        )
    }
    if (uiState.showSettleDialog && detail != null) {
        ConfirmDialog(
            title = stringResource(R.string.debts_settle_title),
            message = stringResource(R.string.debts_settle_message, detail.summary.remaining.formatDisplay(locale)),
            onConfirm = onConfirmSettle,
            onDismiss = onDismissSettle,
        )
    }
    if (uiState.showDeleteDialog && debt != null) {
        ConfirmDialog(
            title = stringResource(R.string.debts_delete_title),
            message = stringResource(R.string.debts_delete_message, debt.personName),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
    uiState.paymentToDelete?.let { payment ->
        ConfirmDialog(
            title = stringResource(R.string.debts_payment_delete_title),
            message = stringResource(R.string.debts_payment_delete_message, payment.amount.formatDisplay(locale)),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDeletePayment,
            onDismiss = onDismissDeletePayment,
        )
    }
}

@Composable
private fun DebtHeaderCard(
    detail: DebtDetail,
    today: LocalDate,
) {
    val summary = detail.summary
    val debt = summary.debt
    val title =
        if (debt.direction == DebtDirection.OWED_TO_ME) {
            stringResource(R.string.debts_detail_owed_to_me, debt.personName)
        } else {
            stringResource(R.string.debts_detail_i_owe, debt.personName)
        }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.debts_remaining_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(
                amount = summary.remaining,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { paymentProgress(paid = summary.paid, amount = debt.amount) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            DetailAmountLine(label = stringResource(R.string.debts_original_label), amount = debt.amount)
            DetailAmountLine(label = stringResource(R.string.debts_paid_label), amount = summary.paid)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.debts_since, debt.date.formatMedium()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            debt.dueDate?.let { dueDate ->
                if (summary.isOverdue(today)) {
                    Text(
                        text = stringResource(R.string.debts_overdue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.debts_due_on, dueDate.formatMedium()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            debt.note?.let { note ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.isSettled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.debts_settled_badge),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DetailAmountLine(
    label: String,
    amount: Money,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            amount = amount,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PaymentRow(
    payment: DebtPayment,
    onDeleteClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MoneyText(
                    amount = payment.amount,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = payment.date.formatMedium(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                payment.note?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.debts_payment_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Records a payment. The amount starts at what is still owed; errors show inline. */
@Composable
private fun PaymentDialog(
    uiState: DebtDetailUiState,
    onAmountChanged: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.debts_payment_dialog_title))
        },
        text = {
            Column {
                AmountTextField(
                    value = uiState.paymentAmountText,
                    onValueChange = onAmountChanged,
                    errorMessage = uiState.paymentAmountErrorRes?.let { stringResource(it) },
                    enabled = !uiState.isProcessing,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FieldLabel(text = stringResource(R.string.debts_date_label))
                TextButton(onClick = onOpenDatePicker, enabled = !uiState.isProcessing) {
                    Text(text = uiState.paymentDate.formatMedium())
                }
                uiState.paymentDateErrorRes?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.paymentNote,
                    onValueChange = onNoteChanged,
                    label = { Text(text = stringResource(R.string.debts_payment_note_label)) },
                    isError = uiState.paymentNoteErrorRes != null,
                    supportingText =
                        uiState.paymentNoteErrorRes?.let { res ->
                            { Text(text = stringResource(res)) }
                        },
                    enabled = !uiState.isProcessing,
                )
                uiState.dialogErrorRes?.let { res ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !uiState.isProcessing) {
                Text(text = stringResource(R.string.debts_add_payment))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isProcessing) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

private val previewInstant: Instant = Instant.EPOCH

private fun previewDebtDetailState(): DebtDetailUiState {
    val debt =
        Debt(
            id = 2L,
            personName = "Luis",
            direction = DebtDirection.I_OWE,
            amount = Money(2_000_000L),
            date = LocalDate.of(2026, 9, 1),
            dueDate = LocalDate.of(2026, 9, 30),
            note = "Préstamo para la moto",
            createdAt = previewInstant,
            updatedAt = previewInstant,
        )
    val payment =
        DebtPayment(
            id = 1L,
            debtId = 2L,
            amount = Money(500_000L),
            date = LocalDate.of(2026, 9, 10),
            note = "Primer abono",
            createdAt = previewInstant,
        )
    return DebtDetailUiState(
        isLoading = false,
        today = LocalDate.of(2026, 10, 4),
        detail =
            DebtDetail(
                summary = DebtSummary(debt = debt, paid = Money(500_000L)),
                payments = listOf(payment),
            ),
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun DebtDetailScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        DebtDetailScreen(
            uiState = previewDebtDetailState(),
            onNavigateBack = {},
            onEditClick = {},
            onAddPaymentClick = {},
            onPaymentAmountChanged = {},
            onOpenPaymentDatePicker = {},
            onDismissPaymentDatePicker = {},
            onPaymentDateSelected = {},
            onPaymentNoteChanged = {},
            onConfirmPayment = {},
            onDismissPaymentDialog = {},
            onSettleClick = {},
            onConfirmSettle = {},
            onDismissSettle = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onDeletePaymentClick = {},
            onConfirmDeletePayment = {},
            onDismissDeletePayment = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DebtDetailScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        DebtDetailScreen(
            uiState = previewDebtDetailState(),
            onNavigateBack = {},
            onEditClick = {},
            onAddPaymentClick = {},
            onPaymentAmountChanged = {},
            onOpenPaymentDatePicker = {},
            onDismissPaymentDatePicker = {},
            onPaymentDateSelected = {},
            onPaymentNoteChanged = {},
            onConfirmPayment = {},
            onDismissPaymentDialog = {},
            onSettleClick = {},
            onConfirmSettle = {},
            onDismissSettle = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onDeletePaymentClick = {},
            onConfirmDeletePayment = {},
            onDismissDeletePayment = {},
        )
    }
}
