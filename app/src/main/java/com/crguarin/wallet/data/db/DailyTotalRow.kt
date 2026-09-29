package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import java.time.LocalDate

/**
 * Net amount for a single day, produced by an aggregated query.
 *
 * `net` is `income - expenses`. Transfers are excluded unless an account filter is applied, in
 * which case outgoing transfers subtract and incoming transfers add.
 */
data class DailyTotalRow(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "net")
    val net: Long,
)
