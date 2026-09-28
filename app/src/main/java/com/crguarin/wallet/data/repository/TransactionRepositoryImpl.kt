package com.crguarin.wallet.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.crguarin.wallet.data.db.TransactionDao
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : TransactionRepository {
        override fun observeTransactions(filter: TransactionFilter): Flow<List<TransactionDetails>> =
            transactionDao
                .observeTransactions(
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                    accountId = filter.accountId,
                    categoryId = filter.categoryId,
                    type = filter.type,
                    noteQuery = filter.noteQuery,
                ).map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeTransactionsPaged(filter: TransactionFilter): Flow<PagingData<TransactionDetails>> =
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = {
                    transactionDao.observeTransactionsPaged(
                        startDate = filter.startDate,
                        endDate = filter.endDate,
                        accountId = filter.accountId,
                        categoryId = filter.categoryId,
                        type = filter.type,
                        noteQuery = filter.noteQuery,
                    )
                },
            ).flow
                .map { pagingData ->
                    pagingData.map { it.toDomain() }
                }.flowOn(ioDispatcher)

        override fun observeRecentTransactions(limit: Int): Flow<List<TransactionDetails>> =
            transactionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

        override fun observeTransaction(id: Long): Flow<TransactionDetails?> =
            transactionDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

        override suspend fun getTransaction(id: Long): Transaction? =
            withContext(ioDispatcher) {
                transactionDao.getById(id)?.toDomain()
            }

        override suspend fun getTransactionDetails(id: Long): TransactionDetails? =
            withContext(ioDispatcher) {
                transactionDao.getWithDetailsById(id)?.toDomain()
            }

        override suspend fun saveTransaction(transaction: Transaction): Long =
            withContext(ioDispatcher) {
                if (transaction.id == 0L) {
                    transactionDao.insert(transaction.toEntity())
                } else {
                    transactionDao.update(transaction.toEntity())
                    transaction.id
                }
            }

        override suspend fun deleteTransaction(id: Long) {
            withContext(ioDispatcher) {
                transactionDao.deleteById(id)
            }
        }

        override fun observeMonthSummary(
            from: LocalDate,
            to: LocalDate,
            accountId: Long?,
        ): Flow<MonthSummary> = transactionDao.observeMonthSummary(from, to, accountId).map { it.toDomain() }.flowOn(ioDispatcher)

        override suspend fun getMonthSummary(
            from: LocalDate,
            to: LocalDate,
            accountId: Long?,
        ): MonthSummary =
            withContext(ioDispatcher) {
                transactionDao.getMonthSummary(from, to, accountId).toDomain()
            }
    }
