package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveCardOverviewUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var cardRepository: FakeCardStatementRepository
    private lateinit var observeCardOverviewUseCase: ObserveCardOverviewUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    private val cardAccount =
        Account(
            id = 1L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-200_000L),
            color = 1,
            icon = "credit_card",
            createdAt = now,
            creditLimit = Money(1_000_000L),
            statementDay = 15,
            paymentDueDay = 25,
        )
    private val savingsAccount =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(500_000L),
            color = 2,
            icon = "savings",
            createdAt = now,
        )

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        cardRepository = FakeCardStatementRepository()
        accountRepository.setInitialData(listOf(cardAccount, savingsAccount))
        observeCardOverviewUseCase =
            ObserveCardOverviewUseCase(
                accountRepository = accountRepository,
                cardStatementRepository = cardRepository,
                timeProvider = FakeTimeProvider(now),
            )
    }

    private fun expense(
        id: Long,
        date: LocalDate,
        amount: Long,
        installments: Int? = null,
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(amount),
        accountId = 1L,
        categoryId = 1L,
        date = date,
        installments = installments,
        createdAt = now,
        updatedAt = now,
    )

    private fun statement(
        closing: LocalDate,
        due: LocalDate,
        balance: Long,
    ) = CardStatement(
        id = 1L,
        accountId = 1L,
        closingDate = closing,
        dueDate = due,
        statementBalance = Money(balance),
        minimumPayment = null,
        statedInterestAndFees = null,
        adjustmentTransactionId = null,
        createdAt = now,
    )

    @Test
    fun `non-card account emits null`() =
        runTest {
            assertThat(observeCardOverviewUseCase(2L).first()).isNull()
        }

    @Test
    fun `missing account emits null`() =
        runTest {
            assertThat(observeCardOverviewUseCase(99L).first()).isNull()
        }

    @Test
    fun `basic figures without statements`() =
        runTest {
            cardRepository.setInitialData(
                emptyList(),
                listOf(
                    expense(1L, LocalDate.of(2026, 9, 20), 60_000L),
                    expense(2L, LocalDate.of(2026, 9, 20), 90_000L, installments = 3),
                    expense(3L, LocalDate.of(2026, 8, 10), 40_000L),
                ),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.debt).isEqualTo(Money(200_000L))
            assertThat(overview.available).isEqualTo(Money(800_000L))
            assertThat(overview.usedPerMille).isEqualTo(200)
            assertThat(overview.nextClosingDate).isEqualTo(LocalDate.of(2026, 10, 15))
            assertThat(overview.lastStatement).isNull()
            assertThat(overview.pendingStatementAmount).isNull()
            assertThat(overview.paymentStatus).isNull()
            // Single-installment purchase of the current cycle in full, plus the plan
            // capital billed at the next closing.
            assertThat(overview.nextStatementCapitalEstimate).isEqualTo(Money(90_000L))

            assertThat(overview.activeInstallments).hasSize(1)
            val plan = overview.activeInstallments.single()
            assertThat(plan.transactionId).isEqualTo(2L)
            assertThat(plan.billedInstallments).isEqualTo(0)
            assertThat(plan.remainingAmount).isEqualTo(Money(90_000L))
            assertThat(plan.nextInstallmentCapital).isEqualTo(Money(30_000L))
        }

    @Test
    fun `pending statement amount discounts payments after closing`() =
        runTest {
            val payment =
                Transaction(
                    id = 10L,
                    type = TransactionType.TRANSFER,
                    amount = Money(100_000L),
                    accountId = 2L,
                    toAccountId = 1L,
                    date = LocalDate.of(2026, 9, 20),
                    createdAt = now,
                    updatedAt = now,
                )
            cardRepository.setInitialData(
                listOf(statement(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 5), 500_000L)),
                listOf(expense(1L, LocalDate.of(2026, 9, 10), 60_000L), payment),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.pendingStatementAmount).isEqualTo(Money(400_000L))
            assertThat(overview.pendingStatementDueDate).isEqualTo(LocalDate.of(2026, 10, 5))
            assertThat(overview.paymentStatus).isEqualTo(StatementPaymentStatus.PENDING)
        }

    @Test
    fun `overdue when due date passed`() =
        runTest {
            cardRepository.setInitialData(
                listOf(statement(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 20), 500_000L)),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.pendingStatementAmount).isEqualTo(Money(500_000L))
            assertThat(overview.paymentStatus).isEqualTo(StatementPaymentStatus.OVERDUE)
        }

    @Test
    fun `paid when payments cover the statement`() =
        runTest {
            val payment =
                Transaction(
                    id = 10L,
                    type = TransactionType.TRANSFER,
                    amount = Money(500_000L),
                    accountId = 2L,
                    toAccountId = 1L,
                    date = LocalDate.of(2026, 9, 20),
                    createdAt = now,
                    updatedAt = now,
                )
            cardRepository.setInitialData(
                listOf(statement(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 5), 500_000L)),
                listOf(payment),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.pendingStatementAmount).isEqualTo(Money.ZERO)
            assertThat(overview.paymentStatus).isEqualTo(StatementPaymentStatus.PAID)
        }

    @Test
    fun `finished plans are not active`() =
        runTest {
            cardRepository.setInitialData(
                emptyList(),
                listOf(expense(1L, LocalDate.of(2026, 6, 1), 90_000L, installments = 2)),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.activeInstallments).isEmpty()
        }

    @Test
    fun `active plans come most recent first`() =
        runTest {
            cardRepository.setInitialData(
                emptyList(),
                listOf(
                    expense(1L, LocalDate.of(2026, 9, 5), 60_000L, installments = 6),
                    expense(2L, LocalDate.of(2026, 9, 20), 90_000L, installments = 3),
                ),
            )

            val overview = observeCardOverviewUseCase(1L).first()!!
            assertThat(overview.activeInstallments.map { it.transactionId })
                .containsExactly(2L, 1L)
                .inOrder()
        }
}
