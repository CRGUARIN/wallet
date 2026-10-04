package com.crguarin.wallet.data.backup

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.CardStatementEntity
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.RecurringPaymentEntity
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
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
        creditLimit = creditLimit,
        statementDay = statementDay,
        paymentDueDay = paymentDueDay,
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
        creditLimit = creditLimit,
        statementDay = statementDay,
        paymentDueDay = paymentDueDay,
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
        installments = installments,
        recurringId = recurringId,
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
        installments = installments,
        recurringId = recurringId,
    )

fun CardStatementEntity.toBackupDto(): CardStatementDto =
    CardStatementDto(
        id = id,
        accountId = accountId,
        closingDate = closingDate.toString(),
        dueDate = dueDate.toString(),
        statementBalance = statementBalance,
        minimumPayment = minimumPayment,
        statedInterestAndFees = statedInterestFees,
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = createdAt.toEpochMilli(),
    )

/** Assumes [CardStatementDto.closingDate] and [CardStatementDto.dueDate] were already validated by [BackupValidator]. */
fun CardStatementDto.toEntity(): CardStatementEntity =
    CardStatementEntity(
        id = id,
        accountId = accountId,
        closingDate = LocalDate.parse(closingDate),
        dueDate = LocalDate.parse(dueDate),
        statementBalance = statementBalance,
        minimumPayment = minimumPayment,
        statedInterestFees = statedInterestAndFees,
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

fun RecurringPayment.toBackupDto(): RecurringPaymentDto =
    RecurringPaymentDto(
        id = id,
        name = name,
        type = type,
        amount = amount.minorUnits,
        accountId = accountId,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        interval = interval,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        nextDueDate = nextDueDate.toString(),
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

fun RecurringPaymentEntity.toBackupDto(): RecurringPaymentDto =
    RecurringPaymentDto(
        id = id,
        name = name,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        interval = interval,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        nextDueDate = nextDueDate.toString(),
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

/** Assumes the dates of [RecurringPaymentDto] were already validated by [BackupValidator]. */
fun RecurringPaymentDto.toEntity(): RecurringPaymentEntity =
    RecurringPaymentEntity(
        id = id,
        name = name,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = categoryId,
        note = note,
        frequency = frequency,
        interval = interval,
        startDate = LocalDate.parse(startDate),
        nextDueDate = LocalDate.parse(nextDueDate),
        endDate = endDate?.let(LocalDate::parse),
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

/** Assumes the dates of [RecurringPaymentDto] were already validated by [BackupValidator]. */
fun RecurringPaymentDto.toDomain(): RecurringPayment =
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
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.let(LocalDate::parse),
        nextDueDate = LocalDate.parse(nextDueDate),
        mode = mode,
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
