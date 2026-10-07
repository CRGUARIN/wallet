package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SetRecurringActiveUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var setRecurringActiveUseCase: SetRecurringActiveUseCase
    private lateinit var deleteRecurringPaymentUseCase: DeleteRecurringPaymentUseCase
    private lateinit var transactionRunner: FakeTransactionRunner

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        transactionRunner = FakeTransactionRunner()
        setRecurringActiveUseCase = SetRecurringActiveUseCase(recurringRepository, timeProvider, transactionRunner)
        deleteRecurringPaymentUseCase = DeleteRecurringPaymentUseCase(recurringRepository)
    }

    @Test
    fun `pausing deactivates without moving nextDueDate`() =
        runTest {
            val due = LocalDate.of(2026, 10, 3)
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = due, nextDueDate = due)))
            assertThat(setRecurringActiveUseCase(1L, false)).isEqualTo(DomainResult.Success(Unit))
            val stored = recurringRepository.get(1L)
            assertThat(stored?.isActive).isFalse()
            assertThat(stored?.nextDueDate).isEqualTo(due)
        }

    @Test
    fun `resuming recalculates firstOnOrAfter today and skips the pause`() =
        runTest {
            val start = LocalDate.of(2026, 8, 5)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = start,
                        nextDueDate = LocalDate.of(2026, 9, 5),
                        isActive = false,
                    ),
                ),
            )
            assertThat(setRecurringActiveUseCase(1L, true)).isEqualTo(DomainResult.Success(Unit))
            val stored = recurringRepository.get(1L)
            assertThat(stored?.isActive).isTrue()
            assertThat(stored?.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        }

    @Test
    fun `resuming a finished payment fails`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 8, 5),
                        nextDueDate = LocalDate.of(2026, 10, 5),
                        endDate = LocalDate.of(2026, 9, 5),
                        isActive = false,
                    ),
                ),
            )
            assertThat(setRecurringActiveUseCase(1L, true).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `toggling a missing payment fails`() =
        runTest {
            assertThat(setRecurringActiveUseCase(99L, false).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `delete removes the payment`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            assertThat(deleteRecurringPaymentUseCase(1L)).isEqualTo(DomainResult.Success(Unit))
            assertThat(recurringRepository.get(1L)).isNull()
        }

    @Test
    fun `delete of missing payment fails`() =
        runTest {
            assertThat(deleteRecurringPaymentUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `resuming never moves nextDueDate back over a recorded occurrence`() =
        runTest {
            // Today's charge already ran (nextDueDate advanced to November); pausing and resuming
            // on the same day must keep November, not bring October back.
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 1, 5),
                        nextDueDate = LocalDate.of(2026, 11, 5),
                        isActive = false,
                    ),
                ),
            )
            assertThat(setRecurringActiveUseCase(1L, true)).isEqualTo(DomainResult.Success(Unit))
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        }

    @Test
    fun `weekly resume lands on the calendar anchor`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 9, 28),
                        nextDueDate = LocalDate.of(2026, 9, 28),
                        frequency = RecurrenceFrequency.WEEKLY,
                        isActive = false,
                    ),
                ),
            )
            setRecurringActiveUseCase(1L, true)
            // Weekly from 2026-09-28: occurrences 09-28, 10-05, 10-12...; today is 10-05.
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        }

    @Test
    fun `pausing and resuming read and write inside one transaction`() =
        runTest {
            val due = LocalDate.of(2026, 10, 3)
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = due, nextDueDate = due)))

            setRecurringActiveUseCase(1L, false)
            setRecurringActiveUseCase(1L, true)

            assertThat(transactionRunner.runs).isEqualTo(2)
        }
}
