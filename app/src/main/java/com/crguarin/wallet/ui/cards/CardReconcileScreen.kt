package com.crguarin.wallet.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.format
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.ui.accounts.TonalSelector
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.BottomActionBar
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.PrimaryActionButton
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.formatMedium
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Reconciles a credit card statement with the bank figures.
 *
 * @param onReviewTransactions navigates to the cycle transactions so the user can look for a
 * missing payment, refund or cashback. It receives the card [accountId] and the closing month in
 * ISO `yyyy-MM` format; the host navigation connects it to `Transactions(accountId, month)`.
 */
@Composable
fun CardReconcileRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReviewTransactions: (accountId: Long, month: String) -> Unit = { _, _ -> },
    viewModel: CardReconcileViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        CardReconcileScreen(
            uiState = CardReconcileUiState(isLoading = false, accountName = ""),
            onClosingDateSelected = {},
            onDueDateSelected = {},
            onOpenClosingDatePicker = {},
            onDismissClosingDatePicker = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onStatementBalanceChanged = {},
            onMinimumPaymentChanged = {},
            onInterestChanged = {},
            onCalculateClick = {},
            onSaveConfirmed = {},
            onForceAdjustConfirmed = {},
            onReviewTransactionsClick = {},
            onNavigateBack = onNavigateBack,
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.card_reconcile_saved)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CardReconcileEvent.ReconciliationSaved -> {
                    snackbarHostState.showSnackbar(savedMessage)
                    onNavigateBack()
                }
            }
        }
    }

    val closingDate = uiState.closingDate
    val closingDateText = closingDate?.formatMedium().orEmpty()
    val adjustmentNote =
        if (closingDate != null) {
            stringResource(R.string.card_reconcile_note, closingDateText)
        } else {
            ""
        }
    CardReconcileScreen(
        uiState = uiState,
        onClosingDateSelected = viewModel::setClosingDate,
        onDueDateSelected = viewModel::setDueDate,
        onOpenClosingDatePicker = viewModel::openClosingDatePicker,
        onDismissClosingDatePicker = viewModel::dismissClosingDatePicker,
        onOpenDueDatePicker = viewModel::openDueDatePicker,
        onDismissDueDatePicker = viewModel::dismissDueDatePicker,
        onStatementBalanceChanged = viewModel::setStatementBalanceText,
        onMinimumPaymentChanged = viewModel::setMinimumPaymentText,
        onInterestChanged = viewModel::setInterestText,
        onCalculateClick = viewModel::calculatePreview,
        onSaveConfirmed = { viewModel.save(adjustmentNote, false) },
        onForceAdjustConfirmed = { viewModel.save(adjustmentNote, true) },
        onReviewTransactionsClick = {
            if (closingDate != null) {
                onReviewTransactions(viewModel.accountId, YearMonth.from(closingDate).toString())
            }
        },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardReconcileScreen(
    uiState: CardReconcileUiState,
    onClosingDateSelected: (LocalDate) -> Unit,
    onDueDateSelected: (LocalDate) -> Unit,
    onOpenClosingDatePicker: () -> Unit,
    onDismissClosingDatePicker: () -> Unit,
    onOpenDueDatePicker: () -> Unit,
    onDismissDueDatePicker: () -> Unit,
    onStatementBalanceChanged: (String) -> Unit,
    onMinimumPaymentChanged: (String) -> Unit,
    onInterestChanged: (String) -> Unit,
    onCalculateClick: () -> Unit,
    onSaveConfirmed: () -> Unit,
    onForceAdjustConfirmed: () -> Unit,
    onReviewTransactionsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = MaterialTheme.walletSpacing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            uiState.accountName.ifBlank {
                                stringResource(R.string.card_reconcile_title)
                            },
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
            )
        },
        bottomBar = {
            if (!uiState.isLoading && !uiState.notFound) {
                // One main action at a time: work out the difference first, then save it.
                BottomActionBar {
                    if (uiState.preview == null) {
                        PrimaryActionButton(
                            text = stringResource(R.string.card_reconcile_calculate),
                            onClick = onCalculateClick,
                            enabled = uiState.isCalculateEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        PrimaryActionButton(
                            text = stringResource(R.string.card_reconcile_confirm),
                            onClick = onSaveConfirmed,
                            enabled = uiState.isSaveEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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
                            .padding(spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(spacing.l),
                ) {
                    Text(
                        text = stringResource(R.string.card_reconcile_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TonalSelector(
                        label = stringResource(R.string.card_reconcile_closing_date),
                        value = uiState.closingDate?.formatMedium().orEmpty(),
                        leadingIcon = Icons.Default.CalendarMonth,
                        onClick = onOpenClosingDatePicker,
                    )

                    TonalSelector(
                        label = stringResource(R.string.card_reconcile_due_date),
                        value = uiState.dueDate?.formatMedium().orEmpty(),
                        leadingIcon = Icons.Default.CalendarMonth,
                        onClick = onOpenDueDatePicker,
                    )

                    AmountTextField(
                        value = uiState.statementBalanceText,
                        onValueChange = onStatementBalanceChanged,
                        label = stringResource(R.string.card_reconcile_statement_balance),
                        errorMessage =
                            uiState.statementBalanceErrorRes?.let { stringResource(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    AmountTextField(
                        value = uiState.minimumPaymentText,
                        onValueChange = onMinimumPaymentChanged,
                        label = stringResource(R.string.card_reconcile_minimum_payment),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    AmountTextField(
                        value = uiState.interestText,
                        onValueChange = onInterestChanged,
                        label = stringResource(R.string.card_reconcile_interest),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.generalErrorRes != null) {
                        Text(
                            text = stringResource(uiState.generalErrorRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    uiState.preview?.let { preview ->
                        ReconciliationResult(
                            preview = preview,
                            statedInterest = uiState.interestText.ifBlank { null }?.let { MoneyParser.parse(it) },
                            onForceAdjustConfirmed = onForceAdjustConfirmed,
                            onReviewTransactionsClick = onReviewTransactionsClick,
                            isSaveEnabled = uiState.isSaveEnabled,
                        )
                    }
                }
            }
        }
    }

    if (uiState.showClosingDatePicker && uiState.closingDate != null) {
        ReconcileDatePickerDialog(
            initialDate = uiState.closingDate,
            onDismiss = onDismissClosingDatePicker,
            onDateSelected = onClosingDateSelected,
        )
    }

    if (uiState.showDueDatePicker && uiState.dueDate != null) {
        ReconcileDatePickerDialog(
            initialDate = uiState.dueDate,
            onDismiss = onDismissDueDatePicker,
            onDateSelected = onDueDateSelected,
        )
    }
}

/**
 * What the reconciliation found, on a tonal layer. Saving is the main action of the bottom bar, so
 * only the secondary choices of a negative difference live here.
 */
@Composable
private fun ReconciliationResult(
    preview: ReconciliationPreview,
    statedInterest: Money?,
    onForceAdjustConfirmed: () -> Unit,
    onReviewTransactionsClick: () -> Unit,
    isSaveEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val difference = preview.difference
    val closingText = preview.closingDate.formatMedium(locale)
    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.m)) {
            ResultRow(
                label = stringResource(R.string.card_reconcile_app_debt),
                amount = preview.appDebt,
            )
            ResultRow(
                label = stringResource(R.string.card_reconcile_statement_balance),
                amount = preview.statementBalance,
            )
            ResultRow(
                label = stringResource(R.string.card_reconcile_difference),
                amount = difference,
                type =
                    when {
                        difference.isPositive -> TransactionType.EXPENSE
                        difference.isNegative -> TransactionType.INCOME
                        else -> null
                    },
                sign =
                    when {
                        difference.isPositive -> MoneySign.PLUS
                        difference.isNegative -> MoneySign.MINUS
                        else -> MoneySign.NONE
                    },
            )

            when {
                difference == Money.ZERO -> {
                    Text(
                        text = stringResource(R.string.card_reconcile_matches),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                difference.isPositive -> {
                    Text(
                        text = stringResource(R.string.card_reconcile_positive, difference.format(locale), closingText),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (preview.interestMismatch && statedInterest != null) {
                        Text(
                            text =
                                stringResource(
                                    R.string.card_reconcile_mismatch,
                                    difference.format(locale),
                                    statedInterest.format(locale),
                                ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.card_reconcile_negative, (-difference).format(locale)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(
                        onClick = onReviewTransactionsClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = SECONDARY_ACTION_HEIGHT),
                    ) {
                        Text(text = stringResource(R.string.card_reconcile_review_transactions))
                    }
                    FilledTonalButton(
                        onClick = onForceAdjustConfirmed,
                        enabled = isSaveEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = SECONDARY_ACTION_HEIGHT),
                    ) {
                        Text(text = stringResource(R.string.card_reconcile_force_adjust))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    amount: Money,
    type: TransactionType? = null,
    sign: MoneySign = MoneySign.NONE,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            amount = amount,
            type = type,
            sign = sign,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private val SECONDARY_ACTION_HEIGHT = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReconcileDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(name = "Reconcile form Light", showBackground = true)
@Composable
private fun CardReconcileScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        CardReconcileScreen(
            uiState =
                CardReconcileUiState(
                    isLoading = false,
                    accountName = "RappiCard",
                    closingDate = LocalDate.of(2026, 9, 15),
                    dueDate = LocalDate.of(2026, 10, 5),
                    statementBalanceText = "1250000",
                ),
            onClosingDateSelected = {},
            onDueDateSelected = {},
            onOpenClosingDatePicker = {},
            onDismissClosingDatePicker = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onStatementBalanceChanged = {},
            onMinimumPaymentChanged = {},
            onInterestChanged = {},
            onCalculateClick = {},
            onSaveConfirmed = {},
            onForceAdjustConfirmed = {},
            onReviewTransactionsClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "Reconcile result Dark", showBackground = true)
@Composable
private fun CardReconcileScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        CardReconcileScreen(
            uiState =
                CardReconcileUiState(
                    isLoading = false,
                    accountName = "RappiCard",
                    closingDate = LocalDate.of(2026, 9, 15),
                    dueDate = LocalDate.of(2026, 10, 5),
                    statementBalanceText = "1250000",
                    preview =
                        ReconciliationPreview(
                            accountId = 1L,
                            closingDate = LocalDate.of(2026, 9, 15),
                            appDebt = Money(1_200_000_00L),
                            statementBalance = Money(1_250_000_00L),
                            difference = Money(50_000_00L),
                            interestMismatch = false,
                        ),
                ),
            onClosingDateSelected = {},
            onDueDateSelected = {},
            onOpenClosingDatePicker = {},
            onDismissClosingDatePicker = {},
            onOpenDueDatePicker = {},
            onDismissDueDatePicker = {},
            onStatementBalanceChanged = {},
            onMinimumPaymentChanged = {},
            onInterestChanged = {},
            onCalculateClick = {},
            onSaveConfirmed = {},
            onForceAdjustConfirmed = {},
            onReviewTransactionsClick = {},
            onNavigateBack = {},
        )
    }
}
