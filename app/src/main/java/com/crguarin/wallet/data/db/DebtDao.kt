package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query(
        """
        SELECT d.*, COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.debt_id = d.id), 0) AS paid
        FROM debts d
        ORDER BY d.date, d.id
        """,
    )
    fun observeAllWithPaid(): Flow<List<DebtWithPaid>>

    @Query(
        """
        SELECT d.*, COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.debt_id = d.id), 0) AS paid
        FROM debts d
        WHERE d.id = :id
        """,
    )
    fun observeWithPaid(id: Long): Flow<DebtWithPaid?>

    @Query("SELECT * FROM debt_payments WHERE debt_id = :debtId ORDER BY date DESC, id DESC")
    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments WHERE debt_id = :debtId")
    suspend fun getPaid(debtId: Long): Long

    @Query("SELECT * FROM debt_payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): DebtPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity): Int

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long): Int
}
