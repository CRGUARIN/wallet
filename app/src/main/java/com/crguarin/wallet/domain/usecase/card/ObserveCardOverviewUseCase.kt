package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardMath
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CardStatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Overview of a credit card: debt, available credit, next closing, last statement
 * status, next statement estimate and active installment plans. Emits null when the
 * account is not a credit card.
 */
class ObserveCardOverviewUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val cardStatementRepository: CardStatementRepository,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(accountId: Long): Flow<CardOverview?> {
            val today = timeProvider.today()
            val from = today.minusMonths(CARD_HISTORY_MONTHS)
            return combine(
                accountRepository.observeAccountWithBalance(accountId),
                cardStatementRepository.observeLastStatement(accountId),
                cardStatementRepository.observeCardTransactions(accountId, from),
            ) { accountWithBalance, lastStatement, transactions ->
                buildOverview(accountId, accountWithBalance, lastStatement, transactions, today)
            }
        }

        private fun buildOverview(
            accountId: Long,
            accountWithBalance: AccountWithBalance?,
            lastStatement: CardStatement?,
            transactions: List<Transaction>,
            today: LocalDate,
        ): CardOverview? {
            val account = accountWithBalance?.account ?: return null
            if (account.type != AccountType.CREDIT_CARD) return null
            val creditLimit = account.creditLimit ?: return null
            val statementDay = account.statementDay ?: return null

            val balance = accountWithBalance.balance
            val debt = -balance
            val nextClosing = CardMath.nextClosingDate(today, statementDay)
            val previousClosing = CardMath.previousClosingDate(nextClosing, statementDay)

            val (pendingAmount, pendingDueDate, paymentStatus) =
                pendingOf(lastStatement, transactions, accountId, today)

            return CardOverview(
                accountId = accountId,
                debt = debt,
                creditLimit = creditLimit,
                available = creditLimit + balance,
                usedPerMille = CardMath.usedPerMille(debt, creditLimit),
                nextClosingDate = nextClosing,
                lastStatement = lastStatement,
                pendingStatementAmount = pendingAmount,
                pendingStatementDueDate = pendingDueDate,
                paymentStatus = paymentStatus,
                nextStatementCapitalEstimate =
                    estimateOf(transactions, previousClosing, nextClosing, statementDay, today),
                activeInstallments =
                    activePlansOf(transactions, statementDay, today),
            )
        }

        private fun pendingOf(
            lastStatement: CardStatement?,
            transactions: List<Transaction>,
            accountId: Long,
            today: LocalDate,
        ): Triple<Money?, LocalDate?, StatementPaymentStatus?> {
            if (lastStatement == null) return Triple(null, null, null)
            val payments =
                transactions
                    .filter { it.date > lastStatement.closingDate }
                    .filter {
                        (it.type == TransactionType.TRANSFER && it.toAccountId == accountId) ||
                            (it.type == TransactionType.INCOME && it.accountId == accountId)
                    }.fold(Money.ZERO) { acc, tx -> acc + tx.amount }
            val pending = lastStatement.statementBalance - payments
            val remaining = if (pending.isNegative) Money.ZERO else pending
            val status =
                when {
                    remaining.isZero -> StatementPaymentStatus.PAID
                    !lastStatement.dueDate.isBefore(today) -> StatementPaymentStatus.PENDING
                    else -> StatementPaymentStatus.OVERDUE
                }
            return Triple(remaining, lastStatement.dueDate, status)
        }

        private fun estimateOf(
            transactions: List<Transaction>,
            previousClosing: LocalDate,
            nextClosing: LocalDate,
            statementDay: Int,
            today: LocalDate,
        ): Money {
            var total = Money.ZERO
            for (tx in transactions) {
                if (tx.type != TransactionType.EXPENSE) continue
                val installments = tx.installments ?: 1
                if (installments <= 1) {
                    if (tx.date > previousClosing && !tx.date.isAfter(nextClosing)) {
                        total += tx.amount
                    }
                } else {
                    val capital =
                        CardMath
                            .installmentSchedule(tx.amount, installments, tx.date, statementDay)
                            .firstOrNull { (date, _) -> date == nextClosing }
                            ?.second
                    if (capital != null) total += capital
                }
            }
            return total
        }

        private fun activePlansOf(
            transactions: List<Transaction>,
            statementDay: Int,
            today: LocalDate,
        ) = transactions
            .filter { it.type == TransactionType.EXPENSE && (it.installments ?: 1) > 1 }
            .map { CardMath.installmentStatus(it, statementDay, today) }
            .filter { it.nextInstallmentCapital != null }
            .sortedWith(compareByDescending { it.date })

        companion object {
            /** Observed transaction window: 48 installments plus one month of margin. */
            const val CARD_HISTORY_MONTHS = 49L
        }
    }
