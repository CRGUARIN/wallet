package com.crguarin.wallet.data.backup

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode

/** A small valid backup: two accounts, two root categories, one subcategory and one transaction of each type. */
fun validBackupDto(): BackupDto =
    BackupDto(
        schemaVersion = BackupDto.SCHEMA_VERSION,
        appVersion = "1.0.0",
        exportedAt = "2026-09-28T15:00:00Z",
        currency = "COP",
        accounts =
            listOf(
                AccountDto(1, "Efectivo", AccountType.CASH, "COP", 5_000_000, -16711936, "wallet", true, false, 0, 1_790_000_000_000),
                AccountDto(2, "Ahorros", AccountType.SAVINGS, "COP", 0, -16776961, "savings", true, false, 1, 1_790_000_000_001),
            ),
        categories =
            listOf(
                CategoryDto(1, null, "food", CategoryType.EXPENSE, null, -65536, "restaurant", false, 0),
                CategoryDto(2, "Salario", null, CategoryType.INCOME, null, -16711936, "work", false, 1),
                CategoryDto(3, "Almuerzos", null, CategoryType.EXPENSE, 1, -65536, "restaurant", false, 0),
            ),
        transactions =
            listOf(
                TransactionDto(
                    1,
                    TransactionType.EXPENSE,
                    1_500_000,
                    1,
                    null,
                    3,
                    "2026-09-27",
                    "Almuerzo",
                    1_790_000_000_000,
                    1_790_000_000_000,
                ),
                TransactionDto(2, TransactionType.INCOME, 20_000_000, 2, null, 2, "2026-09-28", null, 1_790_000_000_000, 1_790_000_000_000),
                TransactionDto(3, TransactionType.TRANSFER, 100_000, 1, 2, null, "2026-09-28", null, 1_790_000_000_000, 1_790_000_000_000),
            ),
    )

/**
 * A valid version 2 backup with a credit card: negative initial balance (current debt),
 * an installment purchase, an interest adjustment and two statements (one with adjustment,
 * one without).
 */
fun validCardBackupDto(): BackupDto =
    validBackupDto().copy(
        accounts =
            validBackupDto().accounts +
                AccountDto(
                    id = 3,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    currencyCode = "COP",
                    initialBalance = -2_000_000,
                    color = -16711936,
                    icon = "credit_card",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 2,
                    createdAt = 1_790_000_000_002,
                    creditLimit = 10_000_000,
                    statementDay = 15,
                    paymentDueDay = 5,
                ),
        categories =
            validBackupDto().categories +
                listOf(
                    CategoryDto(4, null, "card_interest_fees", CategoryType.EXPENSE, null, -65536, "credit_card", false, 3),
                    CategoryDto(5, null, "card_adjustment", CategoryType.INCOME, null, -16711936, "payments", false, 4),
                ),
        transactions =
            validBackupDto().transactions +
                listOf(
                    TransactionDto(
                        4,
                        TransactionType.EXPENSE,
                        300_000,
                        3,
                        null,
                        1,
                        "2026-09-10",
                        "Compra a cuotas",
                        1_790_000_000_000,
                        1_790_000_000_000,
                        installments = 3,
                    ),
                    TransactionDto(
                        5,
                        TransactionType.EXPENSE,
                        15_000,
                        3,
                        null,
                        4,
                        "2026-09-15",
                        "Extracto 2026-09-15",
                        1_790_000_000_000,
                        1_790_000_000_000,
                    ),
                ),
        cardStatements =
            listOf(
                CardStatementDto(
                    1,
                    3,
                    "2026-08-15",
                    "2026-09-05",
                    2_000_000,
                    minimumPayment = 100_000,
                    statedInterestAndFees = null,
                    adjustmentTransactionId = null,
                    createdAt = 1_790_000_000_000,
                ),
                CardStatementDto(
                    2,
                    3,
                    "2026-09-15",
                    "2026-10-05",
                    2_315_000,
                    minimumPayment = 115_000,
                    statedInterestAndFees = 15_000,
                    adjustmentTransactionId = 5,
                    createdAt = 1_790_000_000_001,
                ),
            ),
    )

