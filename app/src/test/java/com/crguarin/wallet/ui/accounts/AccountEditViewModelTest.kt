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
import com.crguarin.wallet.ui.theme.WalletPalette
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

    @Test
    fun `selecting credit card swaps the default icon`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setType(AccountType.CREDIT_CARD)

            val state = viewModel.uiState.value
            assertThat(state.isCreditCard).isTrue()
            assertThat(state.icon).isEqualTo("credit_card")
        }

    @Test
    fun `credit limit and day fields validate inline`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setType(AccountType.CREDIT_CARD)

            viewModel.setCreditLimitText("")
            assertThat(viewModel.uiState.value.creditLimitErrorRes).isEqualTo(R.string.card_error_limit_required)
            viewModel.setCreditLimitText("0")
            assertThat(viewModel.uiState.value.creditLimitErrorRes).isEqualTo(R.string.card_error_limit_required)
            viewModel.setCreditLimitText("500000")
            assertThat(viewModel.uiState.value.creditLimitErrorRes).isNull()

            viewModel.setStatementDayText("32")
            assertThat(viewModel.uiState.value.statementDayErrorRes).isEqualTo(R.string.card_error_day_invalid)
            viewModel.setStatementDayText("15")
            assertThat(viewModel.uiState.value.statementDayErrorRes).isNull()

            viewModel.setPaymentDueDayText("0")
            assertThat(viewModel.uiState.value.paymentDueDayErrorRes).isEqualTo(R.string.card_error_day_invalid)
            viewModel.setPaymentDueDayText("5")
            assertThat(viewModel.uiState.value.paymentDueDayErrorRes).isNull()
        }

    @Test
    fun `edit mode with transactions locks the account type`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(existingAccount),
                txCounts = mapOf(10L to 2),
            )
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            assertThat(viewModel.uiState.value.hasTransactions).isTrue()
            assertThat(viewModel.uiState.value.isTypeLocked).isTrue()

            viewModel.setType(AccountType.CREDIT_CARD)
            assertThat(viewModel.uiState.value.type).isEqualTo(AccountType.SAVINGS)
        }

    @Test
    fun `edit mode without transactions keeps the type editable`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            assertThat(viewModel.uiState.value.isTypeLocked).isFalse()

            viewModel.setType(AccountType.CREDIT_CARD)
            assertThat(viewModel.uiState.value.type).isEqualTo(AccountType.CREDIT_CARD)
        }

    @Test
    fun `edit mode of a card pre-fills debt as a positive amount`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 11L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money(-250_000L),
                    icon = "credit_card",
                    creditLimit = Money(500_000_00L),
                    statementDay = 15,
                    paymentDueDay = 5,
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 11L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.isCreditCard).isTrue()
            assertThat(state.initialBalanceText).isEqualTo("2500")
            assertThat(state.creditLimitText).isEqualTo("500000")
            assertThat(state.statementDayText).isEqualTo("15")
            assertThat(state.paymentDueDayText).isEqualTo("5")
        }

    @Test
    fun `saving a card with invalid fields reports errors and does not save`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setName("RappiCard")
            viewModel.setType(AccountType.CREDIT_CARD)
            viewModel.setInitialBalanceText("2500")
            viewModel.setCreditLimitText("")
            viewModel.setStatementDayText("40")
            viewModel.setPaymentDueDayText("5")

            viewModel.saveAccount()

            val state = viewModel.uiState.value
            assertThat(state.creditLimitErrorRes).isEqualTo(R.string.card_error_limit_required)
            assertThat(state.statementDayErrorRes).isEqualTo(R.string.card_error_day_invalid)
            assertThat(accountRepository.getAllAccounts()).isEmpty()
        }

    @Test
    fun `saving a card stores negated debt limit and billing days`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setName("RappiCard")
            viewModel.setType(AccountType.CREDIT_CARD)
            viewModel.setInitialBalanceText("2500")
            viewModel.setCreditLimitText("500000")
            viewModel.setStatementDayText("15")
            viewModel.setPaymentDueDayText("5")

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val accounts = accountRepository.getAllAccounts()
                assertThat(accounts).hasSize(1)
                val saved = accounts.first()
                assertThat(saved.type).isEqualTo(AccountType.CREDIT_CARD)
                assertThat(saved.initialBalance).isEqualTo(Money(-250_000L))
                assertThat(saved.creditLimit).isEqualTo(Money(50_000_000L))
                assertThat(saved.statementDay).isEqualTo(15)
                assertThat(saved.paymentDueDay).isEqualTo(5)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `editing a card with credit in favor preserves it when debt stays zero`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 12L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money(30_000L),
                    icon = "credit_card",
                    creditLimit = Money(500_000_00L),
                    statementDay = 15,
                    paymentDueDay = 5,
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 12L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            assertThat(viewModel.uiState.value.initialBalanceText).isEmpty()
            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isFalse()

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)
                assertThat(accountRepository.getAccount(12L)?.initialBalance).isEqualTo(Money(30_000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `editing a card with credit in favor stores negated debt when entered`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 12L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money(30_000L),
                    icon = "credit_card",
                    creditLimit = Money(500_000_00L),
                    statementDay = 15,
                    paymentDueDay = 5,
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 12L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setInitialBalanceText("2500")
            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isTrue()

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)
                assertThat(accountRepository.getAccount(12L)?.initialBalance).isEqualTo(Money(-250_000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `new account starts with the first palette color that no other account uses`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount.copy(color = WalletPalette.colors.first())))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.usedColors).containsExactly(WalletPalette.colors.first())
            assertThat(state.color).isEqualTo(WalletPalette.colors[1])
        }

    @Test
    fun `archived accounts free their color for a new account`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                listOf(existingAccount.copy(color = WalletPalette.colors.first(), isArchived = true)),
            )
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.usedColors).isEmpty()
            assertThat(state.color).isEqualTo(WalletPalette.colors.first())
        }

    @Test
    fun `edit mode does not list the account's own color as used`() =
        runTest(testDispatcher) {
            val other = existingAccount.copy(id = 11L, name = "Nequi", color = WalletPalette.colors[2])
            accountRepository.setInitialData(listOf(existingAccount.copy(color = WalletPalette.colors[1]), other))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 10L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.color).isEqualTo(WalletPalette.colors[1])
            assertThat(state.usedColors).containsExactly(WalletPalette.colors[2])
        }

    @Test
    fun `saving a color used by another account reports the color in use error`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(existingAccount.copy(color = WalletPalette.colors[1])))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Efectivo")
            viewModel.setInitialBalanceText("50000")
            viewModel.setColor(WalletPalette.colors[1])

            viewModel.saveAccount()

            assertThat(viewModel.uiState.value.generalErrorRes).isEqualTo(R.string.error_color_in_use)
            assertThat(accountRepository.getAllAccounts()).hasSize(1)
        }

    @Test
    fun `selecting a transit card swaps the default icon for directions_bus`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setType(AccountType.TRANSIT_CARD)

            val state = viewModel.uiState.value
            assertThat(state.isTransitCard).isTrue()
            assertThat(state.icon).isEqualTo("directions_bus")
        }

    @Test
    fun `transit card needs a fare greater than zero`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Cívica")
            viewModel.setType(AccountType.TRANSIT_CARD)
            viewModel.setInitialBalanceText("10500")

            viewModel.setFareText("0")
            assertThat(viewModel.uiState.value.fareErrorRes).isEqualTo(R.string.error_fare_invalid)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.setFareText("3500")
            assertThat(viewModel.uiState.value.fareErrorRes).isNull()
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()
        }

    @Test
    fun `saving a transit card without a fare reports the error and does not save`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Cívica")
            viewModel.setType(AccountType.TRANSIT_CARD)
            viewModel.setInitialBalanceText("10500")

            viewModel.saveAccount()

            assertThat(viewModel.uiState.value.fareErrorRes).isEqualTo(R.string.error_fare_invalid)
            assertThat(accountRepository.getAllAccounts()).isEmpty()
        }

    @Test
    fun `saving a transit card stores its fare and keeps the card fields empty`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Cívica")
            viewModel.setType(AccountType.TRANSIT_CARD)
            viewModel.setInitialBalanceText("10500")
            viewModel.setFareText("3500")

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val saved = accountRepository.getAllAccounts().single()
                assertThat(saved.type).isEqualTo(AccountType.TRANSIT_CARD)
                assertThat(saved.initialBalance).isEqualTo(Money(1_050_000L))
                assertThat(saved.fare).isEqualTo(Money(350_000L))
                assertThat(saved.creditLimit).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `edit mode loads the fare of a transit card`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 12L,
                    name = "Cívica",
                    type = AccountType.TRANSIT_CARD,
                    initialBalance = Money(1_050_000L),
                    icon = "directions_bus",
                    fare = Money(350_000L),
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 12L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.fareText).isEqualTo("3500")
            assertThat(state.isSaveEnabled).isTrue()
        }

    @Test
    fun `transit card with the negative balance switch on saves the amount negated`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Cívica")
            viewModel.setType(AccountType.TRANSIT_CARD)
            viewModel.setFareText("3500")
            viewModel.setInitialBalanceText("1000")
            viewModel.setTransitBalanceNegative(true)

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val saved = accountRepository.getAllAccounts().single()
                assertThat(saved.initialBalance).isEqualTo(Money(-100_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `negative transit balance larger than the fare shows an error and does not save`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Cívica")
            viewModel.setType(AccountType.TRANSIT_CARD)
            viewModel.setFareText("3500")
            viewModel.setTransitBalanceNegative(true)
            viewModel.setInitialBalanceText("5000")

            assertThat(viewModel.uiState.value.amountErrorRes)
                .isEqualTo(R.string.error_transit_credit_too_large)
            assertThat(viewModel.uiState.value.isSaveEnabled).isFalse()

            viewModel.saveAccount()

            assertThat(viewModel.uiState.value.amountErrorRes)
                .isEqualTo(R.string.error_transit_credit_too_large)
            assertThat(accountRepository.getAllAccounts()).isEmpty()

            viewModel.setInitialBalanceText("3500")
            assertThat(viewModel.uiState.value.amountErrorRes).isNull()
            assertThat(viewModel.uiState.value.isSaveEnabled).isTrue()
        }

    @Test
    fun `negative balance switch is ignored for other account types`() =
        runTest(testDispatcher) {
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to null))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)
            viewModel.setName("Efectivo")
            viewModel.setType(AccountType.CASH)
            viewModel.setInitialBalanceText("1000")
            viewModel.setTransitBalanceNegative(true)

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                assertThat(accountRepository.getAllAccounts().single().initialBalance).isEqualTo(Money(100_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `edit mode turns the negative switch on for a transit card with a negative balance`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 13L,
                    name = "Cívica",
                    type = AccountType.TRANSIT_CARD,
                    initialBalance = Money(-100_000L),
                    icon = "directions_bus",
                    fare = Money(350_000L),
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 13L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            val state = viewModel.uiState.value
            assertThat(state.transitBalanceNegative).isTrue()
            assertThat(state.initialBalanceText).isEqualTo("1000")
            assertThat(state.showInitialBalanceWarning).isFalse()

            viewModel.setTransitBalanceNegative(false)
            assertThat(viewModel.uiState.value.showInitialBalanceWarning).isTrue()
        }

    @Test
    fun `leaving the transit type drops the owed credit trip instead of saving it as positive`() =
        runTest(testDispatcher) {
            val card =
                existingAccount.copy(
                    id = 13L,
                    name = "Cívica",
                    type = AccountType.TRANSIT_CARD,
                    initialBalance = Money(-100_000L),
                    icon = "directions_bus",
                    fare = Money(350_000L),
                )
            accountRepository.setInitialData(listOf(card))
            val savedStateHandle = SavedStateHandle(mapOf("accountId" to 13L))
            val viewModel = AccountEditViewModel(savedStateHandle, accountRepository, saveAccountUseCase)

            viewModel.setType(AccountType.CASH)

            val state = viewModel.uiState.value
            assertThat(state.transitBalanceNegative).isFalse()
            assertThat(state.initialBalanceText).isEmpty()
            assertThat(state.amountErrorRes).isNull()
            assertThat(state.showInitialBalanceWarning).isTrue()

            viewModel.events.test {
                viewModel.saveAccount()
                assertThat(awaitItem()).isEqualTo(AccountEditEvent.NavigateBack)

                val saved = accountRepository.getAccount(13L)
                assertThat(saved?.type).isEqualTo(AccountType.CASH)
                assertThat(saved?.initialBalance).isEqualTo(Money.ZERO)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
