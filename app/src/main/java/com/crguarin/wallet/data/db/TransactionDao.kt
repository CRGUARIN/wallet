package com.crguarin.wallet.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crguarin.wallet.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT
            t.*,
            a.name AS account_name,
            a.color AS account_color,
            a.icon AS account_icon,
            to_a.name AS to_account_name,
            to_a.color AS to_account_color,
            to_a.icon AS to_account_icon,
            c.name AS category_name,
            c.system_key AS category_system_key,
            c.color AS category_color,
            c.icon AS category_icon,
            c.parent_id AS category_parent_id,
            pc.name AS parent_category_name,
            pc.system_key AS parent_category_system_key
        FROM transactions t
        INNER JOIN accounts a ON t.account_id = a.id
        LEFT JOIN accounts to_a ON t.to_account_id = to_a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        WHERE (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
          AND (:accountId IS NULL OR t.account_id = :accountId OR t.to_account_id = :accountId)
          AND (:categoryId IS NULL OR t.category_id = :categoryId OR t.category_id IN (SELECT c2.id FROM categories c2 WHERE c2.parent_id = :categoryId))
          AND (:type IS NULL OR t.type = :type)
          AND (:noteQuery IS NULL OR :noteQuery = '' OR t.note LIKE '%' || :noteQuery || '%')
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        """,
    )
    fun observeTransactions(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType? = null,
        noteQuery: String? = null,
    ): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
            t.*,
            a.name AS account_name,
            a.color AS account_color,
            a.icon AS account_icon,
            to_a.name AS to_account_name,
            to_a.color AS to_account_color,
            to_a.icon AS to_account_icon,
            c.name AS category_name,
            c.system_key AS category_system_key,
            c.color AS category_color,
            c.icon AS category_icon,
            c.parent_id AS category_parent_id,
            pc.name AS parent_category_name,
            pc.system_key AS parent_category_system_key
        FROM transactions t
        INNER JOIN accounts a ON t.account_id = a.id
        LEFT JOIN accounts to_a ON t.to_account_id = to_a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        WHERE (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
          AND (:accountId IS NULL OR t.account_id = :accountId OR t.to_account_id = :accountId)
          AND (:categoryId IS NULL OR t.category_id = :categoryId OR t.category_id IN (SELECT c2.id FROM categories c2 WHERE c2.parent_id = :categoryId))
          AND (:type IS NULL OR t.type = :type)
          AND (:noteQuery IS NULL OR :noteQuery = '' OR t.note LIKE '%' || :noteQuery || '%')
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        """,
    )
    fun observeTransactionsPaged(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType? = null,
        noteQuery: String? = null,
    ): PagingSource<Int, TransactionWithDetails>

    @Query(
        """
        SELECT
            t.*,
            a.name AS account_name,
            a.color AS account_color,
            a.icon AS account_icon,
            to_a.name AS to_account_name,
            to_a.color AS to_account_color,
            to_a.icon AS to_account_icon,
            c.name AS category_name,
            c.system_key AS category_system_key,
            c.color AS category_color,
            c.icon AS category_icon,
            c.parent_id AS category_parent_id,
            pc.name AS parent_category_name,
            pc.system_key AS parent_category_system_key
        FROM transactions t
        INNER JOIN accounts a ON t.account_id = a.id
        LEFT JOIN accounts to_a ON t.to_account_id = to_a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
            t.*,
            a.name AS account_name,
            a.color AS account_color,
            a.icon AS account_icon,
            to_a.name AS to_account_name,
            to_a.color AS to_account_color,
            to_a.icon AS to_account_icon,
            c.name AS category_name,
            c.system_key AS category_system_key,
            c.color AS category_color,
            c.icon AS category_icon,
            c.parent_id AS category_parent_id,
            pc.name AS parent_category_name,
            pc.system_key AS parent_category_system_key
        FROM transactions t
        INNER JOIN accounts a ON t.account_id = a.id
        LEFT JOIN accounts to_a ON t.to_account_id = to_a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        WHERE t.id = :id
        """,
    )
    fun observeById(id: Long): Flow<TransactionWithDetails?>

    @Query(
        """
        SELECT
            t.*,
            a.name AS account_name,
            a.color AS account_color,
            a.icon AS account_icon,
            to_a.name AS to_account_name,
            to_a.color AS to_account_color,
            to_a.icon AS to_account_icon,
            c.name AS category_name,
            c.system_key AS category_system_key,
            c.color AS category_color,
            c.icon AS category_icon,
            c.parent_id AS category_parent_id,
            pc.name AS parent_category_name,
            pc.system_key AS parent_category_system_key
        FROM transactions t
        INNER JOIN accounts a ON t.account_id = a.id
        LEFT JOIN accounts to_a ON t.to_account_id = to_a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        WHERE t.id = :id
        """,
    )
    suspend fun getWithDetailsById(id: Long): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query(
        """
        SELECT
            t.date AS date,
            COALESCE(
                SUM(
                    CASE
                        WHEN t.type = 'INCOME' THEN t.amount
                        WHEN t.type = 'EXPENSE' THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND :accountId IS NOT NULL AND t.account_id = :accountId THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND :accountId IS NOT NULL AND t.to_account_id = :accountId THEN t.amount
                        ELSE 0
                    END
                ),
                0
            ) AS net
        FROM transactions t
        WHERE (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
          AND (:accountId IS NULL OR t.account_id = :accountId OR t.to_account_id = :accountId)
          AND (:categoryId IS NULL OR t.category_id = :categoryId OR t.category_id IN (SELECT c2.id FROM categories c2 WHERE c2.parent_id = :categoryId))
          AND (:type IS NULL OR t.type = :type)
          AND (:noteQuery IS NULL OR :noteQuery = '' OR t.note LIKE '%' || :noteQuery || '%')
        GROUP BY t.date
        """,
    )
    fun observeDailyTotals(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType? = null,
        noteQuery: String? = null,
    ): Flow<List<DailyTotalRow>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        WHERE date >= :from AND date <= :to
          AND (:accountId IS NULL OR account_id = :accountId)
        """,
    )
    fun observeMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long? = null,
    ): Flow<MonthSummary>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        WHERE date >= :from AND date <= :to
          AND (:accountId IS NULL OR account_id = :accountId)
        """,
    )
    suspend fun getMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long? = null,
    ): MonthSummary
}
