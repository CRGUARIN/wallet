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

class SaveAccountCardTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var saveAccountUseCase: SaveAccountUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        saveAccountUseCase = SaveAccountUseCase(accountRepository, FakeTimeProvider(now))
    }

    private fun card(
        id: Long = 0L,
        creditLimit: Money? = Money(1_000_000L),
        statementDay: Int? = 15,
        paymentDueDay: Int? = 25,
        initialBalance: Money = Money(-200_000L),
    ) = Account(
        id = id,
        name = "RappiCard",
        type = AccountType.CREDIT_CARD,
        initialBalance = initialBalance,
        color = 1,
        icon = "credit_card",
        createdAt = now,
        creditLimit = creditLimit,
        statementDay = statementDay,
        paymentDueDay = paymentDueDay,
    )

    @Test
    fun `card without limit fails with AmountInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(card(creditLimit = null)).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `card with zero or negative limit fails with AmountInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(card(creditLimit = Money.ZERO)).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
            assertThat(saveAccountUseCase(card(creditLimit = Money(-1L))).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `card with day outside 1-31 fails with CardDayInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(card(statementDay = 0)).errorOrNull())
                .isEqualTo(DomainError.CardDayInvalid)
            assertThat(saveAccountUseCase(card(statementDay = 32)).errorOrNull())
                .isEqualTo(DomainError.CardDayInvalid)
            assertThat(saveAccountUseCase(card(statementDay = null)).errorOrNull())
                .isEqualTo(DomainError.CardDayInvalid)
            assertThat(saveAccountUseCase(card(paymentDueDay = 0)).errorOrNull())
                .isEqualTo(DomainError.CardDayInvalid)
            assertThat(saveAccountUseCase(card(paymentDueDay = null)).errorOrNull())
                .isEqualTo(DomainError.CardDayInvalid)
        }

    @Test
    fun `card accepts boundary days 1 and 31`() =
        runTest {
            assertThat(saveAccountUseCase(card(statementDay = 1, paymentDueDay = 31)).isSuccess).isTrue()
        }

    @Test
    fun `card accepts negative and positive initial balance`() =
        runTest {
            val debtId = saveAccountUseCase(card(initialBalance = Money(-200_000L))).getOrNull()!!
            assertThat(accountRepository.getAccount(debtId)?.initialBalance).isEqualTo(Money(-200_000L))

            val favorId =
                saveAccountUseCase(card(initialBalance = Money(50_000L)).copy(name = "Otra"))
                    .getOrNull()!!
            assertThat(accountRepository.getAccount(favorId)?.initialBalance).isEqualTo(Money(50_000L))
        }

    @Test
    fun `non-card account clears card fields before saving`() =
        runTest {
            val account =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                    creditLimit = Money(100L),
                    statementDay = 5,
                    paymentDueDay = 10,
                )
            val id = saveAccountUseCase(account).getOrNull()!!
            val saved = accountRepository.getAccount(id)!!
            assertThat(saved.creditLimit).isNull()
            assertThat(saved.statementDay).isNull()
            assertThat(saved.paymentDueDay).isNull()
        }

    @Test
    fun `non-card account with negative initial balance still fails`() =
        runTest {
            val account =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(-100L),
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            assertThat(saveAccountUseCase(account).errorOrNull()).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `changing type towards card without transactions succeeds`() =
        runTest {
            val cash =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(cash))

            val result = saveAccountUseCase(card(id = 1L, initialBalance = Money.ZERO))
            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)?.type).isEqualTo(AccountType.CREDIT_CARD)
        }

    @Test
    fun `changing type towards card with transactions fails`() =
        runTest {
            val cash =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(cash), txCounts = mapOf(1L to 2))

            assertThat(saveAccountUseCase(card(id = 1L, initialBalance = Money.ZERO)).errorOrNull())
                .isEqualTo(DomainError.AccountTypeChangeNotAllowed)
        }

    @Test
    fun `changing type away from card with transactions fails`() =
        runTest {
            val stored = card(id = 1L)
            accountRepository.setInitialData(listOf(stored), txCounts = mapOf(1L to 1))

            val asCash =
                stored.copy(
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    creditLimit = null,
                    statementDay = null,
                    paymentDueDay = null,
                )
            assertThat(saveAccountUseCase(asCash).errorOrNull())
                .isEqualTo(DomainError.AccountTypeChangeNotAllowed)
        }

    @Test
    fun `changing type between non-card types with transactions succeeds`() =
        runTest {
            val cash =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 1,
                    icon = "wallet",
                    createdAt = now,
                )
            accountRepository.setInitialData(listOf(cash), txCounts = mapOf(1L to 3))

            val result = saveAccountUseCase(cash.copy(type = AccountType.SAVINGS))
            assertThat(result.isSuccess).isTrue()
        }
}
