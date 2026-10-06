package com.crguarin.wallet.domain.model.recurring

import com.crguarin.wallet.core.money.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class RecurrenceMathTest {
    @Test
    fun `monthly from 31st clamps through february and back`() {
        val start = LocalDate.of(2026, 1, 31)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 0)).isEqualTo(start)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 1))
            .isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 2))
            .isEqualTo(LocalDate.of(2026, 3, 31))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 3))
            .isEqualTo(LocalDate.of(2026, 4, 30))
    }

    @Test
    fun `monthly from 31st lands on 29th in leap february`() {
        val start = LocalDate.of(2023, 12, 31)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 2))
            .isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `monthly from 30th clamps february only`() {
        val start = LocalDate.of(2026, 1, 30)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 1))
            .isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 1, 2))
            .isEqualTo(LocalDate.of(2026, 3, 30))
    }

    @Test
    fun `quarterly crosses year end from anchor`() {
        val start = LocalDate.of(2026, 10, 15)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 3, 0))
            .isEqualTo(LocalDate.of(2026, 10, 15))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 3, 1))
            .isEqualTo(LocalDate.of(2027, 1, 15))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.MONTHLY, 3, 2))
            .isEqualTo(LocalDate.of(2027, 4, 15))
    }

    @Test
    fun `yearly from feb 29 falls on feb 28 outside leap years`() {
        val start = LocalDate.of(2024, 2, 29)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.YEARLY, 1, 0))
            .isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.YEARLY, 1, 1))
            .isEqualTo(LocalDate.of(2025, 2, 28))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.YEARLY, 1, 4))
            .isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun `biweekly steps by 14 days`() {
        val start = LocalDate.of(2026, 9, 28)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.WEEKLY, 2, 0)).isEqualTo(start)
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.WEEKLY, 2, 1))
            .isEqualTo(LocalDate.of(2026, 10, 12))
        assertThat(RecurrenceMath.occurrence(start, RecurrenceFrequency.WEEKLY, 2, 3))
            .isEqualTo(LocalDate.of(2026, 11, 9))
    }

    @Test
    fun `firstOnOrAfter on an occurrence returns it`() {
        val start = LocalDate.of(2026, 1, 31)
        val second = LocalDate.of(2026, 2, 28)
        assertThat(
            RecurrenceMath.firstOnOrAfter(start, RecurrenceFrequency.MONTHLY, 1, second),
        ).isEqualTo(second)
    }

    @Test
    fun `firstOnOrAfter between occurrences returns the next one`() {
        val start = LocalDate.of(2026, 1, 31)
        assertThat(
            RecurrenceMath.firstOnOrAfter(start, RecurrenceFrequency.MONTHLY, 1, LocalDate.of(2026, 2, 10)),
        ).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(
            RecurrenceMath.firstOnOrAfter(start, RecurrenceFrequency.MONTHLY, 1, LocalDate.of(2026, 1, 15)),
        ).isEqualTo(start)
    }

    @Test
    fun `firstOnOrAfter far from start does not drift`() {
        val start = LocalDate.of(2020, 1, 31)
        assertThat(
            RecurrenceMath.firstOnOrAfter(start, RecurrenceFrequency.MONTHLY, 1, LocalDate.of(2026, 2, 10)),
        ).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(
            RecurrenceMath.firstOnOrAfter(start, RecurrenceFrequency.WEEKLY, 2, LocalDate.of(2026, 10, 1)),
        ).isEqualTo(LocalDate.of(2026, 10, 2))
    }

    @Test
    fun `nextAfter is strict`() {
        val start = LocalDate.of(2026, 9, 28)
        assertThat(
            RecurrenceMath.nextAfter(start, RecurrenceFrequency.WEEKLY, 1, LocalDate.of(2026, 9, 28)),
        ).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(
            RecurrenceMath.nextAfter(start, RecurrenceFrequency.WEEKLY, 1, LocalDate.of(2026, 9, 30)),
        ).isEqualTo(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `lastBefore is strict and null before the first occurrence`() {
        val start = LocalDate.of(2026, 1, 31)
        assertThat(RecurrenceMath.lastBefore(start, RecurrenceFrequency.MONTHLY, 1, start)).isNull()
        assertThat(RecurrenceMath.lastBefore(start, RecurrenceFrequency.MONTHLY, 1, start.minusDays(5))).isNull()
        assertThat(
            RecurrenceMath.lastBefore(start, RecurrenceFrequency.MONTHLY, 1, LocalDate.of(2026, 3, 31)),
        ).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(
            RecurrenceMath.lastBefore(start, RecurrenceFrequency.MONTHLY, 1, LocalDate.of(2026, 3, 1)),
        ).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(
            RecurrenceMath.lastBefore(start, RecurrenceFrequency.WEEKLY, 2, LocalDate.of(2026, 2, 15)),
        ).isEqualTo(LocalDate.of(2026, 2, 14))
    }

    @Test
    fun `occurrencesBetween is inclusive on both ends`() {
        val start = LocalDate.of(2026, 9, 7)
        val occurrences =
            RecurrenceMath.occurrencesBetween(
                start,
                RecurrenceFrequency.WEEKLY,
                1,
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 21),
            )
        assertThat(occurrences).containsExactly(
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 9, 14),
            LocalDate.of(2026, 9, 21),
        )
    }

    @Test
    fun `occurrencesBetween empty when nothing falls inside`() {
        val start = LocalDate.of(2026, 9, 7)
        assertThat(
            RecurrenceMath.occurrencesBetween(
                start,
                RecurrenceFrequency.WEEKLY,
                1,
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 13),
            ),
        ).isEmpty()
        assertThat(
            RecurrenceMath.occurrencesBetween(
                start,
                RecurrenceFrequency.WEEKLY,
                1,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 7),
            ),
        ).isEmpty()
    }

    @Test
    fun `endDate is inclusive and isFinished follows nextDueDate`() {
        val today = LocalDate.of(2026, 10, 12)
        val base = testPayment(startDate = LocalDate.of(2026, 9, 28), nextDueDate = LocalDate.of(2026, 10, 12))
        assertThat(base.copy(endDate = LocalDate.of(2026, 10, 12)).isFinished).isFalse()
        assertThat(base.copy(endDate = LocalDate.of(2026, 10, 11)).isFinished).isTrue()
        assertThat(base.copy(endDate = null, nextDueDate = today.plusYears(1)).isFinished).isFalse()
    }

    @Test
    fun `monthlyEquivalent of 52000 weekly uses integer minor-unit math`() {
        val amount = Money(5_200_000L)
        assertThat(RecurrenceMath.monthlyEquivalent(amount, RecurrenceFrequency.WEEKLY, 1))
            .isEqualTo(Money(22_533_333L))
    }

    @Test
    fun `monthlyEquivalent every two months and yearly`() {
        assertThat(RecurrenceMath.monthlyEquivalent(Money(100_000L), RecurrenceFrequency.MONTHLY, 2))
            .isEqualTo(Money(50_000L))
        assertThat(RecurrenceMath.monthlyEquivalent(Money(1_200_000L), RecurrenceFrequency.YEARLY, 1))
            .isEqualTo(Money(100_000L))
        assertThat(RecurrenceMath.monthlyEquivalent(Money(1_200_000L), RecurrenceFrequency.YEARLY, 2))
            .isEqualTo(Money(50_000L))
    }
}
