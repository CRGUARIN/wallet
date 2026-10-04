package com.crguarin.wallet.domain.model.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure date and money calculations for credit card billing cycles and installment plans.
 *
 * A billing cycle ending at closing [C] covers transactions with date in
 * (previous closing, C]. Installment capitals use integer minor-unit division and the
 * last installment absorbs the remainder, so the parts always add up to the exact amount.
 */
object CardMath {
    /**
     * Closing date of [month]: day [statementDay], or the last day of the month when shorter.
     */
    fun closingDate(
        month: YearMonth,
        statementDay: Int,
    ): LocalDate = month.atDay(minOf(statementDay, month.lengthOfMonth()))

    /**
     * First closing on or after [today] (when it is today, the closing is today).
     */
    fun nextClosingDate(
        today: LocalDate,
        statementDay: Int,
    ): LocalDate {
        val thisMonth = closingDate(YearMonth.from(today), statementDay)
        return if (!thisMonth.isBefore(today)) {
            thisMonth
        } else {
            closingDate(YearMonth.from(today).plusMonths(1), statementDay)
        }
    }

    /**
     * Most recent closing strictly before [today].
     */
    fun lastClosingDate(
        today: LocalDate,
        statementDay: Int,
    ): LocalDate {
        val thisMonth = closingDate(YearMonth.from(today), statementDay)
        return if (thisMonth.isBefore(today)) {
            thisMonth
        } else {
            closingDate(YearMonth.from(today).minusMonths(1), statementDay)
        }
    }

    /**
     * Closing strictly before [closing].
     */
    fun previousClosingDate(
        closing: LocalDate,
        statementDay: Int,
    ): LocalDate {
        val sameMonth = closingDate(YearMonth.from(closing), statementDay)
        return if (sameMonth.isBefore(closing)) {
            sameMonth
        } else {
            closingDate(YearMonth.from(closing).minusMonths(1), statementDay)
        }
    }

    /**
     * Payment due date of the statement closed at [closing].
     */
    fun dueDate(
        closing: LocalDate,
        paymentDueDay: Int,
    ): LocalDate {
        val month =
            if (paymentDueDay > closing.dayOfMonth) {
                YearMonth.from(closing)
            } else {
                YearMonth.from(closing).plusMonths(1)
            }
        return month.atDay(minOf(paymentDueDay, month.lengthOfMonth()))
    }

    /**
     * First closing on or after [purchaseDate]: the statement that bills the first installment.
     */
    fun firstBillingClosing(
        purchaseDate: LocalDate,
        statementDay: Int,
    ): LocalDate {
        val candidate = closingDate(YearMonth.from(purchaseDate), statementDay)
        return if (!candidate.isBefore(purchaseDate)) {
            candidate
        } else {
            closingDate(YearMonth.from(purchaseDate).plusMonths(1), statementDay)
        }
    }

    /**
     * Capital billed at each closing for a purchase of [amount] in [installments].
     * Integer minor-unit division; the last installment absorbs the remainder.
     */
    fun installmentSchedule(
        amount: Money,
        installments: Int,
        purchaseDate: LocalDate,
        statementDay: Int,
    ): List<Pair<LocalDate, Money>> {
        require(installments >= 1) { "installments must be >= 1" }
        val firstMonth = YearMonth.from(firstBillingClosing(purchaseDate, statementDay))
        val baseMinor = amount.minorUnits / installments
        return (0 until installments).map { index ->
            val date = closingDate(firstMonth.plusMonths(index.toLong()), statementDay)
            val capital =
                if (index == installments - 1) {
                    Money(amount.minorUnits - baseMinor * (installments - 1))
                } else {
                    Money(baseMinor)
                }
            date to capital
        }
    }

    /**
     * Billing progress of [tx] at [today]. A null installment count counts as a single installment.
     */
    fun installmentStatus(
        tx: Transaction,
        statementDay: Int,
        today: LocalDate,
    ): InstallmentStatus {
        val installments = tx.installments ?: 1
        val schedule = installmentSchedule(tx.amount, installments, tx.date, statementDay)
        val billed = schedule.count { (date, _) -> !date.isAfter(today) }
        val billedCapital = schedule.take(billed).fold(Money.ZERO) { acc, (_, capital) -> acc + capital }
        return InstallmentStatus(
            transactionId = tx.id,
            date = tx.date,
            note = tx.note,
            categoryId = tx.categoryId,
            amount = tx.amount,
            installments = installments,
            billedInstallments = billed,
            remainingAmount = tx.amount - billedCapital,
            nextInstallmentCapital = schedule.getOrNull(billed)?.second,
        )
    }

    /**
     * Debt per mille of [limit] (debt * 1000 / limit), clamped to 0..1000.
     */
    fun usedPerMille(
        debt: Money,
        limit: Money,
    ): Int {
        if (limit <= Money.ZERO) return 0
        val raw = debt.minorUnits * 1000L / limit.minorUnits
        return raw.coerceIn(0L, 1000L).toInt()
    }

    /**
     * Debt at a closing date: the negated balance (initial plus signed effects up to that date).
     */
    fun appDebtAt(
        initialBalance: Money,
        effectUpTo: Money,
    ): Money = -(initialBalance + effectUpTo)
}
