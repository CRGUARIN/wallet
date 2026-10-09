package com.crguarin.wallet.ui.onboarding

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.ui.accounts.FormSectionTitle
import com.crguarin.wallet.ui.accounts.ToggleCard
import com.crguarin.wallet.ui.accounts.TonalOptionCard
import com.crguarin.wallet.ui.accounts.WalletTextField
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.BottomActionBar
import com.crguarin.wallet.ui.components.ColorPicker
import com.crguarin.wallet.ui.components.IconPicker
import com.crguarin.wallet.ui.components.PrimaryActionButton
import com.crguarin.wallet.ui.components.TonalPebble
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.AppLanguage
import com.crguarin.wallet.ui.util.safeHiltViewModel

@Composable
fun OnboardingRoute(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        OnboardingScreen(
            uiState = OnboardingUiState(),
            onLanguageSelected = {},
            onNextStep = {},
            onPreviousStep = {},
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onComplete = onNavigateToHome,
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultCashName = stringResource(R.string.account_default_cash_name)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onLanguageSelected = viewModel::setLanguage,
        onNextStep = {
            viewModel.suggestDefaultNameIfEmpty(defaultCashName)
            viewModel.goToStep(2)
        },
        onPreviousStep = { viewModel.goToStep(1) },
        onAccountNameChanged = viewModel::setAccountName,
        onAccountTypeChanged = viewModel::setAccountType,
        onInitialBalanceChanged = viewModel::setInitialBalanceText,
        onColorSelected = viewModel::setColor,
        onIconSelected = viewModel::setIcon,
        onIncludeInTotalChanged = viewModel::setIncludeInTotal,
        onComplete = viewModel::completeOnboarding,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onIconSelected: (String) -> Unit,
    onIncludeInTotalChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // No TopAppBar here, so the screen itself keeps clear of the status bar.
        contentWindowInsets = WindowInsets.statusBars,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomActionBar {
                if (uiState.step == 1) {
                    PrimaryActionButton(
                        text = stringResource(R.string.action_continue),
                        onClick = onNextStep,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    FilledTonalButton(
                        onClick = onPreviousStep,
                        modifier = Modifier.height(ONBOARDING_ACTION_HEIGHT),
                    ) {
                        Text(text = stringResource(R.string.action_previous), style = MaterialTheme.typography.titleMedium)
                    }
                    PrimaryActionButton(
                        text = stringResource(R.string.action_start),
                        onClick = onComplete,
                        enabled = uiState.isAccountValid,
                        isLoading = uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (uiState.step == 1) {
                OnboardingStepWelcome(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = onLanguageSelected,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                OnboardingStepFirstAccount(
                    uiState = uiState,
                    onAccountNameChanged = onAccountNameChanged,
                    onAccountTypeChanged = onAccountTypeChanged,
                    onInitialBalanceChanged = onInitialBalanceChanged,
                    onColorSelected = onColorSelected,
                    onIconSelected = onIconSelected,
                    onIncludeInTotalChanged = onIncludeInTotalChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepWelcome(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(spacing.xxxl))

        TonalPebble(
            icon = Icons.Default.AccountBalanceWallet,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            size = 96.dp,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(spacing.m))

        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )

        Spacer(modifier = Modifier.height(spacing.m))

        val languages =
            listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.SPANISH to stringResource(R.string.language_es),
                AppLanguage.ENGLISH to stringResource(R.string.language_en),
            )

        languages.forEach { (language, label) ->
            val isSelected = selectedLanguage == language
            TonalOptionCard(
                selected = isSelected,
                onClick = { onLanguageSelected(language) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xs),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(spacing.m))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xxl))
    }
}

@Composable
private fun OnboardingStepFirstAccount(
    uiState: OnboardingUiState,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onIconSelected: (String) -> Unit,
    onIncludeInTotalChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(spacing.l))

            Text(
                text = stringResource(R.string.onboarding_step_indicator, 2, 2),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(spacing.s))

            Text(
                text = stringResource(R.string.onboarding_first_account_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(spacing.s))

            Text(
                text = stringResource(R.string.onboarding_first_account_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Account Name
        WalletTextField(
            value = uiState.accountName,
            onValueChange = onAccountNameChanged,
            label = stringResource(R.string.account_edit_name_label),
            errorRes = uiState.nameErrorRes,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Account Type
        FormSectionTitle(
            text = stringResource(R.string.account_edit_type_label),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            AccountTypeOption(
                isSelected = uiState.accountType == AccountType.CASH,
                onClick = { onAccountTypeChanged(AccountType.CASH) },
                icon = Icons.Default.AccountBalanceWallet,
                label = stringResource(R.string.account_type_cash),
                modifier = Modifier.weight(1f),
            )
            AccountTypeOption(
                isSelected = uiState.accountType == AccountType.SAVINGS,
                onClick = { onAccountTypeChanged(AccountType.SAVINGS) },
                icon = Icons.Default.Savings,
                label = stringResource(R.string.account_type_savings),
                modifier = Modifier.weight(1f),
            )
        }

        // Initial Balance
        AmountTextField(
            value = uiState.initialBalanceText,
            onValueChange = onInitialBalanceChanged,
            label = stringResource(R.string.account_edit_initial_balance_label),
            errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Color Picker
        FormSectionTitle(
            text = stringResource(R.string.account_edit_color_label),
            modifier = Modifier.fillMaxWidth(),
        )

        ColorPicker(
            selectedColor = uiState.selectedColor,
            onColorSelected = onColorSelected,
        )

        // Icon Picker
        FormSectionTitle(
            text = stringResource(R.string.account_edit_icon_label),
            modifier = Modifier.fillMaxWidth(),
        )

        IconPicker(
            selectedIcon = uiState.selectedIcon,
            onIconSelected = onIconSelected,
        )

        // Include in total switch
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
            )
        }

        Spacer(modifier = Modifier.height(spacing.s))
    }
}

/** One of the two account types of the first account: a tonal card with a leading icon and a check when chosen. */
@Composable
private fun AccountTypeOption(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    TonalOptionCard(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(spacing.s))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            // The check keeps its room when off, so both cards are the same height.
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(CHECK_SIZE),
                )
            } else {
                Spacer(modifier = Modifier.size(CHECK_SIZE))
            }
        }
    }
}

private val CHECK_SIZE = 18.dp

private val ONBOARDING_ACTION_HEIGHT = 56.dp

@Preview(name = "Step 1 Light", showBackground = true)
@Composable
private fun OnboardingStep1PreviewLight() {
    WalletTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(step = 1),
            onLanguageSelected = {},
            onNextStep = {},
            onPreviousStep = {},
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onComplete = {},
        )
    }
}

