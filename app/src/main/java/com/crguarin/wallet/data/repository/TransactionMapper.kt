package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.data.db.MonthSummary as DbMonthSummary
import com.crguarin.wallet.data.db.TransactionWithDetails as DbTransactionWithDetails

fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = id,
        type = type,
        amount = Money(amount),
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        date = date,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        installments = installments,
        recurringId = recurringId,
    )

fun Transaction.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        type = type,
        amount = amount.minorUnits,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        date = date,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        installments = installments,
        recurringId = recurringId,
    )

fun DbTransactionWithDetails.toDomain(): TransactionDetails =
    TransactionDetails(
        transaction = transaction.toDomain(),
        accountName = accountName,
        accountColor = accountColor,
        accountIcon = accountIcon,
        toAccountName = toAccountName,
        toAccountColor = toAccountColor,
        toAccountIcon = toAccountIcon,
        categoryName = categoryName,
        categorySystemKey = categorySystemKey,
        categoryColor = categoryColor,
        categoryIcon = categoryIcon,
        categoryParentId = categoryParentId,
        parentCategoryName = parentCategoryName,
        parentCategorySystemKey = parentCategorySystemKey,
    )

fun DbMonthSummary.toDomain(): MonthSummary =
    MonthSummary(
        income = Money(income),
        expense = Money(expense),
    )
