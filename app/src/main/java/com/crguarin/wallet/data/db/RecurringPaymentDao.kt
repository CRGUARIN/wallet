package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments ORDER BY next_due_date, id")
    fun observeAll(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments ORDER BY next_due_date, id")
    suspend fun getAll(): List<RecurringPaymentEntity>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    fun observeById(id: Long): Flow<RecurringPaymentEntity?>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    suspend fun getById(id: Long): RecurringPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: RecurringPaymentEntity): Long

    @Update
    suspend fun update(payment: RecurringPaymentEntity): Int

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query(
        "UPDATE recurring_payments SET next_due_date = :nextDueDate, updated_at = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun updateNextDueDate(
        id: Long,
        nextDueDate: LocalDate,
        updatedAt: Instant,
    ): Int

    @Query("SELECT COUNT(*) FROM recurring_payments WHERE account_id = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring_payments WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("UPDATE recurring_payments SET category_id = :toId WHERE category_id = :fromId")
    suspend fun reassignCategory(
        fromId: Long,
        toId: Long,
    ): Int
}
