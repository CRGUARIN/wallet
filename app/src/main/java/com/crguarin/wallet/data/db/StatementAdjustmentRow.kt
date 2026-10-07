package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo

/** Amount of the adjustment movement linked to a card statement. */
data class StatementAdjustmentRow(
    @ColumnInfo(name = "statement_id")
    val statementId: Long,
    @ColumnInfo(name = "amount")
    val amount: Long,
)
