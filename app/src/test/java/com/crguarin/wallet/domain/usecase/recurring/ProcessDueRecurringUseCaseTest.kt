package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCardAccount
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

class ProcessDueRecurringUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase
    private lateinit var processDueRecurringUseCase: ProcessDueRecurringUseCase

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        accountRepository.setInitialData(listOf(testAccount(), testCardAccount()))
        categoryRepository.setInitialData(listOf(testCategory()))
        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository,
                accountRepository,
                categoryRepository,
                timeProvider,
                CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
                FakeTransactionRunner(),
            )
        processDueRecurringUseCase =
            ProcessDueRecurringUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                saveTransactionUseCase,
                timeProvider,
            )
    }

    @Test
    fun `nothing due records nothing`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today, nextDueDate = today.plusDays(1))),
            )
            assertThat(processDueRecurringUseCase()).isEmpty()
            assertThat(recurringRepository.completedTransactions).isEmpty()
        }

    @Test
    fun `payment due today is recorded with note fallback and recurringId`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            val posted = processDueRecurringUseCase()
            assertThat(posted).hasSize(1)
            assertThat(posted[0].date).isEqualTo(today)
            assertThat(posted[0].paymentId).isEqualTo(1L)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
            val recorded = recurringRepository.completedTransactions.single()
            assertThat(recorded.note).isEqualTo("Netflix")
            assertThat(recorded.recurringId).isEqualTo(1L)
            assertThat(recorded.date).isEqualTo(today)
        }

    @Test
    fun `explicit note is kept`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today, nextDueDate = today, note = "Plan familiar")),
            )
            processDueRecurringUseCase()
            assertThat(recurringRepository.completedTransactions.single().note).isEqualTo("Plan familiar")
        }

    @Test
    fun `three month catch-up records every charge with its real date`() =
        runTest {
            val start = LocalDate.of(2026, 7, 5)
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = start, nextDueDate = LocalDate.of(2026, 8, 5))),
            )
            val posted = processDueRecurringUseCase()
            assertThat(posted.map { it.date }).containsExactly(
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 10, 5),
            )
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        }

    @Test
    fun `catch-up is capped at 120 per payment`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2020, 1, 1),
                        nextDueDate = LocalDate.of(2020, 1, 1),
                        frequency = RecurrenceFrequency.WEEKLY,
                    ),
                ),
            )
            val posted = processDueRecurringUseCase()
            assertThat(posted).hasSize(120)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2022, 4, 20))
        }

    @Test
    fun `endDate stops catch-up halfway`() =
        runTest {
            val start = LocalDate.of(2026, 7, 5)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = start,
                        nextDueDate = start,
                        endDate = LocalDate.of(2026, 8, 5),
                    ),
                ),
            )
            val posted = processDueRecurringUseCase()
            assertThat(posted.map { it.date }).containsExactly(
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 8, 5),
            )
            val stored = recurringRepository.get(1L)
            assertThat(stored?.nextDueDate).isEqualTo(LocalDate.of(2026, 9, 5))
            assertThat(stored?.isFinished).isTrue()
        }

    @Test
    fun `paused confirm and blocked payments are skipped`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(id = 1L, startDate = today, nextDueDate = today, isActive = false),
                    testPayment(id = 2L, startDate = today, nextDueDate = today, mode = RecurringMode.CONFIRM),
                    testPayment(id = 3L, startDate = today, nextDueDate = today, accountId = 99L),
                ),
            )
            accountRepository.setInitialData(
                listOf(testAccount(), testAccount(id = 4L, name = "Archived", isArchived = true)),
            )
            recurringRepository.save(testPayment(id = 4L, startDate = today, nextDueDate = today, accountId = 4L))
            categoryRepository.setInitialData(listOf(testCategory(isArchived = true)))
            assertThat(processDueRecurringUseCase()).isEmpty()
            assertThat(recurringRepository.completedTransactions).isEmpty()
        }

    @Test
    fun `alreadyHandled counts as not recorded`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            val blocking: RecurringRepository =
                object : RecurringRepository by recurringRepository {
                    override suspend fun completeOccurrence(
                        paymentId: Long,
                        expectedDueDate: LocalDate,
                        transaction: Transaction?,
                        newNextDueDate: LocalDate,
                    ): OccurrenceResult = OccurrenceResult.AlreadyHandled
                }
            val useCase =
                ProcessDueRecurringUseCase(
                    blocking,
                    accountRepository,
                    categoryRepository,
                    saveTransactionUseCase,
                    timeProvider,
                )
            assertThat(useCase()).isEmpty()
        }

    @Test
    fun `running twice records each occurrence once`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            assertThat(processDueRecurringUseCase()).hasSize(1)
            assertThat(processDueRecurringUseCase()).isEmpty()
            assertThat(recurringRepository.completedTransactions).hasSize(1)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        }

    @Test
    fun `a repository failure returns empty and never throws`() =
        runTest {
            val throwingRepository: RecurringRepository =
                object : RecurringRepository by recurringRepository {
                    override suspend fun getAll(): List<RecurringPayment> = throw IllegalStateException("db down")
                }
            val useCase =
                ProcessDueRecurringUseCase(
                    throwingRepository,
                    accountRepository,
                    categoryRepository,
                    saveTransactionUseCase,
                    timeProvider,
                )
            assertThat(useCase()).isEmpty()
        }

    @Test
    fun `a payment failing validation does not stop the others`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(id = 1L, startDate = today, nextDueDate = today, amount = Money(Money.MAX_MINOR_UNITS + 1)),
                    testPayment(id = 2L, startDate = today, nextDueDate = today),
                ),
            )
            val posted = processDueRecurringUseCase()
            assertThat(posted.map { it.paymentId }).containsExactly(2L)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(today)
        }

    @Test
    fun `card charge over the limit is recorded anyway`() =
        runTest {
            val tinyLimitCard = testCardAccount().copy(creditLimit = Money(10_000L))
            accountRepository.setInitialData(listOf(tinyLimitCard))
            recurringRepository.setInitialData(
                listOf(
                    testPayment(id = 1L, startDate = today, nextDueDate = today, accountId = tinyLimitCard.id),
                ),
            )
            val posted = processDueRecurringUseCase()
            assertThat(posted).hasSize(1)
        }
}
