package com.crguarin.wallet.domain.model

import java.time.LocalDate

/**
 * A row rendered in a transactions list that is grouped by day.
 *
 * [Item] is a transaction and [DayHeader] is the header inserted before the first transaction of
 * each day. The sealed hierarchy is also used by Paging's `insertSeparators`, which requires the
 * item type to be a subtype of the resulting row type.
 *
 * The header carries only the date: the net of each day comes from a separate aggregated flow
 * that the screen looks up by date. Combining that flow with the `PagingData` flow would re-emit
 * an already collected `PagingData` whenever the totals change, which Paging rejects.
 */
sealed interface TransactionListEntry {
    data class Item(
        val details: TransactionDetails,
    ) : TransactionListEntry

    data class DayHeader(
        val date: LocalDate,
    ) : TransactionListEntry
}

/** Pure grouping logic: decides when a [TransactionListEntry.DayHeader] must be inserted. */
object TransactionDaySeparators {
    fun headerFor(
        before: TransactionListEntry.Item?,
        after: TransactionListEntry.Item?,
    ): TransactionListEntry.DayHeader? {
        if (after == null) return null
        val date = after.details.transaction.date
        if (before != null && before.details.transaction.date == date) return null
        return TransactionListEntry.DayHeader(date = date)
    }
}
