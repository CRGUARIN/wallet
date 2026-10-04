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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(statement: CardStatementEntity): Long

    @Update
    suspend fun update(statement: CardStatementEntity): Int
}
