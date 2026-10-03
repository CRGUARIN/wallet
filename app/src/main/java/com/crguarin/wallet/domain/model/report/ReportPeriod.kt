package com.crguarin.wallet.domain.model.report

import java.time.LocalDate
import java.time.YearMonth

/**
 * Date range shown by the reports screen: a whole month or a whole year.
 *
 * `start` and `end` are inclusive. [previous] and [next] shift the period by one unit of the same
 * kind (one month for [Month], one year for [Year]).
 */
sealed interface ReportPeriod {
    val start: LocalDate
    val end: LocalDate

    fun previous(): ReportPeriod

    fun next(): ReportPeriod

    data class Month(
        val yearMonth: YearMonth,
    ) : ReportPeriod {
        override val start: LocalDate
            get() = yearMonth.atDay(1)
        override val end: LocalDate
            get() = yearMonth.atEndOfMonth()

        override fun previous(): ReportPeriod = Month(yearMonth.minusMonths(1))

        override fun next(): ReportPeriod = Month(yearMonth.plusMonths(1))
    }

    data class Year(
        val year: java.time.Year,
    ) : ReportPeriod {
        override val start: LocalDate
            get() = LocalDate.of(year.value, 1, 1)
        override val end: LocalDate
            get() = LocalDate.of(year.value, 12, 31)

        override fun previous(): ReportPeriod = Year(year.minusYears(1))

        override fun next(): ReportPeriod = Year(year.plusYears(1))
    }
}

/** Kind of period selected by the user: a single month or a whole year. */
enum class ReportMode { MONTH, YEAR }
