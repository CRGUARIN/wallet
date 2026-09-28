package com.crguarin.wallet.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.ui.components.AmountTextField
import com.crguarin.wallet.ui.components.ColorPicker
import com.crguarin.wallet.ui.components.IconPicker
import com.crguarin.wallet.ui.theme.WalletTheme
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
        contentWindowInsets = WindowInsets(0),
        modifier = modifier.fillMaxSize(),
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
                    onContinue = onNextStep,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                OnboardingStepFirstAccount(
                    uiState = uiState,
                    onPrevious = onPreviousStep,
                    onAccountNameChanged = onAccountNameChanged,
                    onAccountTypeChanged = onAccountTypeChanged,
                    onInitialBalanceChanged = onInitialBalanceChanged,
                    onColorSelected = onColorSelected,
                    onIconSelected = onIconSelected,
                    onIncludeInTotalChanged = onIncludeInTotalChanged,
                    onComplete = onComplete,
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
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        val languages =
            listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.SPANISH to stringResource(R.string.language_es),
                AppLanguage.ENGLISH to stringResource(R.string.language_en),
            )

        languages.forEach { (language, label) ->
            val isSelected = selectedLanguage == language
            OutlinedCard(
                onClick = { onLanguageSelected(language) },
                shape = RoundedCornerShape(12.dp),
                border =
                    if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics(mergeDescendants = true) {},
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),
        ) {
            Text(
                text = stringResource(R.string.action_continue),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingStepFirstAccount(
    uiState: OnboardingUiState,
    onPrevious: () -> Unit,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (AccountType) -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onIconSelected: (String) -> Unit,
    onIncludeInTotalChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_step_indicator, 2, 2),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_first_account_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_first_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Account Name
        OutlinedTextField(
            value = uiState.accountName,
            onValueChange = onAccountNameChanged,
            label = { Text(text = stringResource(R.string.account_edit_name_label)) },
            isError = uiState.nameErrorRes != null,
            supportingText = uiState.nameErrorRes?.let { { Text(stringResource(it)) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account Type
        Text(
            text = stringResource(R.string.account_edit_type_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountTypeOption(
                type = AccountType.CASH,
                isSelected = uiState.accountType == AccountType.CASH,
                onClick = { onAccountTypeChanged(AccountType.CASH) },
                icon = Icons.Default.AccountBalanceWallet,
                label = stringResource(R.string.account_type_cash),
                modifier = Modifier.weight(1f),
            )
            AccountTypeOption(
                type = AccountType.SAVINGS,
                isSelected = uiState.accountType == AccountType.SAVINGS,
                onClick = { onAccountTypeChanged(AccountType.SAVINGS) },
                icon = Icons.Default.Savings,
                label = stringResource(R.string.account_type_savings),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Initial Balance
        AmountTextField(
            value = uiState.initialBalanceText,
            onValueChange = onInitialBalanceChanged,
            label = stringResource(R.string.account_edit_initial_balance_label),
            errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Color Picker
        Text(
            text = stringResource(R.string.account_edit_color_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        ColorPicker(
            selectedColor = uiState.selectedColor,
            onColorSelected = onColorSelected,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon Picker
        Text(
            text = stringResource(R.string.account_edit_icon_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        IconPicker(
            selectedIcon = uiState.selectedIcon,
            onIconSelected = onIconSelected,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Include in total switch
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.account_edit_include_in_total_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.account_edit_include_in_total_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = uiState.includeInTotal,
                    onCheckedChange = onIncludeInTotalChanged,
                )
            }
        }

        if (uiState.generalErrorRes != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(uiState.generalErrorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(50.dp),
            ) {
                Text(text = stringResource(R.string.action_previous))
            }

            Button(
                onClick = onComplete,
                enabled = uiState.isAccountValid,
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(50.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.action_start),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AccountTypeOption(
    type: AccountType,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border =
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

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
