package com.crguarin.wallet.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crguarin.wallet.R
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
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
                        _uiState.update { it.copy(isBusy = false, pendingImport = result.value.summary) }
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
            _uiState.update { it.copy(pendingImport = null) }
        }
    }
