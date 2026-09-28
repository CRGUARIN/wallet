package com.crguarin.wallet.domain.usecase

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveTransactionsUseCaseTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var observeTransactionsUseCase: ObserveTransactionsUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        observeTransactionsUseCase = ObserveTransactionsUseCase(transactionRepository)
    }

    @Test
    fun `observes paged transactions`() =
        runTest {
            val tx =
                Transaction(
                    id = 1L,
                    type = TransactionType.EXPENSE,
                    amount = Money(2500L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = LocalDate.of(2026, 9, 28),
                    createdAt = now,
                    updatedAt = now,
                )
            transactionRepository.setInitialData(listOf(tx))

            observeTransactionsUseCase(TransactionFilter()).test {
                val pagingData = awaitItem()
                assertThat(pagingData).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
