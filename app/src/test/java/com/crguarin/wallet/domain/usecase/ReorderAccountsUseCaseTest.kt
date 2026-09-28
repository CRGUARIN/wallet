package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ReorderAccountsUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var reorderAccountsUseCase: ReorderAccountsUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        reorderAccountsUseCase = ReorderAccountsUseCase(accountRepository)
    }

    @Test
    fun `updates sort orders according to list order`() =
        runTest {
            val a1 =
                Account(
                    id = 1L,
                    name = "A1",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    sortOrder = 0,
                    createdAt = now,
                )
            val a2 =
                Account(
                    id = 2L,
                    name = "A2",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    sortOrder = 1,
                    createdAt = now,
                )
            val a3 =
                Account(
                    id = 3L,
                    name = "A3",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    sortOrder = 2,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(a1, a2, a3))

            // Reorder to 3, 1, 2
            val result = reorderAccountsUseCase(listOf(3L, 1L, 2L))
            assertThat(result.isSuccess).isTrue()

            assertThat(accountRepository.getAccount(3L)?.sortOrder).isEqualTo(0)
            assertThat(accountRepository.getAccount(1L)?.sortOrder).isEqualTo(1)
            assertThat(accountRepository.getAccount(2L)?.sortOrder).isEqualTo(2)
        }
}
