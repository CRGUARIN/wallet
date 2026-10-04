package com.crguarin.wallet.domain.model.recurring

import com.crguarin.wallet.core.money.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Pure calendar calculations for recurring payments.
 *
 * Occurrence `k` (k >= 0) is always computed from [startDate] (the calendar
 * anchor), never by chaining the previous occurrence, so there is no drift.
 * `firstOnOrAfter` and `nextAfter` estimate `k` arithmetically and adjust by a
 * step, so they never iterate from `k = 0` over large spans.
 */
object RecurrenceMath {
    fun occurrence(
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
        k: Long,
    ): LocalDate {
        require(interval >= 1) { "interval must be >= 1" }
        require(k >= 0) { "k must be >= 0" }
        return when (frequency) {
            RecurrenceFrequency.WEEKLY -> startDate.plusDays(7L * interval * k)
            RecurrenceFrequency.MONTHLY -> {
                val month = YearMonth.from(startDate).plusMonths(interval.toLong() * k)
                month.atDay(minOf(startDate.dayOfMonth, month.lengthOfMonth()))
            }
            RecurrenceFrequency.YEARLY -> startDate.plusYears(interval.toLong() * k)
        }
    }

    fun occurrence(
        payment: RecurringPayment,
        k: Long,
    ): LocalDate = occurrence(payment.startDate, payment.frequency, payment.interval, k)

    /**
     * First occurrence on or after [date].
     */
    fun firstOnOrAfter(
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
        date: LocalDate,
    ): LocalDate = occurrence(startDate, frequency, interval, firstIndexOnOrAfter(startDate, frequency, interval, date))

    fun firstOnOrAfter(
        payment: RecurringPayment,
        date: LocalDate,
    ): LocalDate = firstOnOrAfter(payment.startDate, payment.frequency, payment.interval, date)

    /**
     * First occurrence strictly after [date].
     */
    fun nextAfter(
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
        date: LocalDate,
    ): LocalDate = firstOnOrAfter(startDate, frequency, interval, date.plusDays(1))

    fun nextAfter(
        payment: RecurringPayment,
        date: LocalDate,
    ): LocalDate = nextAfter(payment.startDate, payment.frequency, payment.interval, date)

    /**
     * Every occurrence in [from, to] (both inclusive). Empty when `from > to`.
     */
    fun occurrencesBetween(
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> {
        if (from > to) return emptyList()
        val result = mutableListOf<LocalDate>()
        var k = firstIndexOnOrAfter(startDate, frequency, interval, from)
        while (true) {
            val current = occurrence(startDate, frequency, interval, k)
            if (current > to) break
            result.add(current)
            k++
        }
        return result
    }

    fun occurrencesBetween(
        payment: RecurringPayment,
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> = occurrencesBetween(payment.startDate, payment.frequency, payment.interval, from, to)

    /**
     * Monthly money equivalent in minor units, with integer division:
     * WEEKLY `amount*52 / (12*interval)`, MONTHLY `amount / interval`,
     * YEARLY `amount / (12*interval)`.
     */
    fun monthlyEquivalent(
        amount: Money,
        frequency: RecurrenceFrequency,
        interval: Int,
    ): Money {
        require(interval >= 1) { "interval must be >= 1" }
        val minor = amount.minorUnits
        return when (frequency) {
            RecurrenceFrequency.WEEKLY -> Money(minor * 52L / (12L * interval))
            RecurrenceFrequency.MONTHLY -> Money(minor / interval)
            RecurrenceFrequency.YEARLY -> Money(minor / (12L * interval))
        }
    }

    fun monthlyEquivalent(payment: RecurringPayment): Money = monthlyEquivalent(payment.amount, payment.frequency, payment.interval)

    private fun firstIndexOnOrAfter(
        startDate: LocalDate,
        frequency: RecurrenceFrequency,
        interval: Int,
        date: LocalDate,
    ): Long {
        require(interval >= 1) { "interval must be >= 1" }
        if (date <= startDate) return 0L
        var k =
            when (frequency) {
                RecurrenceFrequency.WEEKLY -> {
                    val days = ChronoUnit.DAYS.between(startDate, date)
                    ceilDiv(days, 7L * interval)
                }
                RecurrenceFrequency.MONTHLY -> {
                    val months = monthsBetween(YearMonth.from(startDate), YearMonth.from(date))
                    (months / interval).toLong()
                }
                RecurrenceFrequency.YEARLY -> ((date.year - startDate.year) / interval).toLong()
            }.coerceAtLeast(0L)
        while (occurrence(startDate, frequency, interval, k) < date) k++
        while (k > 0 && occurrence(startDate, frequency, interval, k - 1) >= date) k--
        return k
    }

    private fun monthsBetween(
        from: YearMonth,
        to: YearMonth,
    ): Long = (to.year - from.year) * 12L + (to.monthValue - from.monthValue)

    private fun ceilDiv(
        dividend: Long,
        divisor: Long,
    ): Long = (dividend + divisor - 1) / divisor
}
