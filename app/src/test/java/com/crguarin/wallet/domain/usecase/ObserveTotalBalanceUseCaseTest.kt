package com.crguarin.wallet.domain.usecase

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ObserveTotalBalanceUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var observeTotalBalanceUseCase: ObserveTotalBalanceUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository)
    }

    @Test
    fun `sums only active accounts with includeInTotal true`() =
        runTest {
            val a1 =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(1000L),
                    color = 1,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    createdAt = now,
                )
            val a2 =
                Account(
                    id = 2L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(2000L),
                    color = 2,
                    icon = "savings",
                    includeInTotal = true,
                    isArchived = false,
                    createdAt = now,
                )
            val excluded =
                Account(
                    id = 3L,
                    name = "Inversión",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(5000L),
                    color = 3,
                    icon = "trending_up",
                    includeInTotal = false,
                    isArchived = false,
                    createdAt = now,
                )
            val archived =
                Account(
                    id = 4L,
                    name = "Antigua",
                    type = AccountType.CASH,
                    initialBalance = Money(3000L),
                    color = 4,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = true,
                    createdAt = now,
                )

            accountRepository.setInitialData(
                listOf(a1, a2, excluded, archived),
                balanceMap =
                    mapOf(
                        1L to Money(1500L),
                        2L to Money(2500L),
                        3L to Money(9999L),
                        4L to Money(5000L),
                    ),
            )

            observeTotalBalanceUseCase().test {
                val total = awaitItem()
                // Should only sum 1500 + 2500 = 4000
                assertThat(total).isEqualTo(Money(4000L))
                cancelAndIgnoreRemainingEvents()
            }
        }
}
