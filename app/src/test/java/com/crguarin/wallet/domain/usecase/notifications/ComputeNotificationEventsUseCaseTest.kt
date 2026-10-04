package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCardAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ComputeNotificationEventsUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var computeUseCase: ComputeNotificationEventsUseCase

    private val today = LocalDate.of(2026, 10, 7)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        val cardStatementRepository = FakeCardStatementRepository()
        val timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-07T12:00:00Z"))
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
        val observeCardOverview = ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider)
        computeUseCase =
            ComputeNotificationEventsUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                ObserveCardOverviewsUseCase(accountRepository, observeCardOverview),
            )
    }

    private suspend fun compute(
        lastRun: LocalDate?,
        day: LocalDate = today,
        settings: NotificationSettings = NotificationSettings(),
    ): List<NotificationEvent> = computeUseCase(lastRun, day, settings)

    @Test
    fun `reminder fires on its trigger day only`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            val onDay = compute(today.minusDays(1), today, settings).filterIsInstance<NotificationEvent.RecurringUpcoming>()
            assertThat(onDay).hasSize(1)
            assertThat(onDay.single().date).isEqualTo(LocalDate.of(2026, 10, 10))
            assertThat(onDay.single().daysUntil).isEqualTo(3)

            val dayBefore = compute(today.minusDays(2), today.minusDays(1), settings)
            assertThat(dayBefore).isEmpty()

            val dayAfter = compute(today, today.plusDays(1), settings)
            assertThat(dayAfter).isEmpty()
        }

    @Test
    fun `two day gap recovers but ten day gap only recovers recent`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            // Trigger was 2026-10-07 (3 days before the charge).
            val afterTwoDays = compute(today.minusDays(2), today.plusDays(2), settings)
            assertThat(afterTwoDays).hasSize(1)

            val afterTenDays = compute(today.minusDays(10), today.plusDays(7), settings)
            assertThat(afterTenDays).isEmpty()
        }

    @Test
    fun `null lastRun only fires today`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                    ),
                    testPayment(
                        id = 2L,
                        name = "Old",
                        startDate = LocalDate.of(2026, 10, 9),
                        nextDueDate = LocalDate.of(2026, 10, 9),
                        reminderDaysBefore = 3,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            val events = compute(null, today, settings).filterIsInstance<NotificationEvent.RecurringUpcoming>()
            assertThat(events.map { it.paymentId }).containsExactly(1L)
        }

    @Test
    fun `same day repeat notifies nothing`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            assertThat(compute(today, today, settings)).isEmpty()
        }

    @Test
    fun `switches silence their events`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                        mode = RecurringMode.CONFIRM,
                    ),
                ),
            )
            val off =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardClosing = false,
                    cardPayment = false,
                    cardOverdue = false,
                )
            assertThat(compute(today.minusDays(1), today, off)).isEmpty()
        }

    @Test
    fun `confirm payment fires reminder before and pending on the day`() =
        runTest {
            val due = LocalDate.of(2026, 10, 7)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = due,
                        nextDueDate = due,
                        frequency = RecurrenceFrequency.WEEKLY,
                        mode = RecurringMode.CONFIRM,
                        reminderDaysBefore = 2,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            // Reminder trigger was 2026-10-05; recovered within the window.
            val reminders =
                compute(today.minusDays(3), today, settings).filterIsInstance<NotificationEvent.RecurringUpcoming>()
            assertThat(reminders).hasSize(1)
            val pending =
                compute(today.minusDays(1), today, settings).filterIsInstance<NotificationEvent.RecurringPending>()
            assertThat(pending).hasSize(1)
            assertThat(pending.single().date).isEqualTo(due)
        }

    @Test
    fun `paused or blocked payments stay silent`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                        isActive = false,
                    ),
                    testPayment(
                        id = 2L,
                        name = "Blocked",
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                        accountId = 2L,
                    ),
                ),
            )
            accountRepository.setInitialData(
                listOf(testAccount(), testAccount(id = 2L, name = "Archived", isArchived = true), testCardAccount()),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            assertThat(compute(today.minusDays(1), today, settings)).isEmpty()
        }

    @Test
    fun `card closing fires on the trigger day only`() =
        runTest {
            // Next closing is 2026-10-15; with 2 days before, the trigger is 2026-10-13.
            val settings =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardPayment = false,
                    cardOverdue = false,
                    cardClosingDaysBefore = 2,
                )
            val triggerDay = LocalDate.of(2026, 10, 13)
            val events =
                compute(triggerDay.minusDays(1), triggerDay, settings)
                    .filterIsInstance<NotificationEvent.CardClosing>()
            assertThat(events).hasSize(1)
            assertThat(events.single().closingDate).isEqualTo(LocalDate.of(2026, 10, 15))
            assertThat(events.single().daysUntil).isEqualTo(2)

            val dayBefore =
                compute(triggerDay.minusDays(2), triggerDay.minusDays(1), settings)
                    .filterIsInstance<NotificationEvent.CardClosing>()
            assertThat(dayBefore).isEmpty()
            val dayAfter =
                compute(triggerDay, triggerDay.plusDays(1), settings)
                    .filterIsInstance<NotificationEvent.CardClosing>()
            assertThat(dayAfter).isEmpty()
        }

    @Test
    fun `card closing with daysBefore zero fires on the closing day`() =
        runTest {
            val settings =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardPayment = false,
                    cardOverdue = false,
                    cardClosingDaysBefore = 0,
                )
            val closingDay = LocalDate.of(2026, 10, 15)
            val events =
                compute(closingDay.minusDays(1), closingDay, settings)
                    .filterIsInstance<NotificationEvent.CardClosing>()
            assertThat(events).hasSize(1)
            assertThat(events.single().daysUntil).isEqualTo(0)

            val dayAfter =
                compute(closingDay, closingDay.plusDays(1), settings)
                    .filterIsInstance<NotificationEvent.CardClosing>()
            assertThat(dayAfter).isEmpty()
        }

    @Test
    fun `card payment with daysBefore zero fires only on due day`() =
        runTest {
            val settings =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardClosing = false,
                    cardOverdue = false,
                    cardPaymentDaysBefore = 0,
                )
            val dueDay = LocalDate.of(2026, 10, 5)
            val onDue =
                compute(dueDay.minusDays(1), dueDay, settings).filterIsInstance<NotificationEvent.CardPaymentDue>()
            assertThat(onDue).hasSize(1)
            assertThat(onDue.single().amount).isEqualTo(Money(450_000L))
            val before =
                compute(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), settings)
                    .filterIsInstance<NotificationEvent.CardPaymentDue>()
            assertThat(before).isEmpty()
        }

    @Test
    fun `card payment fires before and on due day by default`() =
        runTest {
            val settings =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardClosing = false,
                    cardOverdue = false,
                )
            // Due 2026-10-05 with 3 days before: triggers 10-02 and 10-05.
            val early =
                compute(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), settings)
                    .filterIsInstance<NotificationEvent.CardPaymentDue>()
            assertThat(early).hasSize(1)
            assertThat(early.single().daysUntil).isEqualTo(3)
            val onDue =
                compute(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5), settings)
                    .filterIsInstance<NotificationEvent.CardPaymentDue>()
            assertThat(onDue).hasSize(1)
            assertThat(onDue.single().daysUntil).isEqualTo(0)
        }

    @Test
    fun `overdue card fires the day after the due date`() =
        runTest {
            val settings =
                NotificationSettings(
                    recurringReminders = false,
                    recurringPendingConfirm = false,
                    cardClosing = false,
                    cardPayment = false,
                )
            val overdueDay = LocalDate.of(2026, 10, 6)
            val events =
                compute(overdueDay.minusDays(1), overdueDay, settings)
                    .filterIsInstance<NotificationEvent.CardOverdue>()
            assertThat(events).hasSize(1)
            assertThat(events.single().dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
            assertThat(events.single().amount).isEqualTo(Money(450_000L))
        }

    @Test
    fun `keys are stable and unique and posted never appears`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        reminderDaysBefore = 3,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardOverdue = false)
            val first = compute(today.minusDays(1), today, settings)
            val second = compute(today.minusDays(1), today, settings)
            assertThat(first.map { it.key }).containsNoDuplicates()
            assertThat(second.map { it.key }).containsExactlyElementsIn(first.map { it.key })
            assertThat(first.none { it is NotificationEvent.RecurringPosted }).isTrue()
        }

    @Test
    fun `no reminder events past endDate`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 10, 5),
                        nextDueDate = LocalDate.of(2026, 10, 5),
                        frequency = RecurrenceFrequency.WEEKLY,
                        endDate = LocalDate.of(2026, 10, 8),
                        reminderDaysBefore = 0,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            // Oct 12 would be the next weekly occurrence, but the rule ended Oct 8.
            val events =
                compute(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12), settings)
                    .filterIsInstance<NotificationEvent.RecurringUpcoming>()
            assertThat(events).isEmpty()
        }

    @Test
    fun `no reminder for an occurrence already recorded`() =
        runTest {
            // Today's charge already ran, so nextDueDate moved to next week: only that one is reminded.
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = today,
                        nextDueDate = today.plusDays(7),
                        frequency = RecurrenceFrequency.WEEKLY,
                        reminderDaysBefore = 0,
                    ),
                ),
            )
            val settings = NotificationSettings(cardClosing = false, cardPayment = false, cardOverdue = false)
            val events =
                compute(today.minusDays(1), today, settings)
                    .filterIsInstance<NotificationEvent.RecurringUpcoming>()
            assertThat(events).isEmpty()
        }
}
