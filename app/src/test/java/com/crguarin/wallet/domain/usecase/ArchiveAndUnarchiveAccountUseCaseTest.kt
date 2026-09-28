package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ArchiveAndUnarchiveAccountUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var archiveAccountUseCase: ArchiveAccountUseCase
    private lateinit var unarchiveAccountUseCase: UnarchiveAccountUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        archiveAccountUseCase = ArchiveAccountUseCase(accountRepository)
        unarchiveAccountUseCase = UnarchiveAccountUseCase(accountRepository)
    }

    @Test
    fun `archive fails when account not found`() =
        runTest {
            val result = archiveAccountUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `archive sets isArchived to true`() =
        runTest {
            val account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    isArchived = false,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(account))

            val result = archiveAccountUseCase(1L)
            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)?.isArchived).isTrue()
        }

    @Test
    fun `unarchive fails when account not found`() =
        runTest {
            val result = unarchiveAccountUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `unarchive fails when name conflicts with active account`() =
        runTest {
            val active =
                Account(
                    id = 1L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "savings",
                    isArchived = false,
                    createdAt = now,
                )
            val archived =
                Account(
                    id = 2L,
                    name = "ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money.ZERO,
                    color = 2,
                    icon = "savings",
                    isArchived = true,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(active, archived))

            val result = unarchiveAccountUseCase(2L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
            assertThat(accountRepository.getAccount(2L)?.isArchived).isTrue()
        }

    @Test
    fun `unarchive sets isArchived to false when valid`() =
        runTest {
            val archived =
                Account(
                    id = 1L,
                    name = "Efectivo Antiguo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    isArchived = true,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(archived))

            val result = unarchiveAccountUseCase(1L)
            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)?.isArchived).isFalse()
        }
}
