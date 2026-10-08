package com.crguarin.wallet.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.R
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import com.crguarin.wallet.domain.repository.NotificationGateway
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.ExportBackupUseCase
import com.crguarin.wallet.domain.usecase.ImportBackupUseCase
import com.crguarin.wallet.domain.usecase.PreviewBackupUseCase
import com.crguarin.wallet.ui.util.AppLanguage
import com.crguarin.wallet.ui.util.AppLanguageController
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** True while a backup is being exported, read or imported. */
    val isBusy: Boolean = false,
    /** Summary of a validated backup waiting for the user's confirmation to replace their data. */
    val pendingImport: BackupSummary? = null,
    /** Debts stored now that confirming [pendingImport] would delete because the backup has none. */
    val pendingImportDebtsLost: Int = 0,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    /** True when at least one credit card exists: shows the card notification controls. */
    val hasCreditCard: Boolean = false,
)

sealed interface SettingsEvent {
    data class ShowSnackbar(
        @StringRes val messageRes: Int,
    ) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val languageController: AppLanguageController,
        private val exportBackupUseCase: ExportBackupUseCase,
        private val previewBackupUseCase: PreviewBackupUseCase,
        private val importBackupUseCase: ImportBackupUseCase,
        private val timeProvider: TimeProvider,
        private val accountRepository: AccountRepository,
        private val dailyWorkScheduler: DailyWorkScheduler,
        private val notificationGateway: NotificationGateway,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState(language = languageController.current()))
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
        val events: Flow<SettingsEvent> = _events.receiveAsFlow()

        /** Validated backup content kept between the validation and the user's confirmation. */
        private var pendingImportJson: String? = null

        init {
            viewModelScope.launch {
                preferencesRepository.observeUserPreferences().collect { preferences ->
                    _uiState.update {
                        it.copy(themeMode = preferences.themeMode, dynamicColor = preferences.dynamicColor)
                    }
                }
            }
            viewModelScope.launch {
                preferencesRepository.observeNotificationSettings().collect { settings ->
                    _uiState.update { it.copy(notificationSettings = settings) }
                }
            }
            viewModelScope.launch {
                accountRepository.observeAllAccounts().collect { accounts ->
                    _uiState.update {
                        it.copy(hasCreditCard = accounts.any { account -> account.type == AccountType.CREDIT_CARD })
                    }
                }
            }
        }

        fun setLanguage(language: AppLanguage) {
            languageController.set(language)
            _uiState.update { it.copy(language = language) }
        }

        fun setThemeMode(themeMode: ThemeMode) {
            viewModelScope.launch { preferencesRepository.setThemeMode(themeMode) }
        }

        fun setDynamicColor(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
        }

        fun setNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(enabled = enabled) }
            }
        }

        fun setNotificationTime(
            hour: Int,
            minute: Int,
        ) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(hour = hour, minute = minute) }
                dailyWorkScheduler.schedule(hour, minute, replace = true)
            }
        }

        fun setRecurringReminders(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(recurringReminders = enabled) }
            }
        }

        fun setRecurringPosted(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(recurringPosted = enabled) }
            }
        }

        fun setRecurringPendingConfirm(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(recurringPendingConfirm = enabled) }
            }
        }

        fun setCardClosing(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(cardClosing = enabled) }
            }
        }

        fun setCardClosingDaysBefore(days: Int) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(cardClosingDaysBefore = days) }
            }
        }

        fun setCardPayment(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(cardPayment = enabled) }
            }
        }

        fun setCardPaymentDaysBefore(days: Int) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(cardPaymentDaysBefore = days) }
            }
        }

        fun setCardOverdue(enabled: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateNotificationSettings { it.copy(cardOverdue = enabled) }
            }
        }

        fun sendTestNotification() {
            viewModelScope.launch {
                notificationGateway.showTest()
                _events.send(SettingsEvent.ShowSnackbar(R.string.settings_notifications_test_sent))
            }
        }

        fun suggestedBackupFileName(): String = backupFileName(timeProvider.now())

        fun onExportUriSelected(uri: String) {
            if (!startBusy()) return
            viewModelScope.launch {
                val message =
                    when (val result = exportBackupUseCase(uri)) {
                        is DomainResult.Success -> R.string.backup_export_success
                        is DomainResult.Failure -> result.error.asStringRes()
                    }
                finishBusy(message)
            }
        }

        fun onImportUriSelected(uri: String) {
            if (!startBusy()) return
            viewModelScope.launch {
                when (val result = previewBackupUseCase(uri)) {
                    is DomainResult.Success -> {
                        pendingImportJson = result.value.json
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                pendingImport = result.value.summary,
                                pendingImportDebtsLost = result.value.debtsLost,
                            )
                        }
                    }
                    is DomainResult.Failure -> finishBusy(result.error.asStringRes())
                }
            }
        }

        fun confirmImport() {
            val json = pendingImportJson ?: return
            if (!startBusy()) return
            clearPendingImport()
            viewModelScope.launch {
                val message =
                    when (val result = importBackupUseCase(json)) {
                        is DomainResult.Success -> R.string.backup_import_success
                        is DomainResult.Failure -> result.error.asStringRes()
                    }
                finishBusy(message)
            }
        }

        fun dismissImport() {
            clearPendingImport()
        }

        /** Marks an operation as running; returns false if another one already is (ignores double taps). */
        private fun startBusy(): Boolean {
            var started = false
            _uiState.update {
                started = !it.isBusy
                if (started) it.copy(isBusy = true) else it
            }
            return started
        }

        private suspend fun finishBusy(
            @StringRes messageRes: Int,
        ) {
            _uiState.update { it.copy(isBusy = false) }
            _events.send(SettingsEvent.ShowSnackbar(messageRes))
        }

        private fun clearPendingImport() {
            pendingImportJson = null
            _uiState.update { it.copy(pendingImport = null, pendingImportDebtsLost = 0) }
        }
    }
