package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.data.db.AccountWithBalance as DbAccountWithBalance

fun AccountEntity.toDomain(): Account =
    Account(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
        initialBalance = Money(initialBalance),
        color = color,
        icon = icon,
        includeInTotal = includeInTotal,
        isArchived = isArchived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        creditLimit = creditLimit?.let { Money(it) },
        statementDay = statementDay,
        paymentDueDay = paymentDueDay,
        fare = fare?.let { Money(it) },
    )

fun Account.toEntity(): AccountEntity =
    AccountEntity(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
        initialBalance = initialBalance.minorUnits,
        color = color,
        icon = icon,
        includeInTotal = includeInTotal,
        isArchived = isArchived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        creditLimit = creditLimit?.minorUnits,
        statementDay = statementDay,
        paymentDueDay = paymentDueDay,
        fare = fare?.minorUnits,
    )

fun DbAccountWithBalance.toDomain(): AccountWithBalance =
    AccountWithBalance(
        account = account.toDomain(),
        balance = Money(balance),
    )
