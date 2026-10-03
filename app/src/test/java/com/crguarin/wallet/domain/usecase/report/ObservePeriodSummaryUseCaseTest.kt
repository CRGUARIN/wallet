package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class ObservePeriodSummaryUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var useCase: ObservePeriodSummaryUseCase

    private val today = LocalDate.of(2026, 9, 20)

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        useCase = ObservePeriodSummaryUseCase(reportRepository)
    }

    @Test
    fun `averages the current month over the elapsed days`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 1, expense = 300L),
                        daily(2026, 9, 10, expense = 500L),
                        daily(2026, 8, 5, expense = 400L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), TransactionType.EXPENSE, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(800L))
                assertThat(summary.previousTotal).isEqualTo(Money(400L))
                assertThat(summary.changePercent).isEqualTo(100)
                assertThat(summary.average).isEqualTo(Money(40L))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyRangeRequests)
                .containsExactly(LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 9, 30))
                .inOrder()
        }

    @Test
    fun `averages a past month over all its days`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 7, 1, expense = 310L),
                        daily(2026, 7, 31, expense = 310L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 7)), TransactionType.EXPENSE, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(620L))
                assertThat(summary.average).isEqualTo(Money(20L))
                assertThat(summary.previousTotal).isEqualTo(Money.ZERO)
                assertThat(summary.changePercent).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `averages the current year over the elapsed months`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 1, 15, income = 900L),
                        daily(2025, 12, 31, income = 300L),
                    ),
            )

            useCase(ReportPeriod.Year(Year.of(2026)), TransactionType.INCOME, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(900L))
                assertThat(summary.average).isEqualTo(Money(100L))
                assertThat(summary.previousTotal).isEqualTo(Money(300L))
                assertThat(summary.changePercent).isEqualTo(200)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyRangeRequests)
                .containsExactly(LocalDate.of(2025, 1, 1) to LocalDate.of(2026, 12, 31))
                .inOrder()
        }

    @Test
    fun `averages a past year over twelve months`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2025, 1, 15, income = 1_200L),
                        daily(2024, 6, 15, income = 240L),
                    ),
            )

            useCase(ReportPeriod.Year(Year.of(2025)), TransactionType.INCOME, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(1_200L))
                assertThat(summary.average).isEqualTo(Money(100L))
                assertThat(summary.previousTotal).isEqualTo(Money(240L))
                assertThat(summary.changePercent).isEqualTo(400)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sums each period separately without mixing boundary days`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 8, 31, expense = 400L),
                        daily(2026, 9, 1, expense = 100L),
                        daily(2026, 9, 30, expense = 700L),
                        daily(2026, 10, 1, expense = 999L),
                    ),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), TransactionType.EXPENSE, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(800L))
                assertThat(summary.previousTotal).isEqualTo(Money(400L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `forwards the accountId to the repository`() =
        runTest {
            useCase(
                ReportPeriod.Month(YearMonth.of(2026, 9)),
                TransactionType.EXPENSE,
                accountId = 7L,
                today = today,
            ).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyAccountRequests).containsExactly(7L)
        }

    @Test
    fun `reports a null change when the previous period is zero`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense = listOf(daily(2026, 9, 2, expense = 50L)),
            )

            useCase(ReportPeriod.Month(YearMonth.of(2026, 9)), TransactionType.EXPENSE, accountId = null, today = today).test {
                val summary = awaitItem()

                assertThat(summary.total).isEqualTo(Money(50L))
                assertThat(summary.previousTotal).isEqualTo(Money.ZERO)
                assertThat(summary.changePercent).isNull()
                assertThat(summary.average).isEqualTo(Money(2L))
                cancelAndIgnoreRemainingEvents()
            }
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
