package com.crguarin.wallet.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.NotificationPermissionState
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.components.TonalPebble
import com.crguarin.wallet.ui.components.rememberNotificationPermissionState
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.AppLanguage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    onNavigateBack: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {},
    onNotificationsEnabledChanged: (Boolean) -> Unit = {},
    onNotificationTimeChanged: (Int, Int) -> Unit = { _, _ -> },
    onRecurringRemindersChanged: (Boolean) -> Unit = {},
    onRecurringPostedChanged: (Boolean) -> Unit = {},
    onRecurringPendingChanged: (Boolean) -> Unit = {},
    onCardClosingChanged: (Boolean) -> Unit = {},
    onCardClosingDaysChanged: (Int) -> Unit = {},
    onCardPaymentChanged: (Boolean) -> Unit = {},
    onCardPaymentDaysChanged: (Int) -> Unit = {},
    onCardOverdueChanged: (Boolean) -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    permissionState: NotificationPermissionState? = null,
) {
    val permission = permissionState ?: rememberNotificationPermissionState()
    // The test notification is sent right away when allowed; otherwise it waits for the permission
    // result (or for the user to enable notifications in system settings) before sending.
    var pendingTestNotification by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(permission.granted) {
        if (pendingTestNotification && permission.granted) {
            pendingTestNotification = false
            onSendTestNotification()
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val spacing = MaterialTheme.walletSpacing
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screenGutter)
                        .padding(bottom = spacing.xxl),
            ) {
                // Ordered by how often they are used.
                DataSection(onCategoriesClick, onRecurringClick, onDebtsClick)
                NotificationsSection(
                    settings = uiState.notificationSettings,
                    hasCreditCard = uiState.hasCreditCard,
                    permissionGranted = permission.granted,
                    onRequestPermission = permission::request,
                    onEnabledChanged = {
                        if (it && !permission.granted) permission.request()
                        onNotificationsEnabledChanged(it)
                    },
                    onTimeChanged = onNotificationTimeChanged,
                    onRecurringRemindersChanged = onRecurringRemindersChanged,
                    onRecurringPostedChanged = onRecurringPostedChanged,
                    onRecurringPendingChanged = onRecurringPendingChanged,
                    onCardClosingChanged = onCardClosingChanged,
                    onCardClosingDaysChanged = onCardClosingDaysChanged,
                    onCardPaymentChanged = onCardPaymentChanged,
                    onCardPaymentDaysChanged = onCardPaymentDaysChanged,
                    onCardOverdueChanged = onCardOverdueChanged,
                    onSendTestNotification = {
                        if (permission.granted) {
                            onSendTestNotification()
                        } else {
                            pendingTestNotification = true
                            permission.request()
                        }
                    },
                )
                ThemeSection(uiState, dynamicColorSupported, onThemeSelected, onDynamicColorChanged)
                LanguageSection(uiState.language, onLanguageSelected)
                BackupSection(uiState.isBusy, onExportClick, onImportClick)
                AboutSection(appVersion)
            }
        }
    }

    uiState.pendingImport?.let { summary ->
        val debtsLost = uiState.pendingImportDebtsLost
        val debtsWarning =
            if (debtsLost > 0) {
                "\n\n" + pluralStringResource(R.plurals.backup_import_debts_lost, debtsLost, debtsLost)
            } else {
                ""
            }
        ConfirmDialog(
            title = stringResource(R.string.backup_import_confirm_title),
            message = "${summaryText(summary)}\n\n${stringResource(R.string.backup_import_warning)}$debtsWarning",
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
private fun summaryText(summary: BackupSummary): String {
    val base = baseSummaryText(summary)
    if (summary.debts == 0) return base
    val debts = pluralStringResource(R.plurals.backup_summary_debts, summary.debts, summary.debts)
    return stringResource(R.string.backup_summary_append, base, debts)
}

@Composable
private fun baseSummaryText(summary: BackupSummary): String {
    val accounts = pluralStringResource(R.plurals.backup_summary_accounts, summary.accounts, summary.accounts)
    val categories = pluralStringResource(R.plurals.backup_summary_categories, summary.categories, summary.categories)
    val transactions =
        pluralStringResource(R.plurals.backup_summary_transactions, summary.transactions, summary.transactions)
    val statements =
        pluralStringResource(R.plurals.backup_summary_statements, summary.cardStatements, summary.cardStatements)
    val recurring =
        pluralStringResource(
            R.plurals.backup_summary_recurring,
            summary.recurringPayments,
            summary.recurringPayments,
        )
    return when {
        summary.cardStatements > 0 && summary.recurringPayments > 0 -> {
            stringResource(
                R.string.backup_summary_format_with_statements_recurring,
                accounts,
                categories,
                transactions,
                statements,
                recurring,
            )
        }

        summary.cardStatements > 0 -> {
            stringResource(
                R.string.backup_summary_format_with_statements,
                accounts,
                categories,
                transactions,
                statements,
            )
        }

        summary.recurringPayments > 0 -> {
            stringResource(
                R.string.backup_summary_format_with_recurring,
                accounts,
                categories,
                transactions,
                recurring,
            )
        }

        else -> {
            stringResource(R.string.backup_summary_format, accounts, categories, transactions)
        }
    }
}

/** Title of a settings section, above its tonal card. */
@Composable
private fun SectionHeader(text: String) {
    val spacing = MaterialTheme.walletSpacing
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.l, end = spacing.l, top = spacing.xl, bottom = spacing.s)
                .semantics { heading() },
    )
}

