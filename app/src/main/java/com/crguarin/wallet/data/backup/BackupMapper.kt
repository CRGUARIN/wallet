package com.crguarin.wallet.data.backup

import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.TransactionEntity
import java.time.Instant
import java.time.LocalDate

fun AccountEntity.toBackupDto(): AccountDto =
    AccountDto(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
        initialBalance = initialBalance,
        color = color,
        icon = icon,
        includeInTotal = includeInTotal,
        isArchived = isArchived,
        sortOrder = sortOrder,
        createdAt = createdAt.toEpochMilli(),
    )

fun AccountDto.toEntity(): AccountEntity =
    AccountEntity(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
        initialBalance = initialBalance,
        color = color,
        icon = icon,
        includeInTotal = includeInTotal,
        isArchived = isArchived,
        sortOrder = sortOrder,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

fun CategoryEntity.toBackupDto(): CategoryDto =
    CategoryDto(
        id = id,
        name = name,
        systemKey = systemKey,
        type = type,
        parentId = parentId,
        color = color,
        icon = icon,
        isArchived = isArchived,
        sortOrder = sortOrder,
    )

fun CategoryDto.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        systemKey = systemKey,
        type = type,
        parentId = parentId,
        color = color,
        icon = icon,
        isArchived = isArchived,
        sortOrder = sortOrder,
    )

fun TransactionEntity.toBackupDto(): TransactionDto =
    TransactionDto(
        id = id,
        type = type,
        amount = amount,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        date = date.toString(),
        note = note,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

/** Assumes [TransactionDto.date] was already validated by [BackupValidator]. */
fun TransactionDto.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        date = LocalDate.parse(date),
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
