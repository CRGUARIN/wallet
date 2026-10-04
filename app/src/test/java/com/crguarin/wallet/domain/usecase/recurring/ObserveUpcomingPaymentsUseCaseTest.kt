package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.UpcomingKind
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCardAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveUpcomingPaymentsUseCaseTest {
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var upcomingUseCase: ObserveUpcomingPaymentsUseCase

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        recurringRepository = FakeRecurringRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        categoryRepository.setInitialData(listOf(testCategory()))
        val cardStatementRepository = FakeCardStatementRepository()
        val timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z"))
        val card = testCardAccount()
        accountRepository.setInitialData(
            listOf(testAccount(), card),
            balanceMap = mapOf(1L to Money(1_000_000L), 3L to Money(-450_000L)),
        )
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
        upcomingUseCase =
            ObserveUpcomingPaymentsUseCase(
                recurringRepository,
                accountRepository,
                categoryRepository,
                ObserveCardOverviewsUseCase(accountRepository, observeCardOverview),
                timeProvider,
            )
    }

    @Test
    fun `recurring occurrences and card payment come sorted by date`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        name = "Netflix",
                        startDate = LocalDate.of(2026, 10, 6),
                        nextDueDate = LocalDate.of(2026, 10, 6),
                    ),
                    testPayment(
                        id = 2L,
                        name = "Gym",
                        startDate = LocalDate.of(2026, 10, 1),
                        nextDueDate = LocalDate.of(2026, 10, 1),
                        frequency = RecurrenceFrequency.WEEKLY,
                    ),
                ),
            )
            val rows = upcomingUseCase(7).first()
            assertThat(rows.map { it.date }).containsExactly(
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 8),
            )
            val card = rows.first { it.kind == UpcomingKind.CARD_PAYMENT }
            assertThat(card.refId).isEqualTo(3L)
            assertThat(card.amount).isEqualTo(Money(450_000L))
        }

    @Test
    fun `pending confirm shows one row instead of its occurrences`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = LocalDate.of(2026, 9, 21),
                        nextDueDate = LocalDate.of(2026, 9, 21),
                        frequency = RecurrenceFrequency.WEEKLY,
                        mode = RecurringMode.CONFIRM,
                    ),
                ),
            )
            val rows = upcomingUseCase(7).first()
            val pending = rows.filter { it.kind == UpcomingKind.RECURRING_PENDING }
            assertThat(pending).hasSize(1)
            assertThat(pending.single().date).isEqualTo(LocalDate.of(2026, 9, 21))
            assertThat(rows.none { it.kind == UpcomingKind.RECURRING }).isTrue()
        }

    @Test
    fun `rows are capped at five`() =
        runTest {
            val payments =
                (1..6).map { index ->
                    val id = index.toLong()
                    testPayment(
                        id = id,
                        name = "Sub $id",
                        startDate = today.plusDays(id),
                        nextDueDate = today.plusDays(id),
                        frequency = RecurrenceFrequency.YEARLY,
                    )
                }
            recurringRepository.setInitialData(payments)
            val rows = upcomingUseCase(30).first()
            assertThat(rows).hasSize(5)
            assertThat(rows.map { it.date }).isInOrder()
        }

    @Test
    fun `occurrences outside the window are excluded`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(testPayment(id = 1L, startDate = today.plusDays(20), nextDueDate = today.plusDays(20))),
            )
            val rows = upcomingUseCase(7).first()
            assertThat(rows.none { it.kind == UpcomingKind.RECURRING }).isTrue()
        }

    @Test
    fun `payment with archived category is excluded`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(testCategory(), testCategory(id = 2L, name = "Old", isArchived = true)),
            )
            recurringRepository.setInitialData(
                listOf(
                    testPayment(id = 1L, startDate = today, nextDueDate = today, categoryId = 2L),
                    testPayment(
                        id = 2L,
                        name = "Old sub",
                        startDate = LocalDate.of(2026, 9, 21),
                        nextDueDate = LocalDate.of(2026, 9, 21),
                        frequency = RecurrenceFrequency.WEEKLY,
                        mode = RecurringMode.CONFIRM,
                        categoryId = 2L,
                    ),
                ),
            )
            val rows = upcomingUseCase(7).first()
            assertThat(rows.none { it.kind == UpcomingKind.RECURRING }).isTrue()
            assertThat(rows.none { it.kind == UpcomingKind.RECURRING_PENDING }).isTrue()
        }

    @Test
    fun `occurrences past endDate are excluded`() =
        runTest {
            recurringRepository.setInitialData(
                listOf(
                    testPayment(
                        id = 1L,
                        startDate = today,
                        nextDueDate = today,
                        frequency = RecurrenceFrequency.WEEKLY,
                        endDate = today.plusDays(3),
                    ),
                ),
            )
            val rows = upcomingUseCase(30).first()
            val recurring = rows.filter { it.kind == UpcomingKind.RECURRING }
            assertThat(recurring.map { it.date }).containsExactly(today)
        }
}
