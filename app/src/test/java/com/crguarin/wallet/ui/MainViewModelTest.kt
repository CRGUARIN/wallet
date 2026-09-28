package com.crguarin.wallet.ui

import app.cash.turbine.test
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.model.ThemeMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakePreferencesRepository()
        viewModel = MainViewModel(preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits Ready with onboarding completed flag and preferences`() =
        runTest(testDispatcher) {
            viewModel.uiState.test {
                val item = awaitItem()
                assertThat(item).isInstanceOf(MainUiState.Ready::class.java)
                val ready = item as MainUiState.Ready
                assertThat(ready.onboardingCompleted).isFalse()
                assertThat(ready.preferences.themeMode).isEqualTo(ThemeMode.SYSTEM)

                preferencesRepository.setOnboardingCompleted(true)
                val updated = awaitItem() as MainUiState.Ready
                assertThat(updated.onboardingCompleted).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `userPreferences flow emits updated preferences`() =
        runTest(testDispatcher) {
            viewModel.userPreferences.test {
                val initial = awaitItem()
                assertThat(initial.themeMode).isEqualTo(ThemeMode.SYSTEM)

                preferencesRepository.setThemeMode(ThemeMode.DARK)
                val updated = awaitItem()
                assertThat(updated.themeMode).isEqualTo(ThemeMode.DARK)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
