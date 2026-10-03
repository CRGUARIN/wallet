package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class ObserveBalanceSummaryUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var useCase: ObserveBalanceSummaryUseCase

    private val today = LocalDate.of(2026, 9, 20)

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        useCase = ObserveBalanceSummaryUseCase(reportRepository)
    }

    @Test
    fun `sums the current and the previous month separately`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 1, income = 1000L, expense = 300L),
                        daily(2026, 9, 10, income = 500L, expense = 100L),
                        daily(2026, 8, 5, income = 800L, expense = 200L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.income).isEqualTo(Money(1500L))
                assertThat(summary.expense).isEqualTo(Money(400L))
                assertThat(summary.net).isEqualTo(Money(1100L))
                assertThat(summary.savingsRatePerMille).isEqualTo(733)
                assertThat(summary.previousNet).isEqualTo(Money(600L))
                assertThat(summary.netChange).isEqualTo(Money(500L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyRangeRequests)
                .containsExactly(LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 9, 30))
                .inOrder()
        }

    @Test
    fun `previous net and net change are null when the previous period is empty`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense = listOf(daily(2026, 9, 2, income = 1000L, expense = 300L)),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.net).isEqualTo(Money(700L))
                assertThat(summary.previousNet).isNull()
                assertThat(summary.netChange).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `supports a negative net with a negative savings rate`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 2, income = 1000L, expense = 1500L),
                        daily(2026, 8, 2, income = 1000L, expense = 500L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.net).isEqualTo(Money(-500L))
                assertThat(summary.savingsRatePerMille).isEqualTo(-500)
                assertThat(summary.previousNet).isEqualTo(Money(500L))
                assertThat(summary.netChange).isEqualTo(Money(-1000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `savings rate is null when there is no income`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 2, expense = 300L),
                        daily(2026, 8, 2, expense = 100L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.income).isEqualTo(Money.ZERO)
                assertThat(summary.net).isEqualTo(Money(-300L))
                assertThat(summary.savingsRatePerMille).isNull()
                assertThat(summary.previousNet).isEqualTo(Money(-100L))
                assertThat(summary.netChange).isEqualTo(Money(-200L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `forwards the accountId to the repository`() =
        runTest {
            useCase(
                ReportPeriod.Month(YearMonth.of(2026, 9)),
                accountId = 7L,
                today = today,
            ).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyAccountRequests).containsExactly(7L)
        }

    @Test
    fun `supports year mode with a single query over both years`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 1, 15, income = 1200L, expense = 200L),
                        daily(2026, 6, 15, income = 800L, expense = 300L),
                        daily(2025, 12, 31, income = 900L, expense = 100L),
                    ),
            )

            useCase(ReportPeriod.Year(Year.of(2026)), accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.income).isEqualTo(Money(2000L))
                assertThat(summary.expense).isEqualTo(Money(500L))
                assertThat(summary.net).isEqualTo(Money(1500L))
                assertThat(summary.savingsRatePerMille).isEqualTo(750)
                assertThat(summary.previousNet).isEqualTo(Money(800L))
                assertThat(summary.netChange).isEqualTo(Money(700L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyRangeRequests)
                .containsExactly(LocalDate.of(2025, 1, 1) to LocalDate.of(2026, 12, 31))
                .inOrder()
        }

    private fun daily(
        year: Int,
        month: Int,
        day: Int,
        income: Long = 0L,
        expense: Long = 0L,
    ): DailyIncomeExpense =
        DailyIncomeExpense(
            date = LocalDate.of(year, month, day),
            income = Money(income),
            expense = Money(expense),
        )
}
