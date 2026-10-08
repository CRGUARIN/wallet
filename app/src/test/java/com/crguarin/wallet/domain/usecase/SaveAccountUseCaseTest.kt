package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
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
        saveAccountUseCase = SaveAccountUseCase(accountRepository, FakeTimeProvider(now))
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

    @Test
    fun `new account gets createdAt from clock and is appended at the end`() =
        runTest {
            val base =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = Instant.EPOCH,
                    sortOrder = 99,
                )
            val firstId = saveAccountUseCase(base).getOrNull()!!
            val secondId = saveAccountUseCase(base.copy(name = "Ahorros", color = 2)).getOrNull()!!

            val first = accountRepository.getAccount(firstId)!!
            val second = accountRepository.getAccount(secondId)!!
            assertThat(first.createdAt).isEqualTo(now)
            assertThat(first.sortOrder).isEqualTo(0)
            assertThat(second.sortOrder).isEqualTo(1)
        }

    @Test
    fun `editing keeps createdAt, sortOrder and archived flag from storage`() =
        runTest {
            val id =
                saveAccountUseCase(
                    Account(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        initialBalance = Money.ZERO,
                        color = 1,
                        icon = "wallet",
                        createdAt = now,
                    ),
                ).getOrNull()!!
            val stored = accountRepository.getAccount(id)!!

            val result =
                saveAccountUseCase(
                    stored.copy(name = "Caja", createdAt = Instant.EPOCH, sortOrder = 7, isArchived = true),
                )

            assertThat(result.isSuccess).isTrue()
            val updated = accountRepository.getAccount(id)!!
            assertThat(updated.name).isEqualTo("Caja")
            assertThat(updated.createdAt).isEqualTo(now)
            assertThat(updated.sortOrder).isEqualTo(stored.sortOrder)
            assertThat(updated.isArchived).isFalse()
        }

    @Test
    fun `editing a missing account fails with NotFound`() =
        runTest {
            val result =
                saveAccountUseCase(
                    Account(
                        id = 42,
                        name = "Fantasma",
                        type = AccountType.CASH,
                        initialBalance = Money.ZERO,
                        color = 1,
                        icon = "wallet",
                        createdAt = now,
                    ),
                )
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `new account with a color used by an active account fails with ColorInUse`() =
        runTest {
            accountRepository.setInitialData(listOf(savingsAccount(id = 1L, color = 7)))

            val result = saveAccountUseCase(cashAccount(color = 7))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
            assertThat(accountRepository.getAllAccounts()).hasSize(1)
        }

    @Test
    fun `archived account does not block its color`() =
        runTest {
            accountRepository.setInitialData(listOf(savingsAccount(id = 1L, color = 7).copy(isArchived = true)))

            val result = saveAccountUseCase(cashAccount(color = 7))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `editing an account to a color used by another active account fails with ColorInUse`() =
        runTest {
            accountRepository.setInitialData(
                listOf(savingsAccount(id = 1L, color = 7), savingsAccount(id = 2L, color = 8).copy(name = "Ahorros 2")),
            )

            val result = saveAccountUseCase(savingsAccount(id = 2L, color = 8).copy(name = "Ahorros 2", color = 7))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.ColorInUse)
            assertThat(accountRepository.getAccount(2L)?.color).isEqualTo(8)
        }

    @Test
    fun `an unchanged color shared by two accounts still saves`() =
        runTest {
            val second = savingsAccount(id = 2L, color = 7).copy(name = "Ahorros 2")
            accountRepository.setInitialData(listOf(savingsAccount(id = 1L, color = 7), second))

            val result = saveAccountUseCase(second.copy(initialBalance = Money(2_000L)))

            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(2L)?.initialBalance).isEqualTo(Money(2_000L))
        }

    @Test
    fun `editing an account keeps its own color without a clash`() =
        runTest {
            accountRepository.setInitialData(listOf(savingsAccount(id = 1L, color = 7)))

            val result = saveAccountUseCase(savingsAccount(id = 1L, color = 7).copy(name = "Ahorros 2"))

            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)?.color).isEqualTo(7)
        }

    private fun savingsAccount(
        id: Long,
        color: Int,
    ) = Account(
        id = id,
        name = "Nómina",
        type = AccountType.SAVINGS,
        initialBalance = Money(1_000L),
        color = color,
        icon = "savings",
        createdAt = now,
    )

    private fun cashAccount(color: Int) =
        Account(
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money.ZERO,
            color = color,
            icon = "wallet",
            createdAt = now,
        )
}
