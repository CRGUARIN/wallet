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

class SaveAccountTransitTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var saveAccountUseCase: SaveAccountUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        saveAccountUseCase = SaveAccountUseCase(accountRepository, FakeTimeProvider(now))
    }

    private fun transit(
        id: Long = 0L,
        fare: Money? = Money(350_000L),
        initialBalance: Money = Money(1_000_000L),
        color: Int = 1,
    ) = Account(
        id = id,
        name = "Cívica",
        type = AccountType.TRANSIT_CARD,
        initialBalance = initialBalance,
        color = color,
        icon = "directions_bus",
        createdAt = now,
        fare = fare,
    )

    @Test
    fun `valid transit card saves its fare`() =
        runTest {
            val id = saveAccountUseCase(transit()).getOrNull()!!

            val saved = accountRepository.getAccount(id)!!
            assertThat(saved.type).isEqualTo(AccountType.TRANSIT_CARD)
            assertThat(saved.fare).isEqualTo(Money(350_000L))
        }

    @Test
    fun `transit card with the maximum fare is valid`() =
        runTest {
            assertThat(saveAccountUseCase(transit(fare = Money.MAX)).isSuccess).isTrue()
        }

    @Test
    fun `transit card without fare fails with FareInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(transit(fare = null)).errorOrNull()).isEqualTo(DomainError.FareInvalid)
        }

    @Test
    fun `transit card with zero, negative or too large fare fails with FareInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(transit(fare = Money.ZERO)).errorOrNull()).isEqualTo(DomainError.FareInvalid)
            assertThat(saveAccountUseCase(transit(fare = Money(-1L))).errorOrNull()).isEqualTo(DomainError.FareInvalid)
            assertThat(saveAccountUseCase(transit(fare = Money(Money.MAX_MINOR_UNITS + 1))).errorOrNull())
                .isEqualTo(DomainError.FareInvalid)
            assertThat(accountRepository.getAllAccounts()).isEmpty()
        }

    @Test
    fun `transit card with negative initial balance fails with AmountInvalid`() =
        runTest {
            assertThat(saveAccountUseCase(transit(initialBalance = Money(-1L))).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `transit card clears card fields before saving`() =
        runTest {
            val withCardFields =
                transit().copy(
                    creditLimit = Money(100L),
                    statementDay = 5,
                    paymentDueDay = 10,
                )
            val id = saveAccountUseCase(withCardFields).getOrNull()!!

            val saved = accountRepository.getAccount(id)!!
            assertThat(saved.creditLimit).isNull()
            assertThat(saved.statementDay).isNull()
            assertThat(saved.paymentDueDay).isNull()
            assertThat(saved.fare).isEqualTo(Money(350_000L))
        }

    @Test
    fun `fare is cleared on cash and credit card accounts`() =
        runTest {
            val cashId =
                saveAccountUseCase(
                    Account(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        initialBalance = Money.ZERO,
                        color = 1,
                        icon = "wallet",
                        createdAt = now,
                        fare = Money(350_000L),
                    ),
                ).getOrNull()!!
            val cardId =
                saveAccountUseCase(
                    Account(
                        name = "RappiCard",
                        type = AccountType.CREDIT_CARD,
                        initialBalance = Money(-200_000L),
                        color = 2,
                        icon = "credit_card",
                        createdAt = now,
                        creditLimit = Money(1_000_000L),
                        statementDay = 15,
                        paymentDueDay = 25,
                        fare = Money(350_000L),
                    ),
                ).getOrNull()!!

            assertThat(accountRepository.getAccount(cashId)?.fare).isNull()
            assertThat(accountRepository.getAccount(cardId)?.fare).isNull()
        }

    @Test
    fun `changing a cash account to transit card with transactions succeeds`() =
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

            val result = saveAccountUseCase(cash.copy(type = AccountType.TRANSIT_CARD, icon = "directions_bus", fare = Money(350_000L)))

            assertThat(result.isSuccess).isTrue()
            assertThat(accountRepository.getAccount(1L)?.type).isEqualTo(AccountType.TRANSIT_CARD)
        }

    @Test
    fun `changing a transit card to cash with transactions succeeds and drops the fare`() =
        runTest {
            val stored = transit(id = 1L)
            accountRepository.setInitialData(listOf(stored), txCounts = mapOf(1L to 2))

            val result = saveAccountUseCase(stored.copy(type = AccountType.CASH, icon = "wallet"))

            assertThat(result.isSuccess).isTrue()
            val saved = accountRepository.getAccount(1L)!!
            assertThat(saved.type).isEqualTo(AccountType.CASH)
            assertThat(saved.fare).isNull()
        }

    @Test
    fun `changing a transit card to credit card with transactions fails`() =
        runTest {
            val stored = transit(id = 1L)
            accountRepository.setInitialData(listOf(stored), txCounts = mapOf(1L to 2))

            val asCard =
                stored.copy(
                    type = AccountType.CREDIT_CARD,
                    creditLimit = Money(1_000_000L),
                    statementDay = 15,
                    paymentDueDay = 25,
                    fare = null,
                )
            assertThat(saveAccountUseCase(asCard).errorOrNull()).isEqualTo(DomainError.AccountTypeChangeNotAllowed)
        }
}
