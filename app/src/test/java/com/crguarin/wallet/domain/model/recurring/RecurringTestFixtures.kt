package com.crguarin.wallet.domain.model.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * Shared builders for recurring payment tests (fixed dates, no `LocalDate.now()`).
 */
fun testPayment(
    id: Long = 1L,
    name: String = "Netflix",
    type: TransactionType = TransactionType.EXPENSE,
    amount: Money = Money(45_900L),
    accountId: Long = 1L,
    categoryId: Long = 1L,
    note: String? = null,
    frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    interval: Int = 1,
    startDate: LocalDate = LocalDate.of(2026, 9, 28),
    endDate: LocalDate? = null,
    nextDueDate: LocalDate = startDate,
    mode: RecurringMode = RecurringMode.AUTO,
    reminderDaysBefore: Int? = null,
    isActive: Boolean = true,
): RecurringPayment =
    RecurringPayment(
        id = id,
        name = name,
        type = type,
        amount = amount,
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
        createdAt = Instant.parse("2026-09-28T12:00:00Z"),
        updatedAt = Instant.parse("2026-09-28T12:00:00Z"),
    )

fun testAccount(
    id: Long = 1L,
    name: String = "Ahorros",
    type: AccountType = AccountType.SAVINGS,
    isArchived: Boolean = false,
): Account =
    Account(
        id = id,
        name = name,
        type = type,
        initialBalance = Money(1_000_000L),
        color = 1,
        icon = "savings",
        isArchived = isArchived,
        createdAt = Instant.parse("2026-09-28T12:00:00Z"),
    )

fun testCardAccount(
    id: Long = 3L,
    name: String = "Visa",
    isArchived: Boolean = false,
): Account =
    Account(
        id = id,
        name = name,
        type = AccountType.CREDIT_CARD,
        initialBalance = Money.ZERO,
        color = 3,
        icon = "card",
        isArchived = isArchived,
        createdAt = Instant.parse("2026-09-28T12:00:00Z"),
        creditLimit = Money(5_000_000L),
        statementDay = 15,
        paymentDueDay = 5,
    )

fun testCategory(
    id: Long = 1L,
    name: String = "Suscripciones",
    type: CategoryType = CategoryType.EXPENSE,
    isArchived: Boolean = false,
): Category =
    Category(
        id = id,
        name = name,
        type = type,
        color = 1,
        icon = "repeat",
        isArchived = isArchived,
    )
