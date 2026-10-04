package com.crguarin.wallet.domain.model.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class CardMathTest {
    private val instant = Instant.parse("2026-09-28T12:00:00Z")

    @Test
    fun `closing day 31 clamps in short months`() {
        assertThat(CardMath.closingDate(YearMonth.of(2026, 2), 31)).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(CardMath.closingDate(YearMonth.of(2024, 2), 31)).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(CardMath.closingDate(YearMonth.of(2026, 4), 31)).isEqualTo(LocalDate.of(2026, 4, 30))
        assertThat(CardMath.closingDate(YearMonth.of(2026, 1), 31)).isEqualTo(LocalDate.of(2026, 1, 31))
    }

    @Test
    fun `closing day 30 clamps in february`() {
        assertThat(CardMath.closingDate(YearMonth.of(2026, 2), 30)).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(CardMath.closingDate(YearMonth.of(2024, 2), 30)).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `next closing is today when closing equals today`() {
        val today = LocalDate.of(2026, 9, 15)
        assertThat(CardMath.nextClosingDate(today, 15)).isEqualTo(today)
    }

    @Test
    fun `next closing moves to next month after the closing day`() {
        assertThat(CardMath.nextClosingDate(LocalDate.of(2026, 9, 16), 15))
            .isEqualTo(LocalDate.of(2026, 10, 15))
    }

    @Test
    fun `last closing is strictly before today`() {
        assertThat(CardMath.lastClosingDate(LocalDate.of(2026, 9, 15), 15))
            .isEqualTo(LocalDate.of(2026, 8, 15))
        assertThat(CardMath.lastClosingDate(LocalDate.of(2026, 9, 16), 15))
            .isEqualTo(LocalDate.of(2026, 9, 15))
    }

    @Test
    fun `previous closing goes to the prior month`() {
        assertThat(CardMath.previousClosingDate(LocalDate.of(2026, 9, 15), 15))
            .isEqualTo(LocalDate.of(2026, 8, 15))
        assertThat(CardMath.previousClosingDate(LocalDate.of(2026, 3, 31), 31))
            .isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @Test
    fun `due date stays in the same month when due day is after closing day`() {
        assertThat(CardMath.dueDate(LocalDate.of(2026, 9, 15), 25))
            .isEqualTo(LocalDate.of(2026, 9, 25))
    }

    @Test
    fun `due date moves to next month when due day is on or before closing day`() {
        assertThat(CardMath.dueDate(LocalDate.of(2026, 9, 15), 15))
            .isEqualTo(LocalDate.of(2026, 10, 15))
        assertThat(CardMath.dueDate(LocalDate.of(2026, 9, 20), 5))
            .isEqualTo(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `due date crosses year boundary`() {
        assertThat(CardMath.dueDate(LocalDate.of(2026, 12, 20), 5))
            .isEqualTo(LocalDate.of(2027, 1, 5))
    }

    @Test
    fun `due date clamps in short months`() {
        assertThat(CardMath.dueDate(LocalDate.of(2026, 1, 15), 31))
            .isEqualTo(LocalDate.of(2026, 1, 31))
        assertThat(CardMath.dueDate(LocalDate.of(2026, 2, 10), 5))
            .isEqualTo(LocalDate.of(2026, 3, 5))
    }

    @Test
    fun `purchase on closing day bills in that closing`() {
        assertThat(CardMath.firstBillingClosing(LocalDate.of(2026, 9, 15), 15))
            .isEqualTo(LocalDate.of(2026, 9, 15))
    }

    @Test
    fun `purchase the day after closing bills in the next closing`() {
        assertThat(CardMath.firstBillingClosing(LocalDate.of(2026, 9, 16), 15))
            .isEqualTo(LocalDate.of(2026, 10, 15))
    }

    @Test
    fun `installments of 100000 in 3 split with remainder on the last one`() {
        val schedule =
            CardMath.installmentSchedule(
                Money(10_000_000L),
                3,
                LocalDate.of(2026, 9, 10),
                15,
            )
        assertThat(schedule.map { it.first })
            .containsExactly(
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 11, 15),
            ).inOrder()
        assertThat(schedule.map { it.second })
            .containsExactly(
                Money(3_333_333L),
                Money(3_333_333L),
                Money(3_333_334L),
            ).inOrder()
        assertThat(schedule.fold(Money.ZERO) { acc, (_, capital) -> acc + capital })
            .isEqualTo(Money(10_000_000L))
    }

    @Test
    fun `single installment bills the full amount`() {
        val schedule =
            CardMath.installmentSchedule(Money(50_000L), 1, LocalDate.of(2026, 9, 10), 15)
        assertThat(schedule).containsExactly(LocalDate.of(2026, 9, 15) to Money(50_000L))
    }

    @Test
    fun `48 installments add up to the exact amount`() {
        val schedule =
            CardMath.installmentSchedule(Money(10_000_000L), 48, LocalDate.of(2026, 9, 10), 15)
        assertThat(schedule).hasSize(48)
        assertThat(schedule.fold(Money.ZERO) { acc, (_, capital) -> acc + capital })
            .isEqualTo(Money(10_000_000L))
        assertThat(schedule.map { it.first }.toSet()).hasSize(48)
    }

    @Test
    fun `installment status mid plan`() {
        val tx = purchase(Money(10_000_000L), 3, LocalDate.of(2026, 9, 10))
        val status = CardMath.installmentStatus(tx, 15, LocalDate.of(2026, 10, 15))

        assertThat(status.billedInstallments).isEqualTo(2)
        assertThat(status.remainingAmount).isEqualTo(Money(3_333_334L))
        assertThat(status.nextInstallmentCapital).isEqualTo(Money(3_333_334L))
    }

    @Test
    fun `installment status on the last day is finished`() {
        val tx = purchase(Money(10_000_000L), 3, LocalDate.of(2026, 9, 10))
        val status = CardMath.installmentStatus(tx, 15, LocalDate.of(2026, 11, 15))

        assertThat(status.billedInstallments).isEqualTo(3)
        assertThat(status.remainingAmount).isEqualTo(Money.ZERO)
        assertThat(status.nextInstallmentCapital).isNull()
    }

    @Test
    fun `installment status after the plan stays finished`() {
        val tx = purchase(Money(10_000_000L), 3, LocalDate.of(2026, 9, 10))
        val status = CardMath.installmentStatus(tx, 15, LocalDate.of(2027, 5, 1))

        assertThat(status.billedInstallments).isEqualTo(3)
        assertThat(status.remainingAmount).isEqualTo(Money.ZERO)
        assertThat(status.nextInstallmentCapital).isNull()
    }

    @Test
    fun `installment status before the first closing bills nothing`() {
        val tx = purchase(Money(10_000_000L), 3, LocalDate.of(2026, 9, 10))
        val status = CardMath.installmentStatus(tx, 15, LocalDate.of(2026, 9, 14))

        assertThat(status.billedInstallments).isEqualTo(0)
        assertThat(status.remainingAmount).isEqualTo(Money(10_000_000L))
        assertThat(status.nextInstallmentCapital).isEqualTo(Money(3_333_333L))
    }

    @Test
    fun `app debt negates balance`() {
        assertThat(CardMath.appDebtAt(Money(-200_000L), Money(-300_000L))).isEqualTo(Money(500_000L))
        assertThat(CardMath.appDebtAt(Money(50_000L), Money.ZERO)).isEqualTo(Money(-50_000L))
    }

    @Test
    fun `used per mille clamps to 0-1000`() {
        assertThat(CardMath.usedPerMille(Money(500_000L), Money(1_000_000L))).isEqualTo(500)
        assertThat(CardMath.usedPerMille(Money(1_500_000L), Money(1_000_000L))).isEqualTo(1000)
        assertThat(CardMath.usedPerMille(Money(-100_000L), Money(1_000_000L))).isEqualTo(0)
        assertThat(CardMath.usedPerMille(Money.ZERO, Money(1_000_000L))).isEqualTo(0)
    }

    private fun purchase(
        amount: Money,
        installments: Int,
        date: LocalDate,
    ) = Transaction(
        id = 7L,
        type = TransactionType.EXPENSE,
        amount = amount,
        accountId = 1L,
        categoryId = 1L,
        date = date,
        note = "note",
        installments = installments,
        createdAt = instant,
        updatedAt = instant,
    )
}
