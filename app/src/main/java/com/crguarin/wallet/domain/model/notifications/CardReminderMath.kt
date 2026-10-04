package com.crguarin.wallet.domain.model.notifications

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.card.CardMath
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import java.time.LocalDate

/**
 * Payment reminder derived from a [CardOverview]: its due date, the amount to
 * pay (null when the statement was not reconciled) and its status.
 */
data class CardPaymentInfo(
    val dueDate: LocalDate,
    val amount: Money?,
    val status: StatementPaymentStatus,
)

/**
 * Pure card payment reminder calculation.
 *
 * With `C = CardMath.lastClosingDate(today, statementDay)`: when the last
 * reconciled statement closed at `C`, its due date and pending amount apply
 * (nothing when it is already PAID). Otherwise the due date is estimated with
 * `CardMath.dueDate(C, paymentDueDay)` and the amount is unknown; there is no
 * reminder when the current debt is zero. Returns null when nothing is pending.
 */
object CardReminderMath {
    fun paymentInfo(
        overview: CardOverview,
        statementDay: Int,
        paymentDueDay: Int,
        today: LocalDate,
    ): CardPaymentInfo? {
        val lastClosing = CardMath.lastClosingDate(today, statementDay)
        val statement = overview.lastStatement
        if (statement != null && statement.closingDate == lastClosing) {
            val status = overview.paymentStatus ?: return null
            if (status == StatementPaymentStatus.PAID) return null
            return CardPaymentInfo(
                dueDate = statement.dueDate,
                amount = overview.pendingStatementAmount,
                status = status,
            )
        }
        if (overview.debt <= Money.ZERO) return null
        val dueDate = CardMath.dueDate(lastClosing, paymentDueDay)
        val status =
            if (dueDate.isBefore(today)) {
                StatementPaymentStatus.OVERDUE
            } else {
                StatementPaymentStatus.PENDING
            }
        return CardPaymentInfo(dueDate = dueDate, amount = null, status = status)
    }
}
