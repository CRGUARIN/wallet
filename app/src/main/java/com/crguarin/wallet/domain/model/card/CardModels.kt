package com.crguarin.wallet.domain.model.card

import com.crguarin.wallet.core.money.Money
import java.time.Instant
import java.time.LocalDate

data class CardStatement(
    val id: Long = 0,
    val accountId: Long,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    /** Total debt reported by the bank at closing (>= 0). */
    val statementBalance: Money,
    val minimumPayment: Money?,
    val statedInterestAndFees: Money?,
    val adjustmentTransactionId: Long?,
    val createdAt: Instant,
)

enum class StatementPaymentStatus {
    PAID,
    PENDING,
    OVERDUE,
}

data class InstallmentStatus(
    val transactionId: Long,
    val date: LocalDate,
    val note: String?,
    val categoryId: Long?,
    val amount: Money,
    val installments: Int,
    val billedInstallments: Int,
    val remainingAmount: Money,
    /** Null when the plan is finished. */
    val nextInstallmentCapital: Money?,
)

data class CardOverview(
    val accountId: Long,
    /** -balance (negative = credit in favor). */
    val debt: Money,
    val creditLimit: Money,
    /** creditLimit + balance. */
    val available: Money,
    /** debt*1000/creditLimit clamped to 0..1000. */
    val usedPerMille: Int,
    val nextClosingDate: LocalDate,
    val lastStatement: CardStatement?,
    /** Null when there is no statement. */
    val pendingStatementAmount: Money?,
    val pendingStatementDueDate: LocalDate?,
    val paymentStatus: StatementPaymentStatus?,
    val nextStatementCapitalEstimate: Money,
    /** Plans not finished, most recent first. */
    val activeInstallments: List<InstallmentStatus>,
)

data class ReconciliationPreview(
    val accountId: Long,
    val closingDate: LocalDate,
    val appDebt: Money,
    val statementBalance: Money,
    /** statementBalance - appDebt. */
    val difference: Money,
    /** Stated interest given and difference > 0 and difference != stated. */
    val interestMismatch: Boolean,
)

enum class BalanceWarning {
    NONE,
    NEGATIVE_BALANCE,
    OVER_CREDIT_LIMIT,
}
