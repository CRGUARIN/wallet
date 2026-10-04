package com.crguarin.wallet.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferencesRepositoryImplTest {
    private lateinit var repository: PreferencesRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.filesDir, "datastore/test_prefs_${System.currentTimeMillis()}.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { testFile },
            )
        repository = PreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `default values are returned initially`() =
        testScope.runTest {
            val prefs = repository.getUserPreferences()
            assertThat(prefs.themeMode).isEqualTo(ThemeMode.SYSTEM)
            assertThat(prefs.dynamicColor).isTrue()
            assertThat(prefs.isOnboardingCompleted).isFalse()
            assertThat(prefs.lastUsedAccountId).isNull()
        }

    @Test
    fun `preferences updates are saved and observed correctly`() =
        testScope.runTest {
            repository.observeUserPreferences().test {
                val initial = awaitItem()
                assertThat(initial.themeMode).isEqualTo(ThemeMode.SYSTEM)

                repository.setThemeMode(ThemeMode.DARK)
                val darkState = awaitItem()
                assertThat(darkState.themeMode).isEqualTo(ThemeMode.DARK)

                repository.setDynamicColor(false)
                val noDynamic = awaitItem()
                assertThat(noDynamic.dynamicColor).isFalse()

                repository.setOnboardingCompleted(true)
                val onboarded = awaitItem()
                assertThat(onboarded.isOnboardingCompleted).isTrue()

                repository.setLastUsedAccountId(42L)
                val withAccount = awaitItem()
                assertThat(withAccount.lastUsedAccountId).isEqualTo(42L)

                repository.setLastUsedAccountId(null)
                val withoutAccount = awaitItem()
                assertThat(withoutAccount.lastUsedAccountId).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `notification settings default to the C3 contract values`() =
        testScope.runTest {
            assertThat(repository.getNotificationSettings()).isEqualTo(NotificationSettings())
        }

    @Test
    fun `notification settings updates are saved and observed`() =
        testScope.runTest {
            repository.observeNotificationSettings().test {
                assertThat(awaitItem()).isEqualTo(NotificationSettings())

                repository.updateNotificationSettings { it.copy(enabled = false, hour = 21, minute = 30) }
                val updated = awaitItem()
                assertThat(updated.enabled).isFalse()
                assertThat(updated.hour).isEqualTo(21)
                assertThat(updated.minute).isEqualTo(30)
                assertThat(updated.recurringReminders).isTrue()

                repository.updateNotificationSettings {
                    it.copy(cardClosingDaysBefore = 5, cardPaymentDaysBefore = 0, cardOverdue = false)
                }
                val cards = awaitItem()
                assertThat(cards.cardClosingDaysBefore).isEqualTo(5)
                assertThat(cards.cardPaymentDaysBefore).isEqualTo(0)
                assertThat(cards.cardOverdue).isFalse()
                assertThat(cards.hour).isEqualTo(21)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `notification settings out-of-range values are clamped`() =
        testScope.runTest {
            repository.updateNotificationSettings {
                it.copy(hour = 99, minute = -4, cardClosingDaysBefore = 30, cardPaymentDaysBefore = -2)
            }

            val stored = repository.getNotificationSettings()
            assertThat(stored.hour).isEqualTo(23)
            assertThat(stored.minute).isEqualTo(0)
            assertThat(stored.cardClosingDaysBefore).isEqualTo(7)
            assertThat(stored.cardPaymentDaysBefore).isEqualTo(0)
        }

    @Test
    fun `last notification run date round-trips as ISO date`() =
        testScope.runTest {
            assertThat(repository.getLastNotificationRunDate()).isNull()

            val today = LocalDate.of(2026, 10, 4)
            repository.setLastNotificationRunDate(today)

            assertThat(repository.getLastNotificationRunDate()).isEqualTo(today)
        }
}