/** Title of a group inside a card (for example the recurring payments switches), on the card layer. */
@Composable
private fun GroupHeader(text: String) {
    val spacing = MaterialTheme.walletSpacing
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.l, end = spacing.l, top = spacing.m, bottom = spacing.xs)
                .semantics { heading() },
    )
}

/** A settings section: its [title] and a `surfaceContainerLow` card with the rows. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionHeader(title)
    SectionCard(contentPadding = PaddingValues(vertical = MaterialTheme.walletSpacing.s), content = content)
}

/** [ListItem] that lets the card layer show through. */
@Composable
private fun SettingsListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = headlineContent,
        modifier = modifier,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** Leading icon of a row: a small tonal pebble. */
@Composable
private fun RowIcon(icon: ImageVector) {
    TonalPebble(
        icon = icon,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        size = ROW_ICON_SIZE,
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
    )
}

private val ROW_ICON_SIZE = 40.dp

@Composable
private fun LanguageSection(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_section_language)) {
        RadioOption(stringResource(R.string.language_system), selected == AppLanguage.SYSTEM) { onSelected(AppLanguage.SYSTEM) }
        RadioOption(stringResource(R.string.language_es), selected == AppLanguage.SPANISH) { onSelected(AppLanguage.SPANISH) }
        RadioOption(stringResource(R.string.language_en), selected == AppLanguage.ENGLISH) { onSelected(AppLanguage.ENGLISH) }
    }
}

@Composable
private fun ThemeSection(
    uiState: SettingsUiState,
    dynamicColorSupported: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_section_theme)) {
        RadioOption(stringResource(R.string.theme_system), uiState.themeMode == ThemeMode.SYSTEM) { onThemeSelected(ThemeMode.SYSTEM) }
        RadioOption(stringResource(R.string.theme_light), uiState.themeMode == ThemeMode.LIGHT) { onThemeSelected(ThemeMode.LIGHT) }
        RadioOption(stringResource(R.string.theme_dark), uiState.themeMode == ThemeMode.DARK) { onThemeSelected(ThemeMode.DARK) }
        if (dynamicColorSupported) {
            SettingsListItem(
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
    }
}

@Composable
private fun DataSection(
    onCategoriesClick: () -> Unit,
    onRecurringClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {},
) {
    SettingsSection(stringResource(R.string.settings_section_data)) {
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_item_categories)) },
            supportingContent = { Text(text = stringResource(R.string.settings_item_categories_subtitle)) },
            leadingContent = { RowIcon(Icons.Default.Category) },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable(onClick = onCategoriesClick),
        )
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_item_recurring)) },
            supportingContent = { Text(text = stringResource(R.string.recurring_settings_subtitle)) },
            leadingContent = { RowIcon(Icons.Default.Repeat) },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable(onClick = onRecurringClick),
        )
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.debts_title)) },
            supportingContent = { Text(text = stringResource(R.string.debts_settings_subtitle)) },
            leadingContent = { RowIcon(Icons.Default.Payments) },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable(onClick = onDebtsClick),
        )
    }
}

