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

class ObserveAccountsUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var observeAccountsUseCase: ObserveAccountsUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository)
    }

    @Test
    fun `observes accounts excluding archived by default`() =
        runTest {
            val active =
                Account(
                    id = 1L,
                    name = "Activa",
                    type = AccountType.CASH,
                    initialBalance = Money(100L),
                    color = 1,
                    icon = "wallet",
                    isArchived = false,
                    createdAt = now,
                )
            val archived =
                Account(
                    id = 2L,
                    name = "Archivada",
                    type = AccountType.CASH,
                    initialBalance = Money(50L),
                    color = 2,
                    icon = "wallet",
                    isArchived = true,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(active, archived), balanceMap = mapOf(1L to Money(200L)))

            observeAccountsUseCase().test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().account.id).isEqualTo(1L)
                assertThat(list.first().balance).isEqualTo(Money(200L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observes accounts including archived when requested`() =
        runTest {
            val active =
                Account(
                    id = 1L,
                    name = "Activa",
                    type = AccountType.CASH,
                    initialBalance = Money(100L),
                    color = 1,
                    icon = "wallet",
                    isArchived = false,
                    createdAt = now,
                )
            val archived =
                Account(
                    id = 2L,
                    name = "Archivada",
                    type = AccountType.CASH,
                    initialBalance = Money(50L),
                    color = 2,
                    icon = "wallet",
                    isArchived = true,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(active, archived))

            observeAccountsUseCase(includeArchived = true).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
