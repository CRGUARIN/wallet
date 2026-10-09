package com.crguarin.wallet.ui.accounts

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.BottomActionBar
import com.crguarin.wallet.ui.components.ColorPicker
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.IconPicker
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.components.PrimaryActionButton
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AccountEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        AccountEditScreen(
            uiState = AccountEditUiState(),
            onNavigateBack = onNavigateBack,
            onNameChanged = {},
            onTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onSaveClick = onNavigateBack,
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountEditEvent.NavigateBack -> onNavigateBack()
                is AccountEditEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    AccountEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::setName,
        onTypeChanged = viewModel::setType,
        onInitialBalanceChanged = viewModel::setInitialBalanceText,
        onCreditLimitChanged = viewModel::setCreditLimitText,
        onStatementDayChanged = viewModel::setStatementDayText,
        onPaymentDueDayChanged = viewModel::setPaymentDueDayText,
        onFareChanged = viewModel::setFareText,
        onTransitBalanceNegativeChanged = viewModel::setTransitBalanceNegative,
        onColorSelected = viewModel::setColor,
        onIconSelected = viewModel::setIcon,
        onIncludeInTotalChanged = viewModel::setIncludeInTotal,
        onSaveClick = viewModel::saveAccount,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    uiState: AccountEditUiState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (AccountType) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onIconSelected: (String) -> Unit,
    onIncludeInTotalChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onCreditLimitChanged: (String) -> Unit = {},
    onStatementDayChanged: (String) -> Unit = {},
    onPaymentDueDayChanged: (String) -> Unit = {},
    onFareChanged: (String) -> Unit = {},
    onTransitBalanceNegativeChanged: (Boolean) -> Unit = {},
) {
    val spacing = MaterialTheme.walletSpacing
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            if (uiState.isEditMode) {
                                stringResource(R.string.account_edit_title)
                            } else {
                                stringResource(R.string.account_edit_new_title)
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
                BottomActionBar {
                    PrimaryActionButton(
                        text = stringResource(R.string.action_save),
                        onClick = onSaveClick,
                        enabled = uiState.isSaveEnabled,
                        isLoading = uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (uiState.notFound) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    message = stringResource(R.string.error_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                )
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            // Name Field
            WalletTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = stringResource(R.string.account_edit_name_label),
                errorRes = uiState.nameErrorRes,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Account Type Selector: a column of full-width options so labels fit at fontScale 2.
            FormSectionTitle(text = stringResource(R.string.account_edit_type_label))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                AccountTypeEditOption(
                    isSelected = uiState.type == AccountType.CASH,
                    enabled = !uiState.isTypeLocked,
                    onClick = { onTypeChanged(AccountType.CASH) },
                    icon = Icons.Default.AccountBalanceWallet,
                    label = stringResource(R.string.account_type_cash),
                )
                AccountTypeEditOption(
                    isSelected = uiState.type == AccountType.SAVINGS,
                    enabled = !uiState.isTypeLocked,
                    onClick = { onTypeChanged(AccountType.SAVINGS) },
                    icon = Icons.Default.Savings,
                    label = stringResource(R.string.account_type_savings),
                )
                AccountTypeEditOption(
                    isSelected = uiState.type == AccountType.CREDIT_CARD,
                    enabled = !uiState.isTypeLocked,
                    onClick = { onTypeChanged(AccountType.CREDIT_CARD) },
                    icon = Icons.Default.CreditCard,
                    label = stringResource(R.string.account_type_credit_card),
                )
                AccountTypeEditOption(
                    isSelected = uiState.type == AccountType.TRANSIT_CARD,
                    enabled = !uiState.isTypeLocked,
                    onClick = { onTypeChanged(AccountType.TRANSIT_CARD) },
                    icon = Icons.Default.DirectionsBus,
                    label = stringResource(R.string.account_type_transit_card),
                )
            }
            if (uiState.isTransitCard) {
                Text(
                    text = stringResource(R.string.account_type_transit_card_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.isTypeLocked) {
                Text(
                    text = stringResource(R.string.account_edit_type_locked_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Initial Balance Field (current debt for cards)
            Column {
                AmountTextField(
                    value = uiState.initialBalanceText,
                    onValueChange = onInitialBalanceChanged,
                    label =
                        when {
                            uiState.isCreditCard -> stringResource(R.string.card_current_debt_label)
                            uiState.isTransitCard -> stringResource(R.string.transit_current_balance_label)
                            else -> stringResource(R.string.account_edit_initial_balance_label)
                        },
                    errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (uiState.isTransitCard) {
                    Spacer(modifier = Modifier.height(spacing.s))
                    ToggleCard(
                        title = stringResource(R.string.transit_negative_balance_label),
                        subtitle =
                            if (uiState.transitBalanceNegative) {
                                stringResource(R.string.transit_negative_balance_supporting)
                            } else {
                                null
                            },
                        checked = uiState.transitBalanceNegative,
                        onCheckedChange = onTransitBalanceNegativeChanged,
                    )
                }

                if (uiState.showInitialBalanceWarning) {
                    Spacer(modifier = Modifier.height(spacing.s))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(spacing.s))
                            Text(
                                text = stringResource(R.string.account_edit_initial_balance_warning),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (uiState.isCreditCard) {
                AmountTextField(
                    value = uiState.creditLimitText,
                    onValueChange = onCreditLimitChanged,
                    label = stringResource(R.string.card_credit_limit_label),
                    errorMessage = uiState.creditLimitErrorRes?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                WalletTextField(
                    value = uiState.statementDayText,
                    onValueChange = onStatementDayChanged,
                    label = stringResource(R.string.card_statement_day_label),
                    supportingText = stringResource(R.string.card_day_hint),
                    errorRes = uiState.statementDayErrorRes,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                WalletTextField(
                    value = uiState.paymentDueDayText,
                    onValueChange = onPaymentDueDayChanged,
                    label = stringResource(R.string.card_payment_due_day_label),
                    supportingText = stringResource(R.string.card_day_hint),
                    errorRes = uiState.paymentDueDayErrorRes,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.isTransitCard) {
                Column {
                    AmountTextField(
                        value = uiState.fareText,
                        onValueChange = onFareChanged,
                        label = stringResource(R.string.transit_fare_label),
                        errorMessage = uiState.fareErrorRes?.let { stringResource(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.fareErrorRes == null) {
                        Text(
                            text = stringResource(R.string.transit_fare_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = spacing.l, top = spacing.xs),
                        )
                    }
                }
            }

            // Color Picker
            FormSectionTitle(text = stringResource(R.string.account_edit_color_label))

            val disabledColors = uiState.usedColors - uiState.color
            ColorPicker(
                selectedColor = uiState.color,
                onColorSelected = onColorSelected,
                disabledColors = disabledColors,
            )
            if (disabledColors.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.account_color_in_use_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Icon Picker
            FormSectionTitle(text = stringResource(R.string.account_edit_icon_label))

            IconPicker(
                selectedIcon = uiState.icon,
                onIconSelected = onIconSelected,
            )

            // Include in Total Switch
            ToggleCard(
                title = stringResource(R.string.account_edit_include_in_total_title),
                subtitle = stringResource(R.string.account_edit_include_in_total_subtitle),
                checked = uiState.includeInTotal,
                onCheckedChange = onIncludeInTotalChanged,
            )

            if (uiState.generalErrorRes != null) {
                Text(
                    text = stringResource(uiState.generalErrorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** One account type to choose among four: a tonal card with a leading icon and a check when chosen. */
@Composable
private fun AccountTypeEditOption(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = MaterialTheme.walletSpacing
    TonalOptionCard(
        selected = isSelected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(spacing.m))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Preview(name = "Account Edit Light", showBackground = true)
@Composable
private fun AccountEditPreviewLight() {
    WalletTheme(darkTheme = false) {
        AccountEditScreen(
            uiState =
                AccountEditUiState(
                    name = "Billetera",
                    initialBalanceText = "120000",
                ),
            onNavigateBack = {},
            onNameChanged = {},
            onTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onSaveClick = {},
        )
    }
}

@Preview(name = "Account Edit Dark", showBackground = true)
@Composable
private fun AccountEditPreviewDark() {
    WalletTheme(darkTheme = true) {
        AccountEditScreen(
            uiState =
                AccountEditUiState(
                    name = "Ahorros Bancolombia",
                    type = AccountType.SAVINGS,
                    isEditMode = true,
                ),
            onNavigateBack = {},
            onNameChanged = {},
            onTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onSaveClick = {},
        )
    }
}
