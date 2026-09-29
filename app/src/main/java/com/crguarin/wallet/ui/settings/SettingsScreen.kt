package com.crguarin.wallet.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    appVersion: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onCategoriesClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    modifier: Modifier = Modifier,
    dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BusyIndicator(isBusy = uiState.isBusy)
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LanguageSection(uiState.language, onLanguageSelected)
                ThemeSection(uiState, dynamicColorSupported, onThemeSelected, onDynamicColorChanged)
                DataSection(onCategoriesClick)
                BackupSection(uiState.isBusy, onExportClick, onImportClick)
                AboutSection(appVersion)
            }
        }
    }

    uiState.pendingImport?.let { summary ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_confirm_title),
            message = "${summaryText(summary)}\n\n${stringResource(R.string.backup_import_warning)}",
            confirmText = stringResource(R.string.backup_import_confirm_action),
            isDestructive = true,
            onConfirm = onConfirmImport,
            onDismiss = onDismissImport,
        )
    }
}

@Composable
private fun BusyIndicator(isBusy: Boolean) {
    val description = stringResource(R.string.backup_in_progress)
    if (isBusy) {
        LinearProgressIndicator(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun summaryText(summary: BackupSummary): String =
    stringResource(
        R.string.backup_summary_format,
        pluralStringResource(R.plurals.backup_summary_accounts, summary.accounts, summary.accounts),
        pluralStringResource(R.plurals.backup_summary_categories, summary.categories, summary.categories),
        pluralStringResource(R.plurals.backup_summary_transactions, summary.transactions, summary.transactions),
    )

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)),
    )
}

@Composable
private fun LanguageSection(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_language))
    RadioOption(stringResource(R.string.language_system), selected == AppLanguage.SYSTEM) { onSelected(AppLanguage.SYSTEM) }
    RadioOption(stringResource(R.string.language_es), selected == AppLanguage.SPANISH) { onSelected(AppLanguage.SPANISH) }
    RadioOption(stringResource(R.string.language_en), selected == AppLanguage.ENGLISH) { onSelected(AppLanguage.ENGLISH) }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ThemeSection(
    uiState: SettingsUiState,
    dynamicColorSupported: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_theme))
    RadioOption(stringResource(R.string.theme_system), uiState.themeMode == ThemeMode.SYSTEM) { onThemeSelected(ThemeMode.SYSTEM) }
    RadioOption(stringResource(R.string.theme_light), uiState.themeMode == ThemeMode.LIGHT) { onThemeSelected(ThemeMode.LIGHT) }
    RadioOption(stringResource(R.string.theme_dark), uiState.themeMode == ThemeMode.DARK) { onThemeSelected(ThemeMode.DARK) }
    if (dynamicColorSupported) {
        ListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_dynamic_color)) },
            supportingContent = { Text(text = stringResource(R.string.settings_dynamic_color_subtitle)) },
            trailingContent = { Switch(checked = uiState.dynamicColor, onCheckedChange = null) },
            modifier =
                Modifier.toggleable(
                    value = uiState.dynamicColor,
                    role = Role.Switch,
                    onValueChange = onDynamicColorChanged,
                ),
        )
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DataSection(onCategoriesClick: () -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_data))
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.settings_item_categories)) },
        supportingContent = { Text(text = stringResource(R.string.settings_item_categories_subtitle)) },
        leadingContent = { Icon(imageVector = Icons.Default.Category, contentDescription = null) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = Modifier.clickable(onClick = onCategoriesClick),
    )
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BackupSection(
    isBusy: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_backup))
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.backup_export_title)) },
        supportingContent = { Text(text = stringResource(R.string.backup_export_subtitle)) },
        leadingContent = { Icon(imageVector = Icons.Default.FileUpload, contentDescription = null) },
        modifier = Modifier.clickable(enabled = !isBusy, role = Role.Button, onClick = onExportClick),
    )
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.backup_import_title)) },
        supportingContent = { Text(text = stringResource(R.string.backup_import_subtitle)) },
        leadingContent = { Icon(imageVector = Icons.Default.FileDownload, contentDescription = null) },
        modifier = Modifier.clickable(enabled = !isBusy, role = Role.Button, onClick = onImportClick),
    )
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun AboutSection(appVersion: String) {
    SectionHeader(stringResource(R.string.settings_section_about))
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.settings_about_version)) },
        supportingContent = { Text(text = appVersion) },
        leadingContent = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
    )
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun PreviewContent(
    darkTheme: Boolean,
    uiState: SettingsUiState,
) {
    WalletTheme(darkTheme = darkTheme) {
        SettingsScreen(
            uiState = uiState,
            appVersion = "1.0.0",
            onLanguageSelected = {},
            onThemeSelected = {},
            onDynamicColorChanged = {},
            onCategoriesClick = {},
            onExportClick = {},
            onImportClick = {},
            onConfirmImport = {},
            onDismissImport = {},
            dynamicColorSupported = true,
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewLight() {
    PreviewContent(darkTheme = false, uiState = SettingsUiState())
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun SettingsScreenPreviewDark() {
    PreviewContent(darkTheme = true, uiState = SettingsUiState(themeMode = ThemeMode.DARK, isBusy = true))
}

@Preview(name = "Confirm import", showBackground = true)
@Composable
private fun SettingsScreenPreviewConfirm() {
    PreviewContent(darkTheme = false, uiState = SettingsUiState(pendingImport = BackupSummary(3, 25, 480)))
}
