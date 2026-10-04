package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringBlockReason
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.pendingCount
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveRecurringUseCasesTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var overviewUseCase: ObserveRecurringOverviewUseCase
    private lateinit var paymentUseCase: ObserveRecurringPaymentUseCase

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        val timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        accountRepository.setInitialData(
            listOf(
                testAccount(),
                testAccount(id = 2L, name = "Archived", isArchived = true),
            ),
        )
        categoryRepository.setInitialData(listOf(testCategory()))
        overviewUseCase =
            ObserveRecurringOverviewUseCase(recurringRepository, accountRepository, categoryRepository, timeProvider)
        paymentUseCase =
            ObserveRecurringPaymentUseCase(recurringRepository, accountRepository, categoryRepository, timeProvider)
    }

    @Test
    fun `overview splits sections orders them and totals billable`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        name = "Netflix",
                        startDate = LocalDate.of(2026, 9, 5),
                        nextDueDate = LocalDate.of(2026, 10, 5),
                        mode = RecurringMode.CONFIRM,
                    ),
                    testPayment(
                        id = 2L,
                        name = "Gym",
                        amount = Money(100_000L),
                        startDate = LocalDate.of(2026, 9, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                    ),
                    testPayment(
                        id = 5L,
                        name = "Rent",
                        amount = Money(50_000L),
                        startDate = LocalDate.of(2026, 9, 1),
                        nextDueDate = LocalDate.of(2026, 10, 1),
                    ),
                    testPayment(
                        id = 3L,
                        name = "Old",
                        startDate = LocalDate.of(2026, 9, 10),
                        nextDueDate = LocalDate.of(2026, 9, 10),
                        isActive = false,
                    ),
                    testPayment(
                        id = 6L,
                        name = "Alpha",
                        startDate = LocalDate.of(2026, 9, 10),
                        nextDueDate = LocalDate.of(2026, 9, 10),
                        isActive = false,
                    ),
                    testPayment(
                        id = 4L,
                        name = "Blocked",
                        startDate = LocalDate.of(2026, 9, 10),
                        nextDueDate = LocalDate.of(2026, 10, 10),
                        accountId = 2L,
                    ),
                ),
            )
            val overview = overviewUseCase().first()
            assertThat(overview.pending.map { it.payment.name }).containsExactly("Netflix")
            assertThat(overview.active.map { it.payment.name }).containsExactly("Rent", "Gym", "Blocked").inOrder()
            assertThat(overview.inactive.map { it.payment.name }).containsExactly("Alpha", "Old").inOrder()
            val blocked = overview.active.first { it.payment.name == "Blocked" }
            assertThat(blocked.blockReason).isEqualTo(RecurringBlockReason.ACCOUNT_ARCHIVED)
            // Totals count every active, unfinished and unblocked payment,
            // including the pending CONFIRM one: 50_000 + 100_000 + 45_900.
            assertThat(overview.monthlyExpense).isEqualTo(Money(195_900L))
            assertThat(overview.monthlyIncome).isEqualTo(Money.ZERO)
        }

    @Test
    fun `pending counts every occurrence up to today`() =
        runTest {
            val due = LocalDate.of(2026, 9, 21)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = due,
                        nextDueDate = due,
                        frequency = RecurrenceFrequency.WEEKLY,
                        mode = RecurringMode.CONFIRM,
                    ),
                ),
            )
            val overview = overviewUseCase().first()
            // 09-21, 09-28 and 10-05 are all due.
            assertThat(overview.pending.single().pendingCount).isEqualTo(3)
        }

    @Test
    fun `pendingCount ignores occurrences past endDate`() {
        val payment =
            testPayment(
                startDate = LocalDate.of(2026, 10, 1),
                nextDueDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2026, 10, 5),
                frequency = RecurrenceFrequency.WEEKLY,
                mode = RecurringMode.CONFIRM,
            )
        // Weekly from Oct 1: Oct 1 and Oct 8 are due by Oct 10, but Oct 8 is past the end date.
        assertThat(payment.pendingCount(LocalDate.of(2026, 10, 10))).isEqualTo(1)
    }

    @Test
    fun `auto payments never report pending`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = LocalDate.of(2026, 9, 5), nextDueDate = LocalDate.of(2026, 9, 5))),
            )
            val overview = overviewUseCase().first()
            assertThat(overview.pending).isEmpty()
            assertThat(overview.active.single().pendingCount).isEqualTo(0)
        }

    @Test
    fun `single payment exposes pendingCount blockReason and monthlyEquivalent`() =
        runTest {
            val due = LocalDate.of(2026, 10, 3)
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        amount = Money(1_200_000L),
                        frequency = RecurrenceFrequency.YEARLY,
                        startDate = due,
                        nextDueDate = due,
                        mode = RecurringMode.CONFIRM,
                        reminderDaysBefore = 2,
                    ),
                ),
            )
            val item = paymentUseCase(1L).first()
            assertThat(item?.pendingCount).isEqualTo(1)
            assertThat(item?.blockReason).isNull()
            assertThat(item?.monthlyEquivalent).isEqualTo(Money(100_000L))
            assertThat(item?.accountName).isEqualTo("Ahorros")
        }

    @Test
    fun `single payment of missing id emits null`() =
        runTest {
            assertThat(paymentUseCase(99L).first()).isNull()
        }

    @Test
    fun `income payments total as monthly income`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        name = "Salary",
                        type = TransactionType.INCOME,
                        amount = Money(6_000_000L),
                        categoryId = 1L,
                        frequency = RecurrenceFrequency.MONTHLY,
                        startDate = today,
                        nextDueDate = today,
                    ),
                ),
            )
            // Category 1 is EXPENSE while the payment is INCOME, but the overview
            // does not revalidate: it only reports the type mismatch as data.
            val overview = overviewUseCase().first()
            assertThat(overview.monthlyIncome).isEqualTo(Money(6_000_000L))
            assertThat(overview.monthlyExpense).isEqualTo(Money.ZERO)
        }
}
