package com.crguarin.wallet.ui.settings

import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeAppLanguageController
import com.crguarin.wallet.domain.fake.FakeBackupDocumentStore
import com.crguarin.wallet.domain.fake.FakeBackupRepository
import com.crguarin.wallet.domain.fake.FakeDailyWorkScheduler
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeNotificationGateway
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.testProcessDueRecurringUseCase
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.usecase.ExportBackupUseCase
import com.crguarin.wallet.domain.usecase.ImportBackupUseCase
import com.crguarin.wallet.domain.usecase.PreviewBackupUseCase
import com.crguarin.wallet.ui.util.AppLanguage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val originalTimeZone = TimeZone.getDefault()

    private lateinit var preferences: FakePreferencesRepository
    private lateinit var language: FakeAppLanguageController
    private lateinit var backupRepository: FakeBackupRepository
    private lateinit var store: FakeBackupDocumentStore
    private lateinit var accounts: FakeAccountRepository
    private lateinit var scheduler: FakeDailyWorkScheduler
    private lateinit var gateway: FakeNotificationGateway
    private lateinit var viewModel: SettingsViewModel

    private val summary = BackupSummary(accounts = 3, categories = 25, transactions = 480)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        preferences = FakePreferencesRepository(UserPreferences(themeMode = ThemeMode.LIGHT, dynamicColor = false))
        language = FakeAppLanguageController(AppLanguage.SPANISH)
        backupRepository = FakeBackupRepository()
        store = FakeBackupDocumentStore()
        accounts = FakeAccountRepository()
        scheduler = FakeDailyWorkScheduler()
        gateway = FakeNotificationGateway()
        viewModel =
            SettingsViewModel(
                preferencesRepository = preferences,
                languageController = language,
                exportBackupUseCase = ExportBackupUseCase(backupRepository, store),
                previewBackupUseCase = PreviewBackupUseCase(backupRepository, store, FakeDebtRepository()),
                importBackupUseCase = ImportBackupUseCase(backupRepository, testProcessDueRecurringUseCase()),
                timeProvider = FakeTimeProvider(Instant.parse("2026-09-28T15:30:00Z")),
                accountRepository = accounts,
                dailyWorkScheduler = scheduler,
                notificationGateway = gateway,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `initial state reflects preferences and current language`() {
        val state = viewModel.uiState.value

        assertThat(state.language).isEqualTo(AppLanguage.SPANISH)
        assertThat(state.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(state.dynamicColor).isFalse()
        assertThat(state.isBusy).isFalse()
        assertThat(state.pendingImport).isNull()
    }

    @Test
    fun `changing the theme updates the repository and the state`() =
        runTest {
            viewModel.setThemeMode(ThemeMode.DARK)

            assertThat(preferences.getUserPreferences().themeMode).isEqualTo(ThemeMode.DARK)
            assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.DARK)
        }

    @Test
    fun `changing dynamic color updates the repository and the state`() =
        runTest {
            viewModel.setDynamicColor(true)

            assertThat(preferences.getUserPreferences().dynamicColor).isTrue()
            assertThat(viewModel.uiState.value.dynamicColor).isTrue()
        }

    @Test
    fun `changing the language goes through the controller and updates the state`() {
        viewModel.setLanguage(AppLanguage.ENGLISH)

        assertThat(language.setCalls).containsExactly(AppLanguage.ENGLISH)
        assertThat(viewModel.uiState.value.language).isEqualTo(AppLanguage.ENGLISH)
    }

    @Test
    fun `suggested file name uses the current local time`() {
        assertThat(viewModel.suggestedBackupFileName()).isEqualTo("wallet-backup-20260928-1530.json")
    }

    @Test
    fun `export writes to the chosen uri and shows a success snackbar`() =
        runTest {
            backupRepository.exportResult = DomainResult.Success("""{"schemaVersion":1}""")

            viewModel.events.test {
                viewModel.onExportUriSelected("content://out/1")

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.backup_export_success))
            }
            assertThat(store.writes).containsExactly("content://out/1" to """{"schemaVersion":1}""")
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `export failure shows the error message`() =
        runTest {
            store.writeResult = DomainResult.Failure(DomainError.BackupFailed)

            viewModel.events.test {
                viewModel.onExportUriSelected("content://out/1")

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.error_backup_failed))
            }
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `progress is active while exporting and a second tap is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            backupRepository.onCall = { gate.await() }

            viewModel.onExportUriSelected("content://out/1")
            assertThat(viewModel.uiState.value.isBusy).isTrue()

            viewModel.onExportUriSelected("content://out/2")
            gate.complete(Unit)

            assertThat(backupRepository.exportCalls).isEqualTo(1)
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `selecting a valid file shows the confirmation with the summary and imports nothing yet`() =
        runTest {
            store.readResult = DomainResult.Success("""{"file":true}""")
            backupRepository.parseResult = DomainResult.Success(summary)

            viewModel.onImportUriSelected("content://in/1")

            assertThat(viewModel.uiState.value.pendingImport).isEqualTo(summary)
            assertThat(viewModel.uiState.value.isBusy).isFalse()
            assertThat(backupRepository.importedJson).isEmpty()
        }

    @Test
    fun `confirming imports the validated content and reports success`() =
        runTest {
            store.readResult = DomainResult.Success("""{"file":true}""")
            backupRepository.parseResult = DomainResult.Success(summary)
            backupRepository.importResult = DomainResult.Success(summary)
            viewModel.onImportUriSelected("content://in/1")

            viewModel.events.test {
                viewModel.confirmImport()

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.backup_import_success))
            }
            assertThat(backupRepository.importedJson).containsExactly("""{"file":true}""")
            assertThat(viewModel.uiState.value.pendingImport).isNull()
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `cancelling the confirmation discards the file and imports nothing`() =
        runTest {
            backupRepository.parseResult = DomainResult.Success(summary)
            viewModel.onImportUriSelected("content://in/1")

            viewModel.dismissImport()
            viewModel.confirmImport()

            assertThat(viewModel.uiState.value.pendingImport).isNull()
            assertThat(backupRepository.importedJson).isEmpty()
        }

    @Test
    fun `invalid file shows an error and no confirmation dialog`() =
        runTest {
            backupRepository.parseResult = DomainResult.Failure(DomainError.BackupInvalid("bad currency"))

            viewModel.events.test {
                viewModel.onImportUriSelected("content://in/1")

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.error_backup_invalid))
            }
            assertThat(viewModel.uiState.value.pendingImport).isNull()
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `unsupported version shows its own message`() =
        runTest {
            backupRepository.parseResult = DomainResult.Failure(DomainError.BackupVersionUnsupported)

            viewModel.events.test {
                viewModel.onImportUriSelected("content://in/1")

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.error_backup_version_unsupported))
            }
        }

    @Test
    fun `unreadable file shows an error and no confirmation`() =
        runTest {
            store.readResult = DomainResult.Failure(DomainError.BackupFailed)

            viewModel.events.test {
                viewModel.onImportUriSelected("content://in/1")

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.error_backup_failed))
            }
            assertThat(viewModel.uiState.value.pendingImport).isNull()
        }

    @Test
    fun `import failure after confirmation shows the error`() =
        runTest {
            backupRepository.parseResult = DomainResult.Success(summary)
            backupRepository.importResult = DomainResult.Failure(DomainError.BackupFailed)
            viewModel.onImportUriSelected("content://in/1")

            viewModel.events.test {
                viewModel.confirmImport()

                assertThat(awaitItem()).isEqualTo(SettingsEvent.ShowSnackbar(R.string.error_backup_failed))
            }
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `progress is active while importing and confirming twice imports once`() =
        runTest {
            backupRepository.parseResult = DomainResult.Success(summary)
            viewModel.onImportUriSelected("content://in/1")
            val gate = CompletableDeferred<Unit>()
            backupRepository.onCall = { gate.await() }

            viewModel.confirmImport()
            assertThat(viewModel.uiState.value.isBusy).isTrue()
            viewModel.confirmImport()
            gate.complete(Unit)

            assertThat(backupRepository.importedJson).hasSize(1)
            assertThat(viewModel.uiState.value.isBusy).isFalse()
        }

    @Test
    fun `picking another file while one is being read is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            backupRepository.onCall = { gate.await() }
            backupRepository.parseResult = DomainResult.Success(summary)

            viewModel.onImportUriSelected("content://in/1")
            assertThat(viewModel.uiState.value.isBusy).isTrue()
            viewModel.onImportUriSelected("content://in/2")
            gate.complete(Unit)

            assertThat(store.readUris).containsExactly("content://in/1")
        }

    @Test
    fun `notification settings start with defaults and follow the repository`() =
        runTest {
            assertThat(viewModel.uiState.value.notificationSettings.enabled).isTrue()
            assertThat(viewModel.uiState.value.hasCreditCard).isFalse()

            preferences.updateNotificationSettings { it.copy(enabled = false) }

            assertThat(viewModel.uiState.value.notificationSettings.enabled).isFalse()
        }

    @Test
    fun `disabling notifications persists the setting`() =
        runTest {
            viewModel.setNotificationsEnabled(false)

            assertThat(preferences.getNotificationSettings().enabled).isFalse()
            assertThat(viewModel.uiState.value.notificationSettings.enabled).isFalse()
        }

    @Test
    fun `changing the notification time persists and reschedules with replace`() =
        runTest {
            viewModel.setNotificationTime(21, 30)

            assertThat(preferences.getNotificationSettings().hour).isEqualTo(21)
            assertThat(preferences.getNotificationSettings().minute).isEqualTo(30)
            assertThat(scheduler.calls).containsExactly(
                FakeDailyWorkScheduler.ScheduleCall(hour = 21, minute = 30, replace = true),
            )
        }

    @Test
    fun `every notification switch persists`() =
        runTest {
            viewModel.setRecurringReminders(false)
            viewModel.setRecurringPosted(false)
            viewModel.setRecurringPendingConfirm(false)
            viewModel.setCardClosing(false)
            viewModel.setCardClosingDaysBefore(5)
            viewModel.setCardPayment(false)
            viewModel.setCardPaymentDaysBefore(1)
            viewModel.setCardOverdue(false)

            val settings = preferences.getNotificationSettings()
            assertThat(settings.recurringReminders).isFalse()
            assertThat(settings.recurringPosted).isFalse()
            assertThat(settings.recurringPendingConfirm).isFalse()
            assertThat(settings.cardClosing).isFalse()
            assertThat(settings.cardClosingDaysBefore).isEqualTo(5)
            assertThat(settings.cardPayment).isFalse()
            assertThat(settings.cardPaymentDaysBefore).isEqualTo(1)
            assertThat(settings.cardOverdue).isFalse()
        }

    @Test
    fun `test notification shows the gateway test and a snackbar`() =
        runTest {
            viewModel.events.test {
                viewModel.sendTestNotification()

                assertThat(awaitItem()).isEqualTo(
                    SettingsEvent.ShowSnackbar(R.string.settings_notifications_test_sent),
                )
            }
            assertThat(gateway.testShown).isTrue()
        }

    @Test
    fun `credit card accounts enable the card notification section`() =
        runTest {
            accounts.setInitialData(
                accountList =
                    listOf(
                        Account(
                            id = 1L,
                            name = "Visa",
                            type = AccountType.CREDIT_CARD,
                            initialBalance = Money.ZERO,
                            color = 1,
                            icon = "card",
                            createdAt = Instant.parse("2026-09-28T15:30:00Z"),
                        ),
                    ),
            )

            assertThat(viewModel.uiState.value.hasCreditCard).isTrue()
        }
}
