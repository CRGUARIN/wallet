package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.crguarin.wallet.domain.model.debt.DebtDirection
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "person_name")
    val personName: String,
    @ColumnInfo(name = "direction")
    val direction: DebtDirection,
    @ColumnInfo(name = "amount")
    val amount: Long,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "due_date")
    val dueDate: LocalDate? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
