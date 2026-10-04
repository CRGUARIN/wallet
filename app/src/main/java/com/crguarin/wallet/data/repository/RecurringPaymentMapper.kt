package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.RecurringPaymentEntity
import com.crguarin.wallet.domain.model.recurring.RecurringPayment

fun RecurringPaymentEntity.toDomain(): RecurringPayment =
    RecurringPayment(
        id = id,
        name = name,
        type = type,
        amount = Money(amount),
        accountId = accountId,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        nextDueDate = nextDueDate,
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RecurringPayment.toEntity(): RecurringPaymentEntity =
    RecurringPaymentEntity(
        id = id,
        name = name,
        type = type,
        amount = amount.minorUnits,
        accountId = accountId,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        interval = interval,
        startDate = startDate,
        nextDueDate = nextDueDate,
        endDate = endDate,
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
