package com.crguarin.wallet.domain.usecase

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class ObserveMonthSummaryUseCaseTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var observeMonthSummaryUseCase: ObserveMonthSummaryUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository)
    }

    @Test
    fun `observes month summary correctly for specified month and account`() =
        runTest {
            val incomeTx =
                Transaction(
                    id = 1L,
                    type = TransactionType.INCOME,
                    amount = Money(500_000L),
                    accountId = 1L,
                    categoryId = 2L,
                    date = LocalDate.of(2026, 9, 15),
                    createdAt = now,
                    updatedAt = now,
                )
            val expenseTx =
                Transaction(
                    id = 2L,
                    type = TransactionType.EXPENSE,
                    amount = Money(150_000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = LocalDate.of(2026, 9, 20),
                    createdAt = now,
                    updatedAt = now,
                )
            val otherMonthTx =
                Transaction(
                    id = 3L,
                    type = TransactionType.INCOME,
                    amount = Money(300_000L),
                    accountId = 1L,
                    categoryId = 2L,
                    date = LocalDate.of(2026, 8, 15),
                    createdAt = now,
                    updatedAt = now,
                )
            val otherAccountTx =
                Transaction(
                    id = 4L,
                    type = TransactionType.EXPENSE,
                    amount = Money(50_000L),
                    accountId = 2L,
                    categoryId = 1L,
                    date = LocalDate.of(2026, 9, 22),
                    createdAt = now,
                    updatedAt = now,
                )

            transactionRepository.setInitialData(listOf(incomeTx, expenseTx, otherMonthTx, otherAccountTx))

            observeMonthSummaryUseCase(YearMonth.of(2026, 9), accountId = 1L).test {
                val summary = awaitItem()
                assertThat(summary.income).isEqualTo(Money(500_000L))
                assertThat(summary.expense).isEqualTo(Money(150_000L))
                assertThat(summary.net).isEqualTo(Money(350_000L))
                cancelAndIgnoreRemainingEvents()
            }
        }
}
