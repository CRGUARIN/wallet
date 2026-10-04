package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeDailyWorkScheduler
import com.crguarin.wallet.domain.fake.FakeNotificationGateway
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCardAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RunDailyChecksUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase
    private lateinit var computeUseCase: ComputeNotificationEventsUseCase
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var gateway: FakeNotificationGateway
    private lateinit var runDailyChecksUseCase: RunDailyChecksUseCase
    private lateinit var onAppStartUseCase: OnAppStartUseCase
    private lateinit var scheduler: FakeDailyWorkScheduler

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        val transactionRepository = FakeTransactionRepository()
        val cardStatementRepository = FakeCardStatementRepository()
        timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        preferencesRepository = FakePreferencesRepository()
        gateway = FakeNotificationGateway()
        scheduler = FakeDailyWorkScheduler()
        accountRepository.setInitialData(
            listOf(testAccount(), testCardAccount()),
            balanceMap = mapOf(1L to Money(1_000_000L), 3L to Money(-450_000L)),
        )
        categoryRepository.setInitialData(listOf(testCategory()))
        cardStatementRepository.setInitialData(
            listOf(
                CardStatement(
                    id = 1L,
                    accountId = 3L,
                    closingDate = LocalDate.of(2026, 9, 15),
                    dueDate = LocalDate.of(2026, 10, 5),
                    statementBalance = Money(450_000L),
                    minimumPayment = null,
                    statedInterestAndFees = null,
                    adjustmentTransactionId = null,
                    createdAt = Instant.parse("2026-09-28T12:00:00Z"),
                ),
            ),
        )
        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository,
                accountRepository,
                categoryRepository,
                timeProvider,
                CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
            )
        val processDue =
            ProcessDueRecurringUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                saveTransactionUseCase,
                timeProvider,
            )
        val observeCardOverview = ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider)
        computeUseCase =
            ComputeNotificationEventsUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                ObserveCardOverviewsUseCase(accountRepository, observeCardOverview),
                timeProvider,
            )
        runDailyChecksUseCase = RunDailyChecksUseCase(processDue, preferencesRepository, computeUseCase, gateway, timeProvider)
        onAppStartUseCase = OnAppStartUseCase(processDue, preferencesRepository, scheduler)
    }

    @Test
    fun `daily check processes shows posted and updates last run`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            preferencesRepository.setLastNotificationRunDate(today.minusDays(1))

            runDailyChecksUseCase()

            val posted = gateway.shownEvents.filterIsInstance<NotificationEvent.RecurringPosted>()
            assertThat(posted).hasSize(1)
            assertThat(
                posted
                    .single()
                    .occurrences
                    .single()
                    .paymentId,
            ).isEqualTo(1L)
            assertThat(preferencesRepository.getLastNotificationRunDate()).isEqualTo(today)
        }

    @Test
    fun `more than three charges group into one notification`() =
        runTest {
            val payments =
                (1..4).map { index ->
                    val id = index.toLong()
                    testPayment(id = id, name = "Sub $id", startDate = today, nextDueDate = today)
                }
            recurringRepository.setInitialData(payments)
            preferencesRepository.setLastNotificationRunDate(today.minusDays(1))

            runDailyChecksUseCase()

            val posted = gateway.shownEvents.filterIsInstance<NotificationEvent.RecurringPosted>()
            assertThat(posted).hasSize(1)
            assertThat(posted.single().occurrences).hasSize(4)
        }

    @Test
    fun `disabled notifications update last run silently`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            preferencesRepository.updateNotificationSettings { it.copy(enabled = false) }

            runDailyChecksUseCase()

            // The charge is still recorded: processing ignores the switch.
            assertThat(recurringRepository.completedTransactions).hasSize(1)
            assertThat(gateway.shownBatches).isEmpty()
            assertThat(preferencesRepository.getLastNotificationRunDate()).isEqualTo(today)
        }

    @Test
    fun `process failure still notifies cards and updates last run`() =
        runTest {
            val throwingRecurring =
                object : RecurringRepository by recurringRepository {
                    override suspend fun getAll(): List<RecurringPayment> = throw IllegalStateException("db down")
                }
            val failingProcess =
                ProcessDueRecurringUseCase(
                    throwingRecurring,
                    accountRepository,
                    categoryRepository,
                    saveTransactionUseCase,
                    timeProvider,
                )
            val useCase = RunDailyChecksUseCase(failingProcess, preferencesRepository, computeUseCase, gateway, timeProvider)

            useCase()

            // The card payment due today is still notified despite the step-1 failure.
            assertThat(gateway.shownEvents.filterIsInstance<NotificationEvent.CardPaymentDue>()).hasSize(1)
            assertThat(preferencesRepository.getLastNotificationRunDate()).isEqualTo(today)
        }

    @Test
    fun `on app start processes without notifying and keeps the schedule`() =
        runTest {
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))
            preferencesRepository.updateNotificationSettings { it.copy(hour = 9, minute = 30) }

            onAppStartUseCase()

            assertThat(recurringRepository.completedTransactions).hasSize(1)
            assertThat(gateway.shownBatches).isEmpty()
            assertThat(scheduler.calls.single()).isEqualTo(
                FakeDailyWorkScheduler.ScheduleCall(hour = 9, minute = 30, replace = false),
            )
            // App start never touches the notification run date.
            assertThat(preferencesRepository.getLastNotificationRunDate()).isNull()
        }
}
