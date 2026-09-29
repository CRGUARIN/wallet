package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money
import java.time.LocalDate

/**
 * A row rendered in a transactions list that is grouped by day.
 *
 * [Item] is a transaction and [DayHeader] is the header inserted before the first transaction of
 * each day. The sealed hierarchy is also used by Paging's `insertSeparators`, which requires the
 * item type to be a subtype of the resulting row type.
 */
sealed interface TransactionListEntry {
    data class Item(
        val details: TransactionDetails,
    ) : TransactionListEntry

    data class DayHeader(
        val date: LocalDate,
        val net: Money,
    ) : TransactionListEntry
}

/**
 * Pure grouping logic: decides when a [TransactionListEntry.DayHeader] must be inserted and what
 * net amount the day shows.
 *
 * The net of a day is provided by [netByDate] and already accounts for the active filters
 * (income and expenses only, unless an account filter is active, in which case transfers carry the
 * sign from that account's perspective).
 */
object TransactionDaySeparators {
    fun headerFor(
        before: TransactionListEntry.Item?,
        after: TransactionListEntry.Item?,
        netByDate: Map<LocalDate, Money>,
    ): TransactionListEntry.DayHeader? {
        if (after == null) return null
        val date = after.details.transaction.date
        if (before != null && before.details.transaction.date == date) return null
        return TransactionListEntry.DayHeader(date = date, net = netByDate[date] ?: Money.ZERO)
    }
}
