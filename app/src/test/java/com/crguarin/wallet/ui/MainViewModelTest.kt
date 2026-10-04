package com.crguarin.wallet.ui

import app.cash.turbine.test
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeDailyWorkScheduler
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.ThemeMode
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.notifications.OnAppStartUseCase
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakePreferencesRepository()
        val accountRepository = FakeAccountRepository()
        val transactionRepository = FakeTransactionRepository()
        val categoryRepository = FakeCategoryRepository()
        val timeProvider = FakeTimeProvider(Instant.EPOCH)
        val checkNegativeBalanceUseCase = CheckNegativeBalanceUseCase(accountRepository, transactionRepository)
        viewModel =
            MainViewModel(
                preferencesRepository,
                OnAppStartUseCase(
                    ProcessDueRecurringUseCase(
                        FakeRecurringRepository(),
                        accountRepository,
                        categoryRepository,
                        SaveTransactionUseCase(
                            transactionRepository,
                            accountRepository,
                            categoryRepository,
                            timeProvider,
                            checkNegativeBalanceUseCase,
                        ),
                        timeProvider,
                    ),
                    preferencesRepository,
                    FakeDailyWorkScheduler(),
                ),
            )
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
}
