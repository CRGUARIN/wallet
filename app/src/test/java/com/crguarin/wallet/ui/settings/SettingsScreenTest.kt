package com.crguarin.wallet.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.ui.components.NotificationPermissionState
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
        var recurring = 0
        var notificationsEnabled: Boolean? = null
        var time: Pair<Int, Int>? = null
        var reminders: Boolean? = null
        var posted: Boolean? = null
        var pending: Boolean? = null
        var closing: Boolean? = null
        var closingDays: Int? = null
        var payment: Boolean? = null
        var paymentDays: Int? = null
        var overdue: Boolean? = null
        var test = 0
        var permissionRequests = 0
    }

    private fun show(
        state: SettingsUiState = SettingsUiState(),
        calls: Calls = Calls(),
        dynamicColorSupported: Boolean = true,
        permissionGranted: Boolean = true,
    ): NotificationPermissionState {
        val permission = NotificationPermissionState(permissionGranted)
        permission.requester = { calls.permissionRequests++ }
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
                    onRecurringClick = { calls.recurring++ },
                    onNotificationsEnabledChanged = { calls.notificationsEnabled = it },
                    onNotificationTimeChanged = { hour, minute -> calls.time = hour to minute },
                    onRecurringRemindersChanged = { calls.reminders = it },
                    onRecurringPostedChanged = { calls.posted = it },
                    onRecurringPendingChanged = { calls.pending = it },
                    onCardClosingChanged = { calls.closing = it },
                    onCardClosingDaysChanged = { calls.closingDays = it },
                    onCardPaymentChanged = { calls.payment = it },
                    onCardPaymentDaysChanged = { calls.paymentDays = it },
                    onCardOverdueChanged = { calls.overdue = it },
                    onSendTestNotification = { calls.test++ },
                    permissionState = permission,
                )
            }
        }
        return permission
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
            .onNode(isToggleable() and hasText("Color dinámico"))
            .performScrollTo()
            .assertIsOn()
            .performClick()

        assertThat(calls.dynamicColor).isFalse()
    }

    @Test
    fun `dynamic color switch is off when disabled in preferences`() {
        show(SettingsUiState(dynamicColor = false))

        composeRule
            .onNode(isToggleable() and hasText("Color dinámico"))
            .performScrollTo()
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
        composeRule.onNodeWithText("Se reemplazarán todos tus datos actuales", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Importar").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertThat(calls.confirm).isEqualTo(1)
        assertThat(calls.dismiss).isEqualTo(1)
    }

    @Test
    fun `confirmation dialog warns about the debts a backup without debts deletes`() {
        show(SettingsUiState(pendingImport = BackupSummary(3, 25, 480), pendingImportDebtsLost = 2))

        composeRule
            .onNodeWithText("El backup no incluye deudas: se borrarán 2 deudas registradas.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog has no debts warning when nothing is lost`() {
        show(SettingsUiState(pendingImport = BackupSummary(3, 25, 480)))

        composeRule.onNodeWithText("El backup no incluye deudas", substring = true).assertDoesNotExist()
    }

    @Test
    fun `summary uses singular forms`() {
        show(SettingsUiState(pendingImport = BackupSummary(1, 1, 1)))

        composeRule.onNodeWithText("1 cuenta, 1 categoría, 1 movimiento", substring = true).assertIsDisplayed()
    }

    @Test
    fun `summary counts card statements when the backup has them`() {
        show(SettingsUiState(pendingImport = BackupSummary(3, 21, 11, cardStatements = 2)))

        composeRule
            .onNodeWithText("3 cuentas, 21 categorías, 11 movimientos, 2 extractos", substring = true)
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `confirmation dialog is translated`() {
        show(SettingsUiState(pendingImport = BackupSummary(2, 4, 6)))

        composeRule.onNodeWithText("2 accounts, 4 categories, 6 transactions", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("This replaces all your current data", substring = true).assertIsDisplayed()
    }

    @Test
    fun `no dialog without a pending import`() {
        show()

        composeRule.onNodeWithText("¿Importar datos?").assertDoesNotExist()
    }

    @Test
    fun `recurring entry opens the recurring list`() {
        val calls = Calls()
        show(calls = calls)

        composeRule
            .onAllNodesWithText("Pagos recurrentes")
            .onFirst()
            .performScrollTo()
            .performClick()

        // The first clickable "Pagos recurrentes" is the Datos entry (the section
        // header with the same text is not clickable).
        assertThat(calls.recurring).isEqualTo(1)
    }

    @Test
    fun `notifications section shows master time recurring switches and test button`() {
        show(SettingsUiState(notificationSettings = NotificationSettings(), hasCreditCard = false))

        composeRule.onNodeWithText("Notificaciones").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Activar notificaciones").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hora de los avisos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Recordatorios antes del cobro").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Avisar al registrar un cobro").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Avisar si hay cobros por confirmar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Enviar notificación de prueba").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `toggling the switches reports every change`() {
        val calls = Calls()
        show(SettingsUiState(notificationSettings = NotificationSettings()), calls)

        toggle("Activar notificaciones")
        toggle("Recordatorios antes del cobro")
        toggle("Avisar al registrar un cobro")
        toggle("Avisar si hay cobros por confirmar")

        assertThat(calls.notificationsEnabled).isFalse()
        assertThat(calls.reminders).isFalse()
        assertThat(calls.posted).isFalse()
        assertThat(calls.pending).isFalse()
    }

    @Test
    fun `enabling without permission requests it first`() {
        val calls = Calls()
        show(
            SettingsUiState(notificationSettings = NotificationSettings(enabled = false)),
            calls,
            permissionGranted = false,
        )

        toggle("Activar notificaciones")

        assertThat(calls.permissionRequests).isEqualTo(1)
        assertThat(calls.notificationsEnabled).isTrue()
    }

    @Test
    fun `blocked banner is shown without permission and opens system settings`() {
        val calls = Calls()
        show(
            SettingsUiState(notificationSettings = NotificationSettings(enabled = true)),
            calls,
            permissionGranted = false,
        )

        composeRule
            .onNodeWithText("Notificaciones bloqueadas para Wallet.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Abrir ajustes del sistema")
            .performScrollTo()
            .performClick()

        assertThat(calls.permissionRequests).isEqualTo(1)
    }

    @Test
    fun `no blocked banner when permission is granted`() {
        show(
            SettingsUiState(notificationSettings = NotificationSettings(enabled = true)),
            permissionGranted = true,
        )

        composeRule.onNodeWithText("Notificaciones bloqueadas para Wallet.").assertDoesNotExist()
    }

    @Test
    fun `controls are disabled when the master switch is off`() {
        show(SettingsUiState(notificationSettings = NotificationSettings(enabled = false)))

        composeRule
            .onNode(isToggleable() and hasText("Recordatorios antes del cobro"))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule
            .onNode(isToggleable() and hasText("Avisar al registrar un cobro"))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule
            .onNode(isToggleable() and hasText("Avisar si hay cobros por confirmar"))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Hora de los avisos").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Enviar notificación de prueba").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `card section is hidden when there is no credit card`() {
        show(SettingsUiState(notificationSettings = NotificationSettings(), hasCreditCard = false))

        composeRule.onNodeWithText("Tarjetas de crédito").assertDoesNotExist()
        composeRule.onNodeWithText("Fecha de corte").assertDoesNotExist()
    }

    @Test
    fun `card section shows the card controls when a credit card exists`() {
        show(SettingsUiState(notificationSettings = NotificationSettings(), hasCreditCard = true))

        composeRule.onNodeWithText("Tarjetas de crédito").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fecha de corte").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fecha límite de pago").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Avisar si el pago está vencido").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card switches and lead selectors report their changes`() {
        val calls = Calls()
        show(SettingsUiState(notificationSettings = NotificationSettings(), hasCreditCard = true), calls)

        toggle("Fecha de corte")
        toggle("Fecha límite de pago")
        toggle("Avisar si el pago está vencido")

        assertThat(calls.closing).isFalse()
        assertThat(calls.payment).isFalse()
        assertThat(calls.overdue).isFalse()

        composeRule.onNodeWithText("El mismo día").performScrollTo().performClick()
        composeRule.onNodeWithText("5 días antes").performClick()

        assertThat(calls.closingDays).isEqualTo(5)
    }

    @Test
    fun `time row opens a picker and confirming reports the time`() {
        val calls = Calls()
        show(SettingsUiState(notificationSettings = NotificationSettings(hour = 8, minute = 0)), calls)

        composeRule.onNodeWithText("Hora de los avisos").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar").performClick()

        assertThat(calls.time).isEqualTo(8 to 0)
    }

    @Test
    fun `test button requests permission and sends the notification once granted`() {
        val calls = Calls()
        val permission =
            show(SettingsUiState(notificationSettings = NotificationSettings()), calls, permissionGranted = false)

        composeRule.onNodeWithText("Enviar notificación de prueba").performScrollTo().performClick()

        // While blocked nothing is sent: it waits for the permission result.
        assertThat(calls.permissionRequests).isEqualTo(1)
        assertThat(calls.test).isEqualTo(0)

        permission.update(true)
        composeRule.waitForIdle()

        assertThat(calls.test).isEqualTo(1)
    }

    @Test
    fun `test button sends the notification right away when permission is granted`() {
        val calls = Calls()
        show(SettingsUiState(notificationSettings = NotificationSettings()), calls, permissionGranted = true)

        composeRule.onNodeWithText("Enviar notificación de prueba").performScrollTo().performClick()

        assertThat(calls.permissionRequests).isEqualTo(0)
        assertThat(calls.test).isEqualTo(1)
    }

    @Test
    fun `summary counts recurring payments when the backup has them`() {
        show(SettingsUiState(pendingImport = BackupSummary(3, 25, 480, recurringPayments = 2)))

        composeRule
            .onNodeWithText("3 cuentas, 25 categorías, 480 movimientos, 2 pagos recurrentes", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `summary counts statements and recurring payments together`() {
        show(SettingsUiState(pendingImport = BackupSummary(3, 21, 11, cardStatements = 2, recurringPayments = 1)))

        composeRule
            .onNodeWithText(
                "3 cuentas, 21 categorías, 11 movimientos, 2 extractos, 1 pago recurrente",
                substring = true,
            ).assertIsDisplayed()
    }

    private fun toggle(text: String) {
        composeRule
            .onNode(isToggleable() and hasText(text))
            .performScrollTo()
            .performClick()
    }
}
