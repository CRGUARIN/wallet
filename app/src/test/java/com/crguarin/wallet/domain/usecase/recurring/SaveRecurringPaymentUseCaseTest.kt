package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveRecurringPaymentUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveRecurringPaymentUseCase: SaveRecurringPaymentUseCase

    private val today = LocalDate.of(2026, 10, 5)
    private val now = Instant.parse("2026-10-05T12:00:00Z")

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        timeProvider = FakeTimeProvider(currentInstant = now)
        saveRecurringPaymentUseCase =
            SaveRecurringPaymentUseCase(recurringRepository, accountRepository, categoryRepository, timeProvider)
        accountRepository.setInitialData(listOf(testAccount()))
        categoryRepository.setInitialData(
            listOf(
                testCategory(),
                testCategory(id = 2L, name = "Salario", type = CategoryType.INCOME),
            ),
        )
    }

    @Test
    fun `create sets nextDueDate to startDate`() =
        runTest {
            val result = saveRecurringPaymentUseCase(testPayment(id = 0L, startDate = today))
            val id = (result as DomainResult.Success).value
            assertThat(recurringRepository.get(id)?.nextDueDate).isEqualTo(today)
        }

    @Test
    fun `create stamps createdAt and updatedAt from the time provider`() =
        runTest {
            val result = saveRecurringPaymentUseCase(testPayment(id = 0L, startDate = today))
            val id = (result as DomainResult.Success).value
            val saved = recurringRepository.get(id)
            assertThat(saved?.createdAt).isEqualTo(now)
            assertThat(saved?.updatedAt).isEqualTo(now)
        }

    @Test
    fun `create rejects startDate in the past`() =
        runTest {
            val result = saveRecurringPaymentUseCase(testPayment(id = 0L, startDate = today.minusDays(1)))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.RecurringDateInPast)
        }

    @Test
    fun `create rejects startDate beyond one year`() =
        runTest {
            val result =
                saveRecurringPaymentUseCase(testPayment(id = 0L, startDate = today.plusYears(1).plusDays(1)))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `create rejects endDate before startDate`() =
        runTest {
            val result =
                saveRecurringPaymentUseCase(
                    testPayment(id = 0L, startDate = today, endDate = today.minusDays(1)),
                )
            assertThat(result.errorOrNull()).isEqualTo(DomainError.RecurringEndBeforeStart)
        }

    @Test
    fun `create accepts endDate equal to startDate`() =
        runTest {
            val result =
                saveRecurringPaymentUseCase(testPayment(id = 0L, startDate = today, endDate = today))
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `blank name fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, name = "  ")).errorOrNull())
                .isEqualTo(DomainError.NameEmpty)
        }

    @Test
    fun `too long name fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, name = "x".repeat(41))).errorOrNull())
                .isEqualTo(DomainError.NameTooLong)
        }

    @Test
    fun `invalid amounts fail`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, amount = Money.ZERO)).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
            assertThat(
                saveRecurringPaymentUseCase(testPayment(id = 0L, amount = Money(Money.MAX_MINOR_UNITS + 1))).errorOrNull(),
            ).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `transfer type fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, type = TransactionType.TRANSFER)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `missing account fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, accountId = 99L)).errorOrNull())
                .isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `archived account fails`() =
        runTest {
            accountRepository.setInitialData(listOf(testAccount(isArchived = true)))
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L)).errorOrNull())
                .isEqualTo(DomainError.AccountArchived)
        }

    @Test
    fun `missing category fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, categoryId = 99L)).errorOrNull())
                .isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `archived category fails`() =
        runTest {
            categoryRepository.setInitialData(listOf(testCategory(isArchived = true)))
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L)).errorOrNull())
                .isEqualTo(DomainError.CategoryArchived)
        }

    @Test
    fun `category of another type fails`() =
        runTest {
            assertThat(
                saveRecurringPaymentUseCase(testPayment(id = 0L, type = TransactionType.INCOME)).errorOrNull(),
            ).isEqualTo(DomainError.CategoryTypeMismatch)
        }

    @Test
    fun `interval outside 1 to 12 fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, interval = 0)).errorOrNull())
                .isEqualTo(DomainError.RecurringIntervalInvalid)
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, interval = 13)).errorOrNull())
                .isEqualTo(DomainError.RecurringIntervalInvalid)
        }

    @Test
    fun `reminder outside null or 1 to 7 fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, reminderDaysBefore = 0)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, reminderDaysBefore = 8)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `too long note fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 0L, note = "x".repeat(201))).errorOrNull())
                .isEqualTo(DomainError.NoteTooLong)
        }

    @Test
    fun `edit without calendar change keeps nextDueDate`() =
        runTest {
            val overdue = today.minusDays(3)
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = overdue, nextDueDate = overdue, mode = RecurringMode.CONFIRM)),
            )
            val result =
                saveRecurringPaymentUseCase(
                    testPayment(id = 1L, startDate = overdue, nextDueDate = today, mode = RecurringMode.CONFIRM),
                )
            assertThat(result.isSuccess).isTrue()
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(overdue)
        }

    @Test
    fun `edit with frequency change recalculates nextDueDate from today`() =
        runTest {
            val start = today.minusDays(10)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = start,
                        nextDueDate = start.plusDays(7),
                        frequency = RecurrenceFrequency.WEEKLY,
                    ),
                ),
            )
            val result =
                saveRecurringPaymentUseCase(
                    testPayment(id = 1L, startDate = start, frequency = RecurrenceFrequency.MONTHLY),
                )
            assertThat(result.isSuccess).isTrue()
            // New monthly calendar anchored at start: first occurrence on or after today.
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 25))
        }

    @Test
    fun `edit with a new startDate recalculates nextDueDate from today`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            val newStart = LocalDate.of(2026, 10, 20)
            val result = saveRecurringPaymentUseCase(testPayment(id = 1L, startDate = newStart))
            assertThat(result.isSuccess).isTrue()
            assertThat(recurringRepository.get(1L)?.startDate).isEqualTo(newStart)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(newStart)
        }

    @Test
    fun `edit with an interval change recalculates nextDueDate`() =
        runTest {
            val start = LocalDate.of(2026, 8, 5)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = start,
                        nextDueDate = start,
                        frequency = RecurrenceFrequency.MONTHLY,
                    ),
                ),
            )
            val result =
                saveRecurringPaymentUseCase(
                    testPayment(
                        id = 1L,
                        startDate = start,
                        frequency = RecurrenceFrequency.MONTHLY,
                        interval = 3,
                    ),
                )
            assertThat(result.isSuccess).isTrue()
            // Quarterly anchored on the 5th: Aug 5, Nov 5...; first on/after Oct 5 is Nov 5.
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        }

    @Test
    fun `edit with new startDate in the past fails`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            val result =
                saveRecurringPaymentUseCase(testPayment(id = 1L, startDate = today.minusDays(1)))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.RecurringDateInPast)
        }

    @Test
    fun `edit with new startDate beyond one year fails`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            val result =
                saveRecurringPaymentUseCase(testPayment(id = 1L, startDate = today.plusYears(1).plusDays(1)))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `edit rejects endDate before recalculated nextDueDate`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            val result =
                saveRecurringPaymentUseCase(
                    testPayment(
                        id = 1L,
                        startDate = today,
                        frequency = RecurrenceFrequency.MONTHLY,
                        endDate = today,
                    ),
                )
            // Monthly from today: nextDueDate stays today, endDate == today is fine.
            assertThat(result.isSuccess).isTrue()
            val weekly =
                saveRecurringPaymentUseCase(
                    testPayment(
                        id = 1L,
                        startDate = today.plusDays(2),
                        frequency = RecurrenceFrequency.WEEKLY,
                        endDate = today.plusDays(1),
                    ),
                )
            assertThat(weekly.errorOrNull()).isEqualTo(DomainError.RecurringEndBeforeStart)
        }

    @Test
    fun `edit keeps createdAt and refreshes updatedAt`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today)))
            timeProvider.setTime(Instant.parse("2026-10-06T12:00:00Z"))
            val result = saveRecurringPaymentUseCase(testPayment(id = 1L, startDate = today, name = "Renamed"))
            assertThat(result.isSuccess).isTrue()
            val saved = recurringRepository.get(1L)
            assertThat(saved?.createdAt).isEqualTo(Instant.parse("2026-09-28T12:00:00Z"))
            assertThat(saved?.updatedAt).isEqualTo(Instant.parse("2026-10-06T12:00:00Z"))
        }

    @Test
    fun `edit of missing payment fails`() =
        runTest {
            assertThat(saveRecurringPaymentUseCase(testPayment(id = 99L)).errorOrNull())
                .isEqualTo(DomainError.NotFound)
        }
}
