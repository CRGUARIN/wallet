package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Bulk reads and writes used by the backup import/export. Rows keep their ids, and the delete
 * methods are ordered by the foreign keys: card statements first (they point to transactions),
 * then transactions, recurring payments, subcategories, root categories, accounts.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAccounts(): List<AccountEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM card_statements ORDER BY id")
    suspend fun getCardStatements(): List<CardStatementEntity>

    @Query("SELECT * FROM recurring_payments ORDER BY id")
    suspend fun getRecurringPayments(): List<RecurringPaymentEntity>

    @Query("DELETE FROM card_statements")
    suspend fun deleteAllCardStatements()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAllRecurringPayments()

    @Query("DELETE FROM categories WHERE parent_id IS NOT NULL")
    suspend fun deleteSubcategories()

    @Query("DELETE FROM categories WHERE parent_id IS NULL")
    suspend fun deleteRootCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecurringPayments(payments: List<RecurringPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCardStatements(statements: List<CardStatementEntity>)
}
