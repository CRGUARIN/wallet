package com.crguarin.wallet.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crguarin.wallet.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

private const val TRANSACTION_DETAILS_SELECT =
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
"""

private const val TRANSACTION_FILTER_WHERE =
    """
WHERE (:startDate IS NULL OR t.date >= :startDate)
  AND (:endDate IS NULL OR t.date <= :endDate)
  AND (:accountId IS NULL OR t.account_id = :accountId OR t.to_account_id = :accountId)
  AND (:categoryId IS NULL OR t.category_id = :categoryId OR t.category_id IN (SELECT c2.id FROM categories c2 WHERE c2.parent_id = :categoryId))
  AND (:type IS NULL OR t.type = :type)
  AND (:noteQuery IS NULL OR :noteQuery = '' OR t.note LIKE '%' || :noteQuery || '%')
"""

@Dao
interface TransactionDao {
    @Query(
        """
        $TRANSACTION_DETAILS_SELECT
        $TRANSACTION_FILTER_WHERE
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
        $TRANSACTION_DETAILS_SELECT
        $TRANSACTION_FILTER_WHERE
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
        $TRANSACTION_DETAILS_SELECT
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionWithDetails>>

    @Query(
        """
        $TRANSACTION_DETAILS_SELECT
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
        $TRANSACTION_FILTER_WHERE
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

    @Query(
        """
        SELECT
            t.category_id      AS category_id,
            c.name             AS category_name,
            c.system_key       AS category_system_key,
            c.color            AS category_color,
            c.icon             AS category_icon,
            c.parent_id        AS parent_id,
            pc.name            AS parent_name,
            pc.system_key      AS parent_system_key,
            pc.color           AS parent_color,
            pc.icon            AS parent_icon,
            SUM(t.amount)      AS total,
            COUNT(*)           AS tx_count
        FROM transactions t
        LEFT JOIN categories c  ON t.category_id = c.id
        LEFT JOIN categories pc ON c.parent_id = pc.id
        WHERE t.type = :type
          AND t.date >= :from AND t.date <= :to
          AND (:accountId IS NULL OR t.account_id = :accountId)
        GROUP BY t.category_id
        """,
    )
    fun observeCategoryTotals(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
        accountId: Long? = null,
    ): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT
            t.date AS date,
            COALESCE(SUM(CASE WHEN t.type = 'INCOME'  THEN t.amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS expense
        FROM transactions t
        WHERE t.date >= :from AND t.date <= :to
          AND t.type IN ('INCOME', 'EXPENSE')
          AND (:accountId IS NULL OR t.account_id = :accountId)
        GROUP BY t.date
        ORDER BY t.date
        """,
    )
    fun observeDailyIncomeExpense(
        from: LocalDate,
        to: LocalDate,
        accountId: Long? = null,
    ): Flow<List<DailyIncomeExpenseRow>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (account_id = :accountId OR to_account_id = :accountId)
          AND date >= :from
        ORDER BY date, id
        """,
    )
    fun observeCardTransactions(
        accountId: Long,
        from: LocalDate,
    ): Flow<List<TransactionEntity>>

    /**
     * Signed sum of balance effects of [accountId] with date <= [date], without the
     * initial balance: INCOME (+), EXPENSE (−), outgoing TRANSFER (−), incoming TRANSFER (+).
     */
    @Query(
        """
        SELECT
            COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'INCOME' AND date <= :date), 0)
            - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'EXPENSE' AND date <= :date), 0)
            - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = :accountId AND type = 'TRANSFER' AND date <= :date), 0)
            + COALESCE((SELECT SUM(amount) FROM transactions WHERE to_account_id = :accountId AND type = 'TRANSFER' AND date <= :date), 0)
        """,
    )
    suspend fun getAccountEffectUpTo(
        accountId: Long,
        date: LocalDate,
    ): Long

    /**
     * Signed balance effects per date for [accountIds], up to [to] (inclusive).
     *
     * The first branch covers INCOME (+), EXPENSE (−) and outgoing TRANSFER (−); the second covers
     * incoming TRANSFER (+). A transfer between two accounts of the set appears in both branches
     * and nets to zero. Everything before [from] is grouped into a single row dated [openingDate]
     * (`from - 1 day`). Callers must not pass an empty [accountIds].
     */
    @Query(
        """
        SELECT CASE WHEN x.date < :from THEN :openingDate ELSE x.date END AS date, SUM(x.delta) AS delta
        FROM (
          SELECT t.date AS date,
                 CASE t.type WHEN 'INCOME' THEN t.amount ELSE -t.amount END AS delta
          FROM transactions t
          WHERE t.account_id IN (:accountIds) AND t.date <= :to
          UNION ALL
          SELECT t.date AS date, t.amount AS delta
          FROM transactions t
          WHERE t.type = 'TRANSFER' AND t.to_account_id IN (:accountIds) AND t.date <= :to
        ) x
        GROUP BY 1
        ORDER BY 1
        """,
    )
    fun observeBalanceDeltas(
        accountIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        openingDate: LocalDate,
    ): Flow<List<BalanceDeltaRow>>
}
