package com.crguarin.wallet.ui.accounts

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.InstallmentStatus
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import java.time.Instant
import java.time.LocalDate

fun sampleCardStatement(
    id: Long = 1L,
    accountId: Long = 7L,
    closingDate: LocalDate = LocalDate.of(2026, 9, 15),
    dueDate: LocalDate = LocalDate.of(2026, 10, 5),
    statementBalance: Money = Money(250_000_00L),
    adjustmentTransactionId: Long? = null,
): CardStatement =
    CardStatement(
        id = id,
        accountId = accountId,
        closingDate = closingDate,
        dueDate = dueDate,
        statementBalance = statementBalance,
        minimumPayment = null,
        statedInterestAndFees = null,
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = Instant.EPOCH,
    )

fun sampleInstallment(
    transactionId: Long = 11L,
    billedInstallments: Int = 1,
    installments: Int = 3,
    remainingAmount: Money = Money(200_000_00L),
): InstallmentStatus =
    InstallmentStatus(
        transactionId = transactionId,
        date = LocalDate.of(2026, 9, 10),
        note = "Portátil",
        categoryId = null,
        amount = Money(300_000_00L),
        installments = installments,
        billedInstallments = billedInstallments,
        remainingAmount = remainingAmount,
        nextInstallmentCapital = Money(100_000_00L),
    )

fun sampleCardOverview(
    accountId: Long = 7L,
    debt: Money = Money(250_000_00L),
    today: LocalDate = LocalDate.of(2026, 9, 28),
    paymentStatus: StatementPaymentStatus? = StatementPaymentStatus.PENDING,
    pendingAmount: Money? = Money(250_000_00L),
    dueDate: LocalDate? = LocalDate.of(2026, 10, 5),
): CardOverview =
    CardOverview(
        accountId = accountId,
        debt = debt,
        creditLimit = Money(500_000_00L),
        available = Money(500_000_00L) - debt,
        usedPerMille = 500,
        nextClosingDate = LocalDate.of(2026, 10, 15),
        lastStatement = sampleCardStatement(accountId = accountId),
        pendingStatementAmount = pendingAmount,
        pendingStatementDueDate = dueDate,
        paymentStatus = paymentStatus,
        nextStatementCapitalEstimate = Money(100_000_00L),
        activeInstallments = listOf(sampleInstallment()),
    )
