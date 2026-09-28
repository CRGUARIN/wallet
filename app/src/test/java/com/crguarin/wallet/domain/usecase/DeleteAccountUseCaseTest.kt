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

class DeleteAccountUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        deleteAccountUseCase = DeleteAccountUseCase(accountRepository)
    }

    @Test
    fun `fails when account not found`() =
        runTest {
            val result = deleteAccountUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `fails when account has transactions`() =
        runTest {
            val account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(account), txCounts = mapOf(1L to 3))

            val result = deleteAccountUseCase(1L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.AccountHasTransactions)
            assertThat(accountRepository.getAccount(1L)).isNotNull()
        }

    @Test
    fun `deletes account successfully when it has no transactions`() =
        runTest {
            val account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(account), txCounts = mapOf(1L to 0))

            val result = deleteAccountUseCase(1L)
            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)).isNull()
        }
}
