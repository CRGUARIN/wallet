package com.crguarin.wallet.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.ThemeMode
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
}
