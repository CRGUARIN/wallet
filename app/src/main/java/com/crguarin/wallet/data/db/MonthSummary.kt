package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo

data class MonthSummary(
    @ColumnInfo(name = "income")
    val income: Long,
    @ColumnInfo(name = "expense")
    val expense: Long,
) {
    val net: Long get() = income - expense
}
