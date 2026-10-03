package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import java.time.LocalDate

/**
 * Income and expense totals for a single day, produced by an aggregated query.
 *
 * Transfers are excluded, so a day without income or expenses does not appear at all.
 */
data class DailyIncomeExpenseRow(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "income")
    val income: Long,
    @ColumnInfo(name = "expense")
    val expense: Long,
)