/**
 * A valid version 3 backup: the card backup plus two recurring payments (an AUTO monthly
 * expense on the credit card and a CONFIRM yearly expense with an end date) and one
 * transaction generated by the first payment.
 */
fun validRecurringBackupDto(): BackupDto {
    val card = validCardBackupDto()
    return card.copy(
        recurringPayments =
            listOf(
                RecurringPaymentDto(
                    id = 1,
                    name = "Netflix",
                    type = TransactionType.EXPENSE,
                    amount = 4_590_000,
                    accountId = 3,
                    categoryId = 1,
                    note = null,
                    frequency = RecurrenceFrequency.MONTHLY,
                    interval = 1,
                    startDate = "2026-09-15",
                    endDate = null,
                    nextDueDate = "2026-10-15",
                    mode = RecurringMode.AUTO,
                    reminderDaysBefore = 3,
                    isActive = true,
                    createdAt = 1_790_000_000_000,
                    updatedAt = 1_790_000_000_000,
                ),
                RecurringPaymentDto(
                    id = 2,
                    name = "SOAT",
                    type = TransactionType.EXPENSE,
                    amount = 30_000_000,
                    accountId = 2,
                    categoryId = 1,
                    note = "Moto",
                    frequency = RecurrenceFrequency.YEARLY,
                    interval = 1,
                    startDate = "2026-01-10",
                    endDate = "2028-01-10",
                    nextDueDate = "2027-01-10",
                    mode = RecurringMode.CONFIRM,
                    reminderDaysBefore = null,
                    isActive = true,
                    createdAt = 1_790_000_000_000,
                    updatedAt = 1_790_000_000_000,
                ),
            ),
        transactions =
            card.transactions +
                TransactionDto(
                    id = 6,
                    type = TransactionType.EXPENSE,
                    amount = 4_590_000,
                    accountId = 3,
                    toAccountId = null,
                    categoryId = 1,
                    date = "2026-09-15",
                    note = "Netflix",
                    createdAt = 1_790_000_000_000,
                    updatedAt = 1_790_000_000_000,
                    recurringId = 1,
                ),
    )
}

/**
 * A valid version 4 backup: the recurring backup plus two debts (an owed-to-me debt with a due date
 * and two payments, and a debt I owe without due date) and their payments.
 */
fun validDebtBackupDto(): BackupDto {
    val recurring = validRecurringBackupDto()
    return recurring.copy(
        debts =
            listOf(
                DebtDto(
                    id = 1,
                    personName = "Juan Pérez",
                    direction = DebtDirection.OWED_TO_ME,
                    amount = 50_000_000,
                    date = "2026-08-01",
                    dueDate = "2026-12-01",
                    note = "Préstamo para la moto",
                    createdAt = 1_790_000_000_000,
                    updatedAt = 1_790_000_000_001,
                ),
                DebtDto(
                    id = 2,
                    personName = "María López",
                    direction = DebtDirection.I_OWE,
                    amount = 12_000_000,
                    date = "2026-09-20",
                    dueDate = null,
                    note = null,
                    createdAt = 1_790_000_000_002,
                    updatedAt = 1_790_000_000_002,
                ),
            ),
        debtPayments =
            listOf(
                DebtPaymentDto(
                    id = 1,
                    debtId = 1,
                    amount = 10_000_000,
                    date = "2026-09-01",
                    note = "Primer abono",
                    createdAt = 1_790_000_000_003,
                ),
                DebtPaymentDto(
                    id = 2,
                    debtId = 1,
                    amount = 5_000_000,
                    date = "2026-10-01",
                    note = null,
                    createdAt = 1_790_000_000_004,
                ),
                DebtPaymentDto(
                    id = 3,
                    debtId = 2,
                    amount = 4_000_000,
                    date = "2026-10-02",
                    note = "Abono de octubre",
                    createdAt = 1_790_000_000_005,
                ),
            ),
    )
}
