package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class DebtWithPaid(
    @Embedded
    val debt: DebtEntity,
    @ColumnInfo(name = "paid")
    val paid: Long,
)
