package com.crguarin.wallet.domain.repository

import androidx.paging.PagingData
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    fun observeTransactions(filter: TransactionFilter = TransactionFilter()): Flow<List<TransactionDetails>>

    fun observeTransactionsPaged(filter: TransactionFilter = TransactionFilter()): Flow<PagingData<TransactionDetails>>

    fun observeRecentTransactions(limit: Int): Flow<List<TransactionDetails>>

    /**
     * Net amount per day within the given filter, used to render the day headers of a paginated
     * transactions list.
     */
    fun observeDailyTotals(filter: TransactionFilter = TransactionFilter()): Flow<Map<LocalDate, Money>>

    suspend fun getTransaction(id: Long): Transaction?

    suspend fun getTransactionDetails(id: Long): TransactionDetails?

    suspend fun saveTransaction(transaction: Transaction): Long

    /**
     * Inserts a previously deleted transaction preserving its id and timestamps, so it can be
     * restored exactly as it was (used by the "undo delete" action). Returns false when it cannot be
     * inserted, e.g. its account or category no longer exists or its id is taken.
     */
    suspend fun restoreTransaction(transaction: Transaction): Boolean

    suspend fun deleteTransaction(id: Long)

    fun observeMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long? = null,
    ): Flow<MonthSummary>

    suspend fun getMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long? = null,
    ): MonthSummary
}
