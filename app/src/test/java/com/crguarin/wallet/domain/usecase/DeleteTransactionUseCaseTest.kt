package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DeleteTransactionUseCaseTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository, FakeCardStatementRepository(), FakeTransactionRunner())
    }

    @Test
    fun `fails when transaction not found`() =
        runTest {
            val result = deleteTransactionUseCase(999L)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `deletes transaction successfully`() =
        runTest {
            val tx =
                Transaction(
                    id = 1L,
                    type = TransactionType.EXPENSE,
                    amount = Money(1500L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = LocalDate.of(2026, 9, 28),
                    createdAt = now,
                    updatedAt = now,
                )
            transactionRepository.setInitialData(listOf(tx))

            val result = deleteTransactionUseCase(1L)
            assertThat(result.isSuccess).isTrue()
            assertThat(transactionRepository.getTransaction(1L)).isNull()
        }
}
