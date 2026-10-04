package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConfirmSkipRecurringUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase
    private lateinit var confirmRecurringOccurrenceUseCase: ConfirmRecurringOccurrenceUseCase
    private lateinit var skipRecurringOccurrenceUseCase: SkipRecurringOccurrenceUseCase

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        val transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        accountRepository.setInitialData(listOf(testAccount()))
        categoryRepository.setInitialData(listOf(testCategory()))
        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository,
                accountRepository,
                categoryRepository,
                timeProvider,
                CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
            )
        confirmRecurringOccurrenceUseCase =
            ConfirmRecurringOccurrenceUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                saveTransactionUseCase,
                timeProvider,
            )
        skipRecurringOccurrenceUseCase = SkipRecurringOccurrenceUseCase(recurringRepository, timeProvider)
    }

    @Test
    fun `confirm records edited amount and date and advances`() =
        runTest {
            val due = LocalDate.of(2026, 10, 3)
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = due, nextDueDate = due, mode = RecurringMode.CONFIRM)),
            )
            val result =
                confirmRecurringOccurrenceUseCase(
                    1L,
                    Money(52_300L),
                    LocalDate.of(2026, 10, 4),
                )
            assertThat(result.isSuccess).isTrue()
            val recorded = recurringRepository.completedTransactions.single()
            assertThat(recorded.amount).isEqualTo(Money(52_300L))
            assertThat(recorded.date).isEqualTo(LocalDate.of(2026, 10, 4))
            assertThat(recorded.recurringId).isEqualTo(1L)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 3))
        }

    @Test
    fun `confirm on missing payment fails with NotFound`() =
        runTest {
            val result = confirmRecurringOccurrenceUseCase(99L, Money(1_000L), today)
            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `confirm with invalid amount or far date fails`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today, nextDueDate = today, mode = RecurringMode.CONFIRM)),
            )
            assertThat(confirmRecurringOccurrenceUseCase(1L, Money.ZERO, today).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
            assertThat(
                confirmRecurringOccurrenceUseCase(1L, Money(1_000L), today.plusYears(2)).errorOrNull(),
            ).isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `skip advances without recording`() =
        runTest {
            val due = LocalDate.of(2026, 10, 3)
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = due, nextDueDate = due, mode = RecurringMode.CONFIRM)),
            )
            val result = skipRecurringOccurrenceUseCase(1L)
            assertThat(result).isEqualTo(DomainResult.Success(Unit))
            assertThat(recurringRepository.completedTransactions).isEmpty()
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 3))
        }

    @Test
    fun `skip on missing payment fails with NotFound`() =
        runTest {
            assertThat(skipRecurringOccurrenceUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `confirm already handled by a concurrent run fails with NotFound`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today, nextDueDate = today, mode = RecurringMode.CONFIRM)),
            )
            val useCase =
                ConfirmRecurringOccurrenceUseCase(
                    alreadyHandledRepository(),
                    accountRepository,
                    categoryRepository,
                    saveTransactionUseCase,
                    timeProvider,
                )
            assertThat(useCase(1L, Money(1_000L), today).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `skip already handled by a concurrent run fails with NotFound`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today, nextDueDate = today, mode = RecurringMode.CONFIRM)),
            )
            val useCase = SkipRecurringOccurrenceUseCase(alreadyHandledRepository(), timeProvider)
            assertThat(useCase(1L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    private fun alreadyHandledRepository(): RecurringRepository =
        object : RecurringRepository by recurringRepository {
            override suspend fun completeOccurrence(
                paymentId: Long,
                expectedDueDate: LocalDate,
                transaction: Transaction?,
                newNextDueDate: LocalDate,
            ): OccurrenceResult = OccurrenceResult.AlreadyHandled
        }
}
