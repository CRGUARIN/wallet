package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CardStatementDao {
    @Query("SELECT * FROM card_statements WHERE account_id = :accountId ORDER BY closing_date DESC")
    fun observeByAccount(accountId: Long): Flow<List<CardStatementEntity>>

    @Query("SELECT * FROM card_statements WHERE account_id = :accountId ORDER BY closing_date DESC LIMIT 1")
    fun observeLastByAccount(accountId: Long): Flow<CardStatementEntity?>

    @Query("SELECT * FROM card_statements WHERE account_id = :accountId AND closing_date = :closingDate")
    suspend fun getByAccountAndClosing(
        accountId: Long,
        closingDate: LocalDate,
    ): CardStatementEntity?

    @Query(
        """
        SELECT s.id AS statement_id, t.amount AS amount
        FROM card_statements s
        INNER JOIN transactions t ON t.id = s.adjustment_transaction_id
        WHERE s.account_id = :accountId
        """,
    )
    fun observeAdjustmentAmounts(accountId: Long): Flow<List<StatementAdjustmentRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(statement: CardStatementEntity): Long

    @Update
    suspend fun update(statement: CardStatementEntity): Int
}
