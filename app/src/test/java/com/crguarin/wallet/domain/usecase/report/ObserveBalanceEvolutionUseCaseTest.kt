package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.report.BalanceDeltas
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceGranularity
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class ObserveBalanceEvolutionUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var useCase: ObserveBalanceEvolutionUseCase

    private val today = LocalDate.of(2026, 9, 20)
    private val now = Instant.parse("2026-09-20T10:00:00Z")

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        accountRepository = FakeAccountRepository()
        useCase = ObserveBalanceEvolutionUseCase(reportRepository, accountRepository)
    }

    @Test
    fun `past month returns one point per day`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L), account(2L, 2000L)))
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money(100L),
                    daily =
                        mapOf(
                            LocalDate.of(2026, 8, 1) to Money(50L),
                            LocalDate.of(2026, 8, 15) to Money(-20L),
                        ),
                )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 8)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.granularity).isEqualTo(BalanceGranularity.DAILY)
                assertThat(evolution.opening).isEqualTo(Money(3100L))
                assertThat(evolution.points).hasSize(31)
                assertThat(evolution.points.first().date).isEqualTo(LocalDate.of(2026, 8, 1))
                assertThat(evolution.points.first().balance).isEqualTo(Money(3150L))
                assertThat(evolution.points.last().date).isEqualTo(LocalDate.of(2026, 8, 31))
                assertThat(evolution.points.last().balance).isEqualTo(Money(3130L))
                assertThat(evolution.closing).isEqualTo(Money(3130L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(1L, 2L), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
                )
        }

    @Test
    fun `current month returns points up to today`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L)))
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money.ZERO,
                    daily = mapOf(LocalDate.of(2026, 9, 10) to Money(200L)),
                )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.granularity).isEqualTo(BalanceGranularity.DAILY)
                assertThat(evolution.points).hasSize(20)
                assertThat(evolution.points.first().date).isEqualTo(LocalDate.of(2026, 9, 1))
                assertThat(evolution.points.first().balance).isEqualTo(Money(1000L))
                assertThat(evolution.points.last().date).isEqualTo(today)
                assertThat(evolution.points.last().balance).isEqualTo(Money(1200L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(1L), LocalDate.of(2026, 9, 1), today),
                )
        }

    @Test
    fun `current year returns one point per elapsed month with the last dated today`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L)))
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money.ZERO,
                    daily =
                        mapOf(
                            LocalDate.of(2026, 1, 15) to Money(100L),
                            LocalDate.of(2026, 9, 10) to Money(50L),
                        ),
                )

            useCase(ReportPeriod.Year(Year.of(2026)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.granularity).isEqualTo(BalanceGranularity.MONTHLY)
                assertThat(evolution.points.map { it.date })
                    .containsExactly(
                        LocalDate.of(2026, 1, 31),
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 5, 31),
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 7, 31),
                        LocalDate.of(2026, 8, 31),
                        today,
                    ).inOrder()
                assertThat(evolution.points.map { it.balance })
                    .containsExactly(
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1100L),
                        Money(1150L),
                    ).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `past year returns twelve month-end points`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L)))

            useCase(ReportPeriod.Year(Year.of(2025)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.granularity).isEqualTo(BalanceGranularity.MONTHLY)
                assertThat(evolution.points).hasSize(12)
                assertThat(evolution.points.first().date).isEqualTo(LocalDate.of(2025, 1, 31))
                assertThat(evolution.points.last().date).isEqualTo(LocalDate.of(2025, 12, 31))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(1L), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                )
        }

    @Test
    fun `account filter queries only that account with its own initial`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L), account(2L, 2000L)))
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money(100L),
                    daily = mapOf(LocalDate.of(2026, 9, 5) to Money(50L)),
                )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = 2L, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.opening).isEqualTo(Money(2100L))
                assertThat(evolution.points.first().balance).isEqualTo(Money(2100L))
                assertThat(evolution.points.last().balance).isEqualTo(Money(2150L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(2L), LocalDate.of(2026, 9, 1), today),
                )
        }

    @Test
    fun `without a filter archived and out-of-total accounts are excluded`() =
        runTest {
            accountRepository.setInitialData(
                listOf(
                    account(1L, 1000L),
                    account(3L, 5000L, includeInTotal = false),
                    account(4L, 3000L, isArchived = true),
                ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.opening).isEqualTo(Money(1000L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(1L), LocalDate.of(2026, 9, 1), today),
                )
        }

    @Test
    fun `with a filter an archived account is included`() =
        runTest {
            accountRepository.setInitialData(
                listOf(
                    account(1L, 1000L),
                    account(4L, 3000L, isArchived = true),
                ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = 4L, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.opening).isEqualTo(Money(3000L))
                assertThat(evolution.points).hasSize(20)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests)
                .containsExactly(
                    Triple(setOf(4L), LocalDate.of(2026, 9, 1), today),
                )
        }

    @Test
    fun `without movements the line is flat at the summed initials`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L), account(2L, 2000L)))

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.opening).isEqualTo(Money(3000L))
                assertThat(evolution.points).hasSize(20)
                assertThat(evolution.points.map { it.balance }.toSet())
                    .containsExactly(Money(3000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `supports a negative balance`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 100L)))
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money.ZERO,
                    daily = mapOf(LocalDate.of(2026, 9, 1) to Money(-500L)),
                )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.opening).isEqualTo(Money(100L))
                assertThat(evolution.points.first().balance).isEqualTo(Money(-400L))
                assertThat(evolution.closing).isEqualTo(Money(-400L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `without accounts returns empty with the period granularity`() =
        runTest {
            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                assertThat(awaitItem()).isEqualTo(BalanceEvolution.EMPTY)
                cancelAndIgnoreRemainingEvents()
            }
            useCase(ReportPeriod.Year(Year.of(2026)), accountId = null, today = today).test {
                assertThat(awaitItem())
                    .isEqualTo(BalanceEvolution(BalanceGranularity.MONTHLY, Money.ZERO, emptyList()))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests).isEmpty()
        }

    @Test
    fun `with a missing filtered account returns empty with the period granularity`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L)))

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = 99L, today = today).test {
                assertThat(awaitItem()).isEqualTo(BalanceEvolution.EMPTY)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests).isEmpty()
        }

    @Test
    fun `when the period starts after today returns empty with the period granularity`() =
        runTest {
            accountRepository.setInitialData(listOf(account(1L, 1000L)))

            useCase(ReportPeriod.Month(YearMonth.of(2026, 10)), accountId = null, today = today).test {
                assertThat(awaitItem()).isEqualTo(BalanceEvolution.EMPTY)
                cancelAndIgnoreRemainingEvents()
            }
            useCase(ReportPeriod.Year(Year.of(2027)), accountId = null, today = today).test {
                assertThat(awaitItem())
                    .isEqualTo(BalanceEvolution(BalanceGranularity.MONTHLY, Money.ZERO, emptyList()))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.balanceRequests).isEmpty()
        }

    @Test
    fun `closing matches the total balance when there are no future transactions`() =
        runTest {
            val a1 = account(1L, 1000L)
            val a2 = account(2L, 2000L)
            accountRepository.setInitialData(
                listOf(a1, a2),
                balanceMap = mapOf(1L to Money(1500L), 2L to Money(2500L)),
            )
            // Deltas explain the whole difference between initials and balances.
            reportRepository.balanceDeltas =
                BalanceDeltas(
                    openingDelta = Money(300L),
                    daily = mapOf(LocalDate.of(2026, 9, 5) to Money(700L)),
                )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val evolution = awaitItem()

                assertThat(evolution.closing).isEqualTo(Money(4000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `balance-only updates do not requery but initial changes do`() =
        runTest {
            val a1 = account(1L, 1000L)
            accountRepository.setInitialData(listOf(a1))

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                assertThat(awaitItem().opening).isEqualTo(Money(1000L))

                accountRepository.setBalance(1L, Money(9999L))
                expectNoEvents()
                assertThat(reportRepository.balanceRequests).hasSize(1)

                accountRepository.saveAccount(a1.copy(initialBalance = Money(2000L)))
                assertThat(awaitItem().opening).isEqualTo(Money(2000L))
                assertThat(reportRepository.balanceRequests).hasSize(2)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun account(
        id: Long,
        initial: Long,
        includeInTotal: Boolean = true,
        isArchived: Boolean = false,
    ): Account =
        Account(
            id = id,
            name = "Account $id",
            type = AccountType.CASH,
            initialBalance = Money(initial),
            color = 1,
            icon = "wallet",
            includeInTotal = includeInTotal,
            isArchived = isArchived,
            createdAt = now,
        )
}
