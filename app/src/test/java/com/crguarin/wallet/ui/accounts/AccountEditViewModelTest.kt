package com.crguarin.wallet.ui.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.usecase.SaveAccountUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class AccountEditViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveAccountUseCase: SaveAccountUseCase

    private val existingAccount =
        Account(
            id = 10L,
            name = "Bancolombia",
            type = AccountType.SAVINGS,
            currencyCode = "COP",
            initialBalance = Money(500_000L),
            color = -16711936,
            icon = "savings",
            includeInTotal = true,
            isArchived = false,
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        timeProvider = FakeTimeProvider(Instant.parse("2026-09-28T12:00:00Z"))
        saveAccountUseCase = SaveAccountUseCase(accountRepository, timeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `creation mode initializes with default values`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isFalse()
            assertThat(state.name).isEmpty()
            assertThat(state.type).isEqualTo(AccountType.CASH)
            assertThat(state.icon).isEqualTo("wallet")
            assertThat(state.includeInTotal).isTrue()
            assertThat(state.isSaveEnabled).isFalse()
        }

    @Test
    fun `edit mode for a missing account is reported as not found`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 999L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.notFound).isTrue()
            assertThat(state.isSaveEnabled).isFalse()
        }

    @Test
    fun `edit mode pre-fills fields from existing account`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.isEditMode).isTrue()
            assertThat(state.name).isEqualTo("Bancolombia")
            assertThat(state.type).isEqualTo(AccountType.SAVINGS)
            assertThat(state.icon).isEqualTo("savings")
            assertThat(state.initialBalanceText).isEqualTo("5000")
            assertThat(state.showInitialBalanceWarning).isFalse()
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `initial balance warning is shown when changed in edit mode`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isFalse()

            viewModel.setInitialBalanceText("6000")
            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isTrue()

            viewModel.setInitialBalanceText("5000")
            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isFalse()
        }

    @Test
    fun `validation detects empty name, long name, and duplicate name`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            // Empty name with spaces
            viewModel.setName("   ")
            assertThat(viewModel.uiState.value.nameErrorRes).isEqualTo(R.string.error_name_empty)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            // Too long name
            viewModel.setName("A".repeat(41))
            assertThat(viewModel.uiState.value.nameErrorRes).isEqualTo(R.string.error_name_too_long)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            // Duplicate active name (case insensitive)
            viewModel.setName("bancolombia")
            assertThat(viewModel.uiState.value.nameErrorRes).isEqualTo(R.string.error_name_duplicated)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            // Valid name
            viewModel.setName("Efectivo")
            assertThat(viewModel.uiState.value.nameErrorRes).isNull()
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()
        }

    @Test
    fun `saveAccount in creation mode saves new account and emits NavigateBack`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setName("Efectivo")
            viewModel.setInitialBalanceText("50000")

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val accounts = accountRepository.getAllAccounts()
                assertThat(accounts).hasSize(1)
                assertThat(accounts.first().name).isEqualTo("Efectivo")
                assertThat(accounts.first().initialBalance).isEqualTo(Money(5_000_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `double tap on save saves the account only once`() {
        val standardDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(standardDispatcher)
        runTest(standardDispatcher) {
            var saveCalls = 0
            val countingRepository =
                object : AccountRepository by accountRepository {
                    override suspend fun saveAccount(account: Account): Long {
                        saveCalls++
                        return accountRepository.saveAccount(account)
                    }
                }
            val useCase = SaveAccountUseCase(countingRepository, timeProvider)
            val viewModel =
                AccountEditViewModel(SavedStateHandle(mapOf("accountId" to null)), countingRepository, useCase)
            advanceUntilIdle()
            viewModel.setName("Efectivo")
            viewModel.setInitialBalanceText("50000")

            viewModel.saveAccount()
            viewModel.saveAccount()
            advanceUntilIdle()

            assertThat(saveCalls).isEqualTo(1)
            assertThat(accountRepository.getAllAccounts()).hasSize(1)
        }
    }

    @Test
    fun `saveAccount in edit mode updates existing account and emits NavigateBack`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setName("Bancolombia Principal")
            viewModel.setInitialBalanceText("8000")

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val updated = accountRepository.getAccount(10L)
                assertThat(updated?.name).isEqualTo("Bancolombia Principal")
                assertThat(updated?.initialBalance).isEqualTo(Money(800_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    @Config(qualifiers = "es")
    fun `edit mode keeps cents parseable with the locale decimal separator`() =
        runTest(testDispatcher) {
            val defaultLocale = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag("es-CO"))
            try {
                accountRepository.setInitialData(listOf(existingAccount.copy(initialBalance = Money(150_050L))))
                val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
                val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

                val state = viewModel.uiState.value
                assertThat(state.initialBalanceText).isEqualTo("1500,50")
                assertThat(state.showInitialBalanceWarning).isFalse()
                assertThat(state.isSaveEnabled).isTrue()
            } finally {
                Locale.setDefault(defaultLocale)
            }
        }
}
