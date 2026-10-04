package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "card_statements",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["adjustment_transaction_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["account_id", "closing_date"], unique = true),
        Index(value = ["adjustment_transaction_id"]),
    ],
)
data class CardStatementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "closing_date")
    val closingDate: LocalDate,
    @ColumnInfo(name = "due_date")
    val dueDate: LocalDate,
    @ColumnInfo(name = "statement_balance")
    val statementBalance: Long,
    @ColumnInfo(name = "minimum_payment")
    val minimumPayment: Long? = null,
    @ColumnInfo(name = "stated_interest_fees")
    val statedInterestFees: Long? = null,
    @ColumnInfo(name = "adjustment_transaction_id")
    val adjustmentTransactionId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)
