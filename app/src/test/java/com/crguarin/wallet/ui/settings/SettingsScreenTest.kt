package com.crguarin.wallet.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.AppLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class Calls {
        var language: AppLanguage? = null
        var theme: ThemeMode? = null
        var dynamicColor: Boolean? = null
        var categories = 0
        var export = 0
        var import = 0
        var confirm = 0
        var dismiss = 0
    }

    private fun show(
        state: SettingsUiState = SettingsUiState(),
        calls: Calls = Calls(),
        dynamicColorSupported: Boolean = true,
    ) {
        composeRule.setContent {
            WalletTheme {
                SettingsScreen(
                    uiState = state,
                    appVersion = "1.2.3",
                    onLanguageSelected = { calls.language = it },
                    onThemeSelected = { calls.theme = it },
                    onDynamicColorChanged = { calls.dynamicColor = it },
                    onCategoriesClick = { calls.categories++ },
                    onExportClick = { calls.export++ },
                    onImportClick = { calls.import++ },
                    onConfirmImport = { calls.confirm++ },
                    onDismissImport = { calls.dismiss++ },
                    dynamicColorSupported = dynamicColorSupported,
                )
            }
        }
    }

    private fun option(text: String) = composeRule.onNode(isSelectable() and hasText(text))

    @Test
    fun `sections and version are visible in spanish`() {
        show()

        listOf("Idioma", "Tema", "Datos", "Backup", "Acerca de").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Exportar datos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Importar datos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Categorías").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1.2.3").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `sections are translated to english`() {
        show()

        listOf("Language", "Theme", "Data", "Backup", "About", "Export data", "Import data", "Dark", "Dynamic color").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `selected theme and language options are marked as selected`() {
        show(SettingsUiState(language = AppLanguage.ENGLISH, themeMode = ThemeMode.DARK))

        option("Oscuro").assertIsSelected()
        option("Claro").assertIsNotSelected()
        option("Sistema").assertIsNotSelected()
        option("English").assertIsSelected()
        option("Español").assertIsNotSelected()
    }

    @Test
    fun `choosing options reports language and theme`() {
        val calls = Calls()
        show(calls = calls)

        option("English").performScrollTo().performClick()
        option("Claro").performScrollTo().performClick()

        assertThat(calls.language).isEqualTo(AppLanguage.ENGLISH)
        assertThat(calls.theme).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `dynamic color switch reflects and changes the state`() {
        val calls = Calls()
        show(SettingsUiState(dynamicColor = true), calls)

        composeRule
            .onNode(
                androidx.compose.ui.test
                    .isToggleable(),
            ).performScrollTo()
            .assertIsOn()
            .performClick()

        assertThat(calls.dynamicColor).isFalse()
    }

    @Test
    fun `dynamic color switch is off when disabled in preferences`() {
        show(SettingsUiState(dynamicColor = false))

        composeRule
            .onNode(
                androidx.compose.ui.test
                    .isToggleable(),
            ).performScrollTo()
            .assertIsOff()
    }

    @Test
    fun `dynamic color is hidden when the device does not support it`() {
        show(dynamicColorSupported = false)

        composeRule.onNodeWithText("Color dinámico").assertDoesNotExist()
        composeRule.onNodeWithText("Oscuro").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `categories export and import entries trigger their callbacks`() {
        val calls = Calls()
        show(calls = calls)

        composeRule.onNodeWithText("Categorías").performScrollTo().performClick()
        composeRule.onNodeWithText("Exportar datos").performScrollTo().performClick()
        composeRule.onNodeWithText("Importar datos").performScrollTo().performClick()

        assertThat(calls.categories).isEqualTo(1)
        assertThat(calls.export).isEqualTo(1)
        assertThat(calls.import).isEqualTo(1)
    }

    @Test
    fun `backup entries are disabled and progress is shown while busy`() {
        val calls = Calls()
        show(SettingsUiState(isBusy = true), calls)

        composeRule.onNodeWithContentDescription("Procesando backup").assertIsDisplayed()
        composeRule.onNodeWithText("Exportar datos").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Importar datos").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `no progress is shown when idle`() {
        show()

        composeRule.onNodeWithContentDescription("Procesando backup").assertDoesNotExist()
    }

    @Test
    fun `confirmation dialog shows the summary and the replace warning`() {
        val calls = Calls()
        show(SettingsUiState(pendingImport = BackupSummary(3, 25, 480)), calls)

        composeRule.onNodeWithText("¿Importar datos?").assertIsDisplayed()
        composeRule.onNodeWithText("3 cuentas, 25 categorías, 480 movimientos", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Esto reemplazará todos tus datos actuales", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Importar").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertThat(calls.confirm).isEqualTo(1)
        assertThat(calls.dismiss).isEqualTo(1)
    }

    @Test
    fun `summary uses singular forms`() {
        show(SettingsUiState(pendingImport = BackupSummary(1, 1, 1)))

        composeRule.onNodeWithText("1 cuenta, 1 categoría, 1 movimiento", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `confirmation dialog is translated`() {
        show(SettingsUiState(pendingImport = BackupSummary(2, 4, 6)))

        composeRule.onNodeWithText("2 accounts, 4 categories, 6 transactions", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("This will replace all your current data", substring = true).assertIsDisplayed()
    }

    @Test
    fun `no dialog without a pending import`() {
        show()

        composeRule.onNodeWithText("¿Importar datos?").assertDoesNotExist()
    }
}
