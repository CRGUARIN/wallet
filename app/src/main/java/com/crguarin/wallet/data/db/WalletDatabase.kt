package com.crguarin.wallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        CardStatementEntity::class,
        RecurringPaymentEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun cardStatementDao(): CardStatementDao

    abstract fun recurringPaymentDao(): RecurringPaymentDao

    abstract fun debtDao(): DebtDao

    abstract fun backupDao(): BackupDao
}
