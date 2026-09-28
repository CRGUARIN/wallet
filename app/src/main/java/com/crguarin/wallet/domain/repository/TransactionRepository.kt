package com.crguarin.wallet.domain.repository

import androidx.paging.PagingData
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

    fun observeTransaction(id: Long): Flow<TransactionDetails?>

    suspend fun getTransaction(id: Long): Transaction?

    suspend fun getTransactionDetails(id: Long): TransactionDetails?

    suspend fun saveTransaction(transaction: Transaction): Long

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
