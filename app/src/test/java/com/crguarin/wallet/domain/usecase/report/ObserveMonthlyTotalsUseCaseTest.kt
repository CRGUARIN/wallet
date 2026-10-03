package com.crguarin.wallet.domain.usecase.report

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeReportRepository
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObserveMonthlyTotalsUseCaseTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var useCase: ObserveMonthlyTotalsUseCase

    @Before
    fun setUp() {
        reportRepository = FakeReportRepository()
        useCase = ObserveMonthlyTotalsUseCase(reportRepository)
    }

    @Test
    fun `aggregates daily rows into the requested months in order`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 11, 5, income = 100L, expense = 40L),
                        daily(2027, 1, 3, income = 300L, expense = 30L),
                    ),
            )
            val months =
                listOf(
                    YearMonth.of(2026, 11),
                    YearMonth.of(2026, 12),
                    YearMonth.of(2027, 1),
                )

            useCase(months, accountId = null).test {
                assertThat(awaitItem())
                    .containsExactly(
                        MonthTotals(YearMonth.of(2026, 11), Money(100L), Money(40L)),
                        MonthTotals(YearMonth.of(2026, 12), Money.ZERO, Money.ZERO),
                        MonthTotals(YearMonth.of(2027, 1), Money(300L), Money(30L)),
                    ).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `queries the whole range between the first and last month once`() =
        runTest {
            val months = listOf(YearMonth.of(2026, 11), YearMonth.of(2027, 1))

            useCase(months, accountId = null).test {
                awaitItem()

                assertThat(reportRepository.dailyRangeRequests)
                    .containsExactly(LocalDate.of(2026, 11, 1) to LocalDate.of(2027, 1, 31))
                    .inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ignores rows outside the requested range`() =
        runTest {
            reportRepository.setInitialData(
                dailyIncomeExpense =
                    listOf(
                        daily(2026, 8, 31, income = 999L),
                        daily(2026, 9, 15, income = 10L),
                    ),
            )

            useCase(listOf(YearMonth.of(2026, 9)), accountId = null).test {
                assertThat(awaitItem())
                    .containsExactly(MonthTotals(YearMonth.of(2026, 9), Money(10L), Money.ZERO))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `returns an empty list without querying when there are no months`() =
        runTest {
            useCase(emptyList(), accountId = null).test {
                assertThat(awaitItem()).isEmpty()
                assertThat(reportRepository.dailyRangeRequests).isEmpty()
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
