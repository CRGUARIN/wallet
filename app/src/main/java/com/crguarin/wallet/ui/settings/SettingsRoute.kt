package com.crguarin.wallet.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.BuildConfig
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.ui.util.AppLanguage
import com.crguarin.wallet.ui.util.safeHiltViewModel

private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun SettingsRoute(
    onCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        SettingsScreen(
            uiState = SettingsUiState(),
            appVersion = BuildConfig.VERSION_NAME,
            onLanguageSelected = {},
            onThemeSelected = {},
            onDynamicColorChanged = {},
            onCategoriesClick = onCategoriesClick,
            onExportClick = {},
            onImportClick = {},
            onConfirmImport = {},
            onDismissImport = {},
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
                is SettingsEvent.ShowSnackbar -> {
                    @Suppress("LocalContextGetResourceValueCall")
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    // A null URI means the user left the picker without choosing a file: nothing to do.
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.onExportUriSelected(it.toString()) }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onImportUriSelected(it.toString()) }
        }

    SettingsScreen(
        uiState = uiState,
        appVersion = BuildConfig.VERSION_NAME,
        onLanguageSelected = { language: AppLanguage -> viewModel.setLanguage(language) },
        onThemeSelected = { themeMode: ThemeMode -> viewModel.setThemeMode(themeMode) },
        onDynamicColorChanged = viewModel::setDynamicColor,
        onCategoriesClick = onCategoriesClick,
        onExportClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) },
        onImportClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
        onConfirmImport = viewModel::confirmImport,
        onDismissImport = viewModel::dismissImport,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
