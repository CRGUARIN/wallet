package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import java.time.LocalDate

/**
 * Signed balance effect of a single date, produced by an aggregated query.
 *
 * The effect combines INCOME (+), EXPENSE (−), outgoing TRANSFER (−) and incoming TRANSFER (+)
 * for the requested account set. All effects before the requested `from` are grouped into a
 * single row dated `from - 1 day`.
 */
data class BalanceDeltaRow(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "delta")
    val delta: Long,
)