private val CARD_LEAD_DAY_OPTIONS = listOf(0, 1, 2, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSection(
    settings: NotificationSettings,
    hasCreditCard: Boolean,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onTimeChanged: (Int, Int) -> Unit,
    onRecurringRemindersChanged: (Boolean) -> Unit,
    onRecurringPostedChanged: (Boolean) -> Unit,
    onRecurringPendingChanged: (Boolean) -> Unit,
    onCardClosingChanged: (Boolean) -> Unit,
    onCardClosingDaysChanged: (Int) -> Unit,
    onCardPaymentChanged: (Boolean) -> Unit,
    onCardPaymentDaysChanged: (Int) -> Unit,
    onCardOverdueChanged: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24Hour =
        android.text.format.DateFormat
            .is24HourFormat(context)
    val timeText =
        LocalTime.of(settings.hour, settings.minute).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
        )

    SettingsSection(stringResource(R.string.settings_section_notifications)) {
        SwitchRow(
            title = stringResource(R.string.settings_notifications_enabled),
            checked = settings.enabled,
            enabled = true,
            onCheckedChange = onEnabledChanged,
        )
        if (settings.enabled && !permissionGranted) {
            SettingsListItem(
                headlineContent = { Text(text = stringResource(R.string.settings_notifications_blocked)) },
                supportingContent = {
                    TextButton(onClick = onRequestPermission) {
                        Text(text = stringResource(R.string.settings_notifications_open_system))
                    }
                },
            )
        }
        val controlsEnabled = settings.enabled
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_notifications_time)) },
            supportingContent = {
                Text(text = stringResource(R.string.settings_notifications_time_value, timeText))
            },
            leadingContent = { RowIcon(Icons.Default.Schedule) },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable(enabled = controlsEnabled, role = Role.Button) { showTimePicker = true },
        )
        GroupHeader(stringResource(R.string.settings_notifications_recurring_header))
        SwitchRow(
            title = stringResource(R.string.settings_notif_recurring_reminders),
            subtitle = stringResource(R.string.settings_notif_recurring_reminders_desc),
            checked = settings.recurringReminders,
            enabled = controlsEnabled,
            onCheckedChange = onRecurringRemindersChanged,
        )
        SwitchRow(
            title = stringResource(R.string.settings_notif_recurring_posted),
            checked = settings.recurringPosted,
            enabled = controlsEnabled,
            onCheckedChange = onRecurringPostedChanged,
        )
        SwitchRow(
            title = stringResource(R.string.settings_notif_recurring_pending),
            checked = settings.recurringPendingConfirm,
            enabled = controlsEnabled,
            onCheckedChange = onRecurringPendingChanged,
        )
        if (hasCreditCard) {
            GroupHeader(stringResource(R.string.settings_notifications_cards_header))
            SwitchRow(
                title = stringResource(R.string.settings_notif_card_closing),
                checked = settings.cardClosing,
                enabled = controlsEnabled,
                onCheckedChange = onCardClosingChanged,
            )
            LeadDaysSelector(
                selectedDays = settings.cardClosingDaysBefore,
                enabled = controlsEnabled && settings.cardClosing,
                onSelected = onCardClosingDaysChanged,
            )
            SwitchRow(
                title = stringResource(R.string.settings_notif_card_payment),
                checked = settings.cardPayment,
                enabled = controlsEnabled,
                onCheckedChange = onCardPaymentChanged,
            )
            LeadDaysSelector(
                selectedDays = settings.cardPaymentDaysBefore,
                enabled = controlsEnabled && settings.cardPayment,
                onSelected = onCardPaymentDaysChanged,
            )
            SwitchRow(
                title = stringResource(R.string.settings_notif_card_overdue),
                checked = settings.cardOverdue,
                enabled = controlsEnabled,
                onCheckedChange = onCardOverdueChanged,
            )
        }
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_notifications_test)) },
            modifier =
                Modifier.clickable(
                    enabled = controlsEnabled,
                    role = Role.Button,
                    onClick = onSendTestNotification,
                ),
        )
    }

    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = settings.hour,
                initialMinute = settings.minute,
                is24Hour = is24Hour,
            )
        TimePickerDialog(
            title = { Text(text = stringResource(R.string.settings_notifications_time)) },
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        onTimeChanged(timePickerState.hour, timePickerState.minute)
                    },
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    SettingsListItem(
        headlineContent = { Text(text = title) },
        supportingContent = subtitle?.let { { Text(text = it) } },
        trailingContent = { Switch(checked = checked, enabled = enabled, onCheckedChange = null) },
        modifier =
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    )
}

@Composable
private fun LeadDaysSelector(
    selectedDays: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
        if (selectedDays == 0) {
            stringResource(R.string.settings_notif_same_day)
        } else {
            pluralStringResource(R.plurals.settings_notif_days_before, selectedDays, selectedDays)
        }
    Box {
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_notif_lead_days)) },
            supportingContent = { Text(text = selectedLabel) },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CARD_LEAD_DAY_OPTIONS.forEach { days ->
                val label =
                    if (days == 0) {
                        stringResource(R.string.settings_notif_same_day)
                    } else {
                        pluralStringResource(R.plurals.settings_notif_days_before, days, days)
                    }
                DropdownMenuItem(
                    text = { Text(text = label) },
                    onClick = {
                        expanded = false
                        onSelected(days)
                    },
                )
            }
        }
    }
}

@Composable
private fun BackupSection(
    isBusy: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_section_backup)) {
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.backup_export_title)) },
            supportingContent = { Text(text = stringResource(R.string.backup_export_subtitle)) },
            leadingContent = { RowIcon(Icons.Default.FileUpload) },
            modifier = Modifier.clickable(enabled = !isBusy, role = Role.Button, onClick = onExportClick),
        )
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.backup_import_title)) },
            supportingContent = { Text(text = stringResource(R.string.backup_import_subtitle)) },
            leadingContent = { RowIcon(Icons.Default.FileDownload) },
            modifier = Modifier.clickable(enabled = !isBusy, role = Role.Button, onClick = onImportClick),
        )
    }
}

@Composable
private fun AboutSection(appVersion: String) {
    SettingsSection(stringResource(R.string.settings_section_about)) {
        SettingsListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_about_version)) },
            supportingContent = { Text(text = appVersion) },
            leadingContent = { RowIcon(Icons.Default.Info) },
        )
    }
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SettingsListItem(
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
