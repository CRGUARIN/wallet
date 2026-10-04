package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.CardStatementEntity
import com.crguarin.wallet.domain.model.card.CardStatement

fun CardStatementEntity.toDomain(): CardStatement =
    CardStatement(
        id = id,
        accountId = accountId,
        closingDate = closingDate,
        dueDate = dueDate,
        statementBalance = Money(statementBalance),
        minimumPayment = minimumPayment?.let { Money(it) },
        statedInterestAndFees = statedInterestFees?.let { Money(it) },
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = createdAt,
    )

fun CardStatement.toEntity(): CardStatementEntity =
    CardStatementEntity(
        id = id,
        accountId = accountId,
        closingDate = closingDate,
        dueDate = dueDate,
        statementBalance = statementBalance.minorUnits,
        minimumPayment = minimumPayment?.minorUnits,
        statedInterestFees = statedInterestAndFees?.minorUnits,
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = createdAt,
    )
