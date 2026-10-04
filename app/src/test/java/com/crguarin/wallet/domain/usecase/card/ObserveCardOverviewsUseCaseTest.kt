package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ObserveCardOverviewsUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var observeCardOverviewsUseCase: ObserveCardOverviewsUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    private fun card(
        id: Long,
        name: String,
        archived: Boolean = false,
    ) = Account(
        id = id,
        name = name,
        type = AccountType.CREDIT_CARD,
        initialBalance = Money(-100_000L),
        color = 1,
        icon = "credit_card",
        isArchived = archived,
        createdAt = now,
        creditLimit = Money(1_000_000L),
        statementDay = 15,
        paymentDueDay = 25,
    )

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        val cardRepository = FakeCardStatementRepository()
        observeCardOverviewsUseCase =
            ObserveCardOverviewsUseCase(
                accountRepository = accountRepository,
                observeCardOverviewUseCase =
                    ObserveCardOverviewUseCase(
                        accountRepository = accountRepository,
                        cardStatementRepository = cardRepository,
                        timeProvider = FakeTimeProvider(now),
                    ),
            )
    }

    @Test
    fun `emits one overview per non-archived card`() =
        runTest {
            accountRepository.setInitialData(
                listOf(
                    card(1L, "RappiCard"),
                    card(2L, "Otra"),
                    Account(
                        id = 3L,
                        name = "Ahorros",
                        type = AccountType.SAVINGS,
                        initialBalance = Money(500_000L),
                        color = 2,
                        icon = "savings",
                        createdAt = now,
                    ),
                    card(4L, "Vieja", archived = true),
                ),
            )

            val overviews = observeCardOverviewsUseCase().first()
            assertThat(overviews.keys).containsExactly(1L, 2L)
            assertThat(overviews.getValue(1L).debt).isEqualTo(Money(100_000L))
        }

    @Test
    fun `emits empty map without cards`() =
        runTest {
            accountRepository.setInitialData(emptyList())

            assertThat(observeCardOverviewsUseCase().first()).isEmpty()
        }
}