@Preview(name = "Step 1 Dark", showBackground = true)
@Composable
private fun OnboardingStep1PreviewDark() {
    WalletTheme(darkTheme = true) {
        OnboardingScreen(
            uiState = OnboardingUiState(step = 1),
            onLanguageSelected = {},
            onNextStep = {},
            onPreviousStep = {},
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onComplete = {},
        )
    }
}

@Preview(name = "Step 2 Light", showBackground = true)
@Composable
private fun OnboardingStep2PreviewLight() {
    WalletTheme(darkTheme = false) {
        OnboardingScreen(
            uiState =
                OnboardingUiState(
                    step = 2,
                    accountName = "Efectivo",
                    accountType = AccountType.CASH,
                ),
            onLanguageSelected = {},
            onNextStep = {},
            onPreviousStep = {},
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onComplete = {},
        )
    }
}

@Preview(name = "Step 2 Dark", showBackground = true)
@Composable
private fun OnboardingStep2PreviewDark() {
    WalletTheme(darkTheme = true) {
        OnboardingScreen(
            uiState =
                OnboardingUiState(
                    step = 2,
                    accountName = "Efectivo",
                    accountType = AccountType.CASH,
                ),
            onLanguageSelected = {},
            onNextStep = {},
            onPreviousStep = {},
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onInitialBalanceChanged = {},
            onColorSelected = {},
            onIconSelected = {},
            onIncludeInTotalChanged = {},
            onComplete = {},
        )
    }
}
