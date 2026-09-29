package com.crguarin.wallet.data.backup

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType

/** A small valid backup: two accounts, two root categories, one subcategory and one transaction of each type. */
fun validBackupDto(): BackupDto =
    BackupDto(
        schemaVersion = 1,
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
