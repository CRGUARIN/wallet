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

class SaveAccountUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var saveAccountUseCase: SaveAccountUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        saveAccountUseCase = SaveAccountUseCase(accountRepository)
    }

    @Test
    fun `fails when name is empty or blank`() =
        runTest {
            val emptyAccount =
                Account(
                    name = "   ",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(emptyAccount)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameEmpty)
        }

    @Test
    fun `fails when name is longer than 40 chars`() =
        runTest {
            val longNameAccount =
                Account(
                    name = "A".repeat(41),
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(longNameAccount)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameTooLong)
        }

    @Test
    fun `fails when initial balance is negative`() =
        runTest {
            val negativeBalanceAccount =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(-100L),
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(negativeBalanceAccount)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `fails when name is duplicated among active accounts case-insensitively with accents`() =
        runTest {
            val existing =
                Account(
                    id = 1L,
                    name = "Nómina",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(1000L),
                    color = 1,
                    icon = "savings",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(existing))

            val duplicateCandidate =
                Account(
                    name = "  nómina  ",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 2,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(duplicateCandidate)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NameDuplicated)
        }

    @Test
    fun `allows duplicate name if existing account with same name is archived`() =
        runTest {
            val archived =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    isArchived = true,
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(archived))

            val newAccount =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 2,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(newAccount)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `allows same name when editing account itself`() =
        runTest {
            val existing =
                Account(
                    id = 1L,
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(5000L),
                    color = 1,
                    icon = "savings",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(existing))

            val edited = existing.copy(color = 3)
            val result = saveAccountUseCase(edited)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(1L)
        }

    @Test
    fun `saves new account successfully and trims whitespace from name`() =
        runTest {
            val newAccount =
                Account(
                    name = "  Billetera  ",
                    type = AccountType.CASH,
                    initialBalance = Money(15000L),
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )

            val result = saveAccountUseCase(newAccount)
            assertThat(result.isSuccess).isTrue()
            val id = result.getOrNull()!!
            val saved = accountRepository.getAccount(id)
            assertThat(saved?.name).isEqualTo("Billetera")
        }
}
