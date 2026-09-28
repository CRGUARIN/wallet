package com.crguarin.wallet.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategorySeeder
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.usecase.CompleteOnboardingUseCase
import com.crguarin.wallet.domain.usecase.SaveAccountUseCase
import com.crguarin.wallet.domain.usecase.SeedDefaultCategoriesUseCase
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
class OnboardingViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var categorySeeder: FakeCategorySeeder
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var saveAccountUseCase: SaveAccountUseCase
    private lateinit var seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase
    private lateinit var completeOnboardingUseCase: CompleteOnboardingUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        timeProvider = FakeTimeProvider(Instant.parse("2026-09-28T12:00:00Z"))
        categorySeeder = FakeCategorySeeder()
        preferencesRepository = FakePreferencesRepository()
        saveAccountUseCase = SaveAccountUseCase(accountRepository, timeProvider)
        seedDefaultCategoriesUseCase = SeedDefaultCategoriesUseCase(categorySeeder)
        completeOnboardingUseCase =
            CompleteOnboardingUseCase(
                saveAccountUseCase = saveAccountUseCase,
                seedDefaultCategoriesUseCase = seedDefaultCategoriesUseCase,
                preferencesRepository = preferencesRepository,
            )
        savedStateHandle = SavedStateHandle()
        viewModel = OnboardingViewModel(completeOnboardingUseCase, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is step 1 with default values`() {
        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(1)
        assertThat(state.accountName).isEmpty()
        assertThat(state.accountType).isEqualTo(AccountType.CASH)
        assertThat(state.selectedIcon).isEqualTo("wallet")
        assertThat(state.includeInTotal).isTrue()
        assertThat(state.isAccountValid).isFalse()
    }

    @Test
    fun `navigation between steps updates state`() {
        viewModel.goToStep(2)
        assertThat(viewModel.uiState.value.step).isEqualTo(2)

        viewModel.goToStep(1)
        assertThat(viewModel.uiState.value.step).isEqualTo(1)
    }

    @Test
    fun `suggestDefaultNameIfEmpty sets name only when currently empty`() {
        viewModel.suggestDefaultNameIfEmpty("Efectivo")
        assertThat(viewModel.uiState.value.accountName).isEqualTo("Efectivo")

        viewModel.setAccountName("Mi cuenta")
        viewModel.suggestDefaultNameIfEmpty("Efectivo")
        assertThat(viewModel.uiState.value.accountName).isEqualTo("Mi cuenta")
    }

    @Test
    fun `account type change automatically updates default icon`() {
        viewModel.setAccountType(AccountType.SAVINGS)
        assertThat(viewModel.uiState.value.selectedIcon).isEqualTo("savings")

        viewModel.setAccountType(AccountType.CASH)
        assertThat(viewModel.uiState.value.selectedIcon).isEqualTo("wallet")
    }

    @Test
    fun `form validation checks name length and amount format`() {
        viewModel.setAccountName("A".repeat(45))
        assertThat(viewModel.uiState.value.nameErrorRes).isNotNull()
        assertThat(viewModel.uiState.value.isAccountValid).isFalse()

        viewModel.setAccountName("Efectivo")
        assertThat(viewModel.uiState.value.nameErrorRes).isNull()
        assertThat(viewModel.uiState.value.isAccountValid).isTrue()

        viewModel.setInitialBalanceText("invalid")
        assertThat(viewModel.uiState.value.amountErrorRes).isNotNull()
        assertThat(viewModel.uiState.value.isAccountValid).isFalse()

        viewModel.setInitialBalanceText("15000")
        assertThat(viewModel.uiState.value.amountErrorRes).isNull()
        assertThat(viewModel.uiState.value.isAccountValid).isTrue()
    }

    @Test
    fun `completeOnboarding creates account seeds categories and emits NavigateToHome`() =
        runTest(testDispatcher) {
            viewModel.setAccountName("Efectivo")
            viewModel.setInitialBalanceText("50000")

            viewModel.events.test {
                viewModel.completeOnboarding()
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToHome)

                val accounts = accountRepository.getAllAccounts()
                assertThat(accounts).hasSize(1)
                assertThat(accounts.first().name).isEqualTo("Efectivo")
                assertThat(accounts.first().initialBalance).isEqualTo(Money(5_000_000L))
                assertThat(categorySeeder.seedCallCount).isEqualTo(1)
                assertThat(preferencesRepository.getUserPreferences().isOnboardingCompleted).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }
}
