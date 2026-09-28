package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategorySeeder
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CompleteOnboardingUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var categorySeeder: FakeCategorySeeder
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var saveAccountUseCase: SaveAccountUseCase
    private lateinit var seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase
    private lateinit var completeOnboardingUseCase: CompleteOnboardingUseCase

    @Before
    fun setUp() {
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
    }

    @Test
    fun `successful onboarding saves account seeds categories and marks onboarding complete`() =
        runTest {
            val account =
                Account(
                    id = 0L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = Money(50_000L),
                    color = -16711936,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 0,
                    createdAt = Instant.EPOCH,
                )

            val result = completeOnboardingUseCase(account)

            assertThat(result).isInstanceOf(DomainResult.Success::class.java)
            assertThat((result as DomainResult.Success).value).isEqualTo(1L)
            assertThat(accountRepository.getAllAccounts()).hasSize(1)
            assertThat(categorySeeder.seedCallCount).isEqualTo(1)
            assertThat(preferencesRepository.getUserPreferences().isOnboardingCompleted).isTrue()
        }

    @Test
    fun `account with initial balance of zero succeeds in onboarding`() =
        runTest {
            val account =
                Account(
                    id = 0L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = Money.ZERO,
                    color = -16711936,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 0,
                    createdAt = Instant.EPOCH,
                )

            val result = completeOnboardingUseCase(account)

            assertThat(result).isInstanceOf(DomainResult.Success::class.java)
            assertThat(preferencesRepository.getUserPreferences().isOnboardingCompleted).isTrue()
        }

    @Test
    fun `empty account name returns failure and does not seed or mark onboarding complete`() =
        runTest {
            val account =
                Account(
                    id = 0L,
                    name = "   ",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = Money.ZERO,
                    color = -16711936,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 0,
                    createdAt = Instant.EPOCH,
                )

            val result = completeOnboardingUseCase(account)

            assertThat(result).isEqualTo(DomainResult.Failure(DomainError.NameEmpty))
            assertThat(accountRepository.getAllAccounts()).isEmpty()
            assertThat(categorySeeder.seedCallCount).isEqualTo(0)
            assertThat(preferencesRepository.getUserPreferences().isOnboardingCompleted).isFalse()
        }

    @Test
    fun `name too long returns failure and does not complete onboarding`() =
        runTest {
            val account =
                Account(
                    id = 0L,
                    name = "A".repeat(41),
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = Money.ZERO,
                    color = -16711936,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 0,
                    createdAt = Instant.EPOCH,
                )

            val result = completeOnboardingUseCase(account)

            assertThat(result).isEqualTo(DomainResult.Failure(DomainError.NameTooLong))
            assertThat(accountRepository.getAllAccounts()).isEmpty()
            assertThat(categorySeeder.seedCallCount).isEqualTo(0)
            assertThat(preferencesRepository.getUserPreferences().isOnboardingCompleted).isFalse()
        }
}
