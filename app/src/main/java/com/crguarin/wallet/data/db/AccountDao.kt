package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query(
        """
        SELECT a.*,
          a.initial_balance
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'INCOME'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'EXPENSE'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'TRANSFER'), 0)
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE to_account_id = a.id AND type = 'TRANSFER'), 0)
          AS balance
        FROM accounts a
        ORDER BY a.is_archived, a.sort_order, a.name
        """,
    )
    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>>

    @Query(
        """
        SELECT a.*,
          a.initial_balance
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'INCOME'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'EXPENSE'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'TRANSFER'), 0)
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE to_account_id = a.id AND type = 'TRANSFER'), 0)
          AS balance
        FROM accounts a
        WHERE a.id = :id
        """,
    )
    fun observeAccountWithBalanceById(id: Long): Flow<AccountWithBalance?>

    @Query(
        """
        SELECT a.*,
          a.initial_balance
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'INCOME'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'EXPENSE'), 0)
          - COALESCE((SELECT SUM(amount) FROM transactions WHERE account_id = a.id AND type = 'TRANSFER'), 0)
          + COALESCE((SELECT SUM(amount) FROM transactions WHERE to_account_id = a.id AND type = 'TRANSFER'), 0)
          AS balance
        FROM accounts a
        WHERE a.id = :id
        """,
    )
    suspend fun getAccountWithBalanceById(id: Long): AccountWithBalance?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY is_archived, sort_order, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity): Int

    @Delete
    suspend fun delete(account: AccountEntity): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId OR to_account_id = :accountId")
    suspend fun countTransactions(accountId: Long): Int

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM accounts
          WHERE is_archived = 0
            AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
            AND (:excludeId IS NULL OR id != :excludeId)
        )
        """,
    )
    suspend fun existsActiveWithName(
        name: String,
        excludeId: Long? = null,
    ): Boolean
}
