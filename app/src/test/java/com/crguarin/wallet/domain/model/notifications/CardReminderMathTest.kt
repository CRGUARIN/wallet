package com.crguarin.wallet.domain.model.notifications

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CardReminderMathTest {
    private val today = LocalDate.of(2026, 10, 2)
    private val statementDay = 15
    private val paymentDueDay = 5

    @Test
    fun `reconciled last closing pending returns its due date and amount`() {
        val overview =
            overviewWith(
                statement =
                    statement(
                        closingDate = LocalDate.of(2026, 9, 15),
                        dueDate = LocalDate.of(2026, 10, 5),
                    ),
                pendingAmount = Money(450_000L),
                pendingDueDate = LocalDate.of(2026, 10, 5),
                status = StatementPaymentStatus.PENDING,
            )
        val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(info?.amount).isEqualTo(Money(450_000L))
        assertThat(info?.status).isEqualTo(StatementPaymentStatus.PENDING)
    }

    @Test
    fun `reconciled last closing paid returns null`() {
        val overview =
            overviewWith(
                statement =
                    statement(
                        closingDate = LocalDate.of(2026, 9, 15),
                        dueDate = LocalDate.of(2026, 10, 5),
                    ),
                pendingAmount = Money.ZERO,
                pendingDueDate = LocalDate.of(2026, 10, 5),
                status = StatementPaymentStatus.PAID,
            )
        assertThat(CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)).isNull()
    }

    @Test
    fun `not reconciled with debt estimates due date with unknown amount`() {
        val overview = overviewWith(statement = null, debt = Money(300_000L))
        val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)
        // Last closing before 2026-10-02 with statement day 15 is 2026-09-15;
        // payment day 5 falls in the next month: 2026-10-05.
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(info?.amount).isNull()
        assertThat(info?.status).isEqualTo(StatementPaymentStatus.PENDING)
    }

    @Test
    fun `not reconciled without debt returns null`() {
        val overview = overviewWith(statement = null, debt = Money.ZERO)
        assertThat(CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)).isNull()
    }

    @Test
    fun `old statement from a previous cut is ignored`() {
        val overview =
            overviewWith(
                statement =
                    statement(
                        closingDate = LocalDate.of(2026, 8, 15),
                        dueDate = LocalDate.of(2026, 9, 5),
                    ),
                pendingAmount = Money(100_000L),
                pendingDueDate = LocalDate.of(2026, 9, 5),
                status = StatementPaymentStatus.OVERDUE,
                debt = Money(200_000L),
            )
        val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(info?.amount).isNull()
        assertThat(info?.status).isEqualTo(StatementPaymentStatus.PENDING)
    }

    @Test
    fun `closing at month end clamps correctly`() {
        val endOfMonth = LocalDate.of(2026, 10, 31)
        val overview = overviewWith(statement = null, debt = Money(100_000L))
        val info = CardReminderMath.paymentInfo(overview, 31, 10, endOfMonth)
        // Last closing before 2026-10-31 with day 31 is 2026-09-30;
        // payment day 10 falls in October.
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 10, 10))
    }

    @Test
    fun `payment falls in the next month`() {
        val overview = overviewWith(statement = null, debt = Money(100_000L))
        // Statement day 28, payment day 10: closing 2026-09-28 pays on 2026-10-10.
        val info = CardReminderMath.paymentInfo(overview, 28, 10, today)
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 10, 10))
    }

    @Test
    fun `unreconciled overdue due date reports overdue status`() {
        val lateToday = LocalDate.of(2026, 10, 20)
        val overview = overviewWith(statement = null, debt = Money(100_000L))
        val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, lateToday)
        // Last closing before 2026-10-20 is 2026-10-15; payment day 5 -> 2026-11-05, still pending.
        assertThat(info?.dueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        assertThat(info?.status).isEqualTo(StatementPaymentStatus.PENDING)
    }

    private fun statement(
        closingDate: LocalDate,
        dueDate: LocalDate,
    ): CardStatement =
        CardStatement(
            id = 1L,
            accountId = 3L,
            closingDate = closingDate,
            dueDate = dueDate,
            statementBalance = Money(450_000L),
            minimumPayment = null,
            statedInterestAndFees = null,
            adjustmentTransactionId = null,
            createdAt = Instant.parse("2026-09-28T12:00:00Z"),
        )

    private fun overviewWith(
        statement: CardStatement?,
        pendingAmount: Money? = null,
        pendingDueDate: LocalDate? = null,
        status: StatementPaymentStatus? = null,
        debt: Money = Money(450_000L),
    ): CardOverview =
        CardOverview(
            accountId = 3L,
            debt = debt,
            creditLimit = Money(5_000_000L),
            available = Money(4_550_000L),
            usedPerMille = 90,
            nextClosingDate = LocalDate.of(2026, 10, 15),
            lastStatement = statement,
            pendingStatementAmount = pendingAmount,
            pendingStatementDueDate = pendingDueDate,
            paymentStatus = status,
            nextStatementCapitalEstimate = Money.ZERO,
            activeInstallments = emptyList(),
        )
}
