package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.DebtEntity
import com.crguarin.wallet.data.db.DebtPaymentEntity
import com.crguarin.wallet.data.db.DebtWithPaid
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.model.debt.DebtSummary

fun DebtEntity.toDomain(): Debt =
    Debt(
        id = id,
        personName = personName,
        direction = direction,
        amount = Money(amount),
        date = date,
        dueDate = dueDate,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Debt.toEntity(): DebtEntity =
    DebtEntity(
        id = id,
        personName = personName,
        direction = direction,
        amount = amount.minorUnits,
        date = date,
        dueDate = dueDate,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun DebtPaymentEntity.toDomain(): DebtPayment =
    DebtPayment(
        id = id,
        debtId = debtId,
        amount = Money(amount),
        date = date,
        note = note,
        createdAt = createdAt,
    )

fun DebtPayment.toEntity(): DebtPaymentEntity =
    DebtPaymentEntity(
        id = id,
        debtId = debtId,
        amount = amount.minorUnits,
        date = date,
        note = note,
        createdAt = createdAt,
    )

fun DebtWithPaid.toDomain(): DebtSummary =
    DebtSummary(
        debt = debt.toDomain(),
        paid = Money(paid),
    )
