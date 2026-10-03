package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObserveSpendingPaceUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var useCase: ObserveSpendingPaceUseCase

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        useCase = ObserveSpendingPaceUseCase(reportRepository)
    }

    @Test
    fun `cuts the current month at today and compares with the same day of the previous month`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 1, expense = 10L),
                        daily(2026, 9, 15, expense = 20L),
                        daily(2026, 9, 20, expense = 30L),
                        daily(2026, 8, 1, expense = 5L),
                        daily(2026, 8, 31, expense = 45L),
                    ),
            )

            useCase(
                yearMonth = YearMonth.of(2026, 9),
                type = TransactionType.EXPENSE,
                accountId = null,
                today = LocalDate.of(2026, 9, 15),
            ).test {
                val pace = awaitItem()

                assertThat(pace.yearMonth).isEqualTo(YearMonth.of(2026, 9))
                assertThat(pace.daysInMonth).isEqualTo(30)
                assertThat(pace.comparisonDay).isEqualTo(15)
                assertThat(pace.current).hasSize(15)
                assertThat(pace.current.last()).isEqualTo(Money(30L))
                assertThat(pace.previous).hasSize(31)
                assertThat(pace.currentAtDay).isEqualTo(Money(30L))
                assertThat(pace.previousAtDay).isEqualTo(Money(5L))
                assertThat(pace.changePercent).isEqualTo(500)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(reportRepository.dailyRangeRequests)
                .containsExactly(LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 9, 30))
                .inOrder()
        }

    @Test
    fun `renders a past month complete`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 8, 1, expense = 10L),
                        daily(2026, 8, 31, expense = 20L),
                        daily(2026, 7, 31, expense = 5L),
                    ),
            )

            useCase(
                yearMonth = YearMonth.of(2026, 8),
                type = TransactionType.EXPENSE,
                accountId = null,
                today = LocalDate.of(2026, 9, 15),
            ).test {
                val pace = awaitItem()

                assertThat(pace.daysInMonth).isEqualTo(31)
                assertThat(pace.comparisonDay).isEqualTo(31)
                assertThat(pace.current).hasSize(31)
                assertThat(pace.currentAtDay).isEqualTo(Money(30L))
                assertThat(pace.previousAtDay).isEqualTo(Money(5L))
                assertThat(pace.changePercent).isEqualTo(500)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `compares march 31st against the end of february`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 3, 31, expense = 100L),
                        daily(2026, 2, 28, expense = 200L),
                    ),
            )

            useCase(
                yearMonth = YearMonth.of(2026, 3),
                type = TransactionType.EXPENSE,
                accountId = null,
                today = LocalDate.of(2026, 3, 31),
            ).test {
                val pace = awaitItem()

                assertThat(pace.daysInMonth).isEqualTo(31)
                assertThat(pace.comparisonDay).isEqualTo(31)
                assertThat(pace.previous).hasSize(28)
                assertThat(pace.currentAtDay).isEqualTo(Money(100L))
                assertThat(pace.previousAtDay).isEqualTo(Money(200L))
                assertThat(pace.changePercent).isEqualTo(-50)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `handles a leap february and clamps the previous month to the comparison day`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2028, 2, 29, expense = 40L),
                        daily(2028, 1, 10, expense = 10L),
                        daily(2028, 1, 31, expense = 90L),
                    ),
            )

            useCase(
                yearMonth = YearMonth.of(2028, 2),
                type = TransactionType.EXPENSE,
                accountId = null,
                today = LocalDate.of(2028, 2, 29),
            ).test {
                val pace = awaitItem()

                assertThat(pace.daysInMonth).isEqualTo(29)
                assertThat(pace.comparisonDay).isEqualTo(29)
                assertThat(pace.current).hasSize(29)
                assertThat(pace.currentAtDay).isEqualTo(Money(40L))
                // January has 31 days, but only the first 29 are compared.
                assertThat(pace.previous).hasSize(31)
                assertThat(pace.previousAtDay).isEqualTo(Money(10L))
                assertThat(pace.changePercent).isEqualTo(300)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uses income instead of expense when the type is income`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 9, 1, income = 50L, expense = 999L),
                    ),
            )

            useCase(
                yearMonth = YearMonth.of(2026, 9),
                type = TransactionType.INCOME,
                accountId = null,
                today = LocalDate.of(2026, 9, 1),
            ).test {
                val pace = awaitItem()

                assertThat(pace.currentAtDay).isEqualTo(Money(50L))
                assertThat(pace.changePercent).isNull()
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
