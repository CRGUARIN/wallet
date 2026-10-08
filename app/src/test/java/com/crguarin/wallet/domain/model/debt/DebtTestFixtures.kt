package com.crguarin.wallet.domain.model.debt

import com.crguarin.wallet.core.money.Money
import java.time.Instant
import java.time.LocalDate

/**
 * Shared builders for debt tests (fixed dates, no `LocalDate.now()`).
 */
fun testDebt(
    id: Long = 1L,
    personName: String = "Ana",
    direction: DebtDirection = DebtDirection.OWED_TO_ME,
    amount: Money = Money(100_000L),
    date: LocalDate = LocalDate.of(2026, 9, 1),
    dueDate: LocalDate? = null,
    note: String? = null,
    createdAt: Instant = Instant.parse("2026-09-01T12:00:00Z"),
    updatedAt: Instant = createdAt,
): Debt =
    Debt(
        id = id,
        personName = personName,
        direction = direction,
        amount = amount,
        date = date,
        dueDate = dueDate,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun testDebtPayment(
    id: Long = 1L,
    debtId: Long = 1L,
    amount: Money = Money(10_000L),
    date: LocalDate = LocalDate.of(2026, 9, 2),
    note: String? = null,
    createdAt: Instant = Instant.parse("2026-09-02T12:00:00Z"),
): DebtPayment =
    DebtPayment(
        id = id,
        debtId = debtId,
        amount = amount,
        date = date,
        note = note,
        createdAt = createdAt,
    )
