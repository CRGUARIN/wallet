package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RestoreTransactionUseCaseTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var restoreTransactionUseCase: RestoreTransactionUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")
    private val transaction =
        Transaction(
            id = 7L,
            type = TransactionType.EXPENSE,
            amount = Money(150_000L),
            accountId = 1L,
            categoryId = 3L,
            date = LocalDate.of(2026, 9, 27),
            note = "Almuerzo",
            createdAt = now,
            updatedAt = now,
        )

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        restoreTransactionUseCase = RestoreTransactionUseCase(transactionRepository)
    }

    @Test
    fun `restores a deleted transaction preserving its id and fields`() =
        runTest {
            transactionRepository.setInitialData(listOf(transaction))
            transactionRepository.deleteTransaction(transaction.id)
            assertThat(transactionRepository.getTransaction(transaction.id)).isNull()

            val result = restoreTransactionUseCase(transaction)

            assertThat(result.isSuccess).isTrue()
            assertThat(transactionRepository.getTransaction(transaction.id)).isEqualTo(transaction)
            assertThat(transactionRepository.getTransaction(transaction.id)?.createdAt).isEqualTo(now)
            assertThat(transactionRepository.getTransaction(transaction.id)?.updatedAt).isEqualTo(now)
            assertThat(transactionRepository.getTransaction(transaction.id)?.note).isEqualTo("Almuerzo")
        }

    @Test
    fun `fails with NotFound when the repository cannot restore the transaction`() =
        runTest {
            transactionRepository.failRestore = true

            val result = restoreTransactionUseCase(transaction)

            assertThat(result).isEqualTo(DomainResult.Failure(DomainError.NotFound))
            assertThat(transactionRepository.getTransaction(transaction.id)).isNull()
        }

    @Test
    fun `restored transaction keeps its category and dates`() =
        runTest {
            restoreTransactionUseCase(transaction)

            val restored = transactionRepository.getTransaction(7L)
            assertThat(restored?.categoryId).isEqualTo(3L)
            assertThat(restored?.date).isEqualTo(LocalDate.of(2026, 9, 27))
        }
}
