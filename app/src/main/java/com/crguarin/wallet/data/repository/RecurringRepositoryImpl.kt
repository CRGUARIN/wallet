package com.crguarin.wallet.data.repository

import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provisional stub: Task 16 (data) replaces it with the Room implementation.
 */
@Singleton
class RecurringRepositoryImpl
    @Inject
    constructor() : RecurringRepository {
        override fun observeAll(): Flow<List<RecurringPayment>> = flowOf(emptyList())

        override fun observe(id: Long): Flow<RecurringPayment?> = flowOf(null)

        override suspend fun get(id: Long): RecurringPayment? = null

        override suspend fun getAll(): List<RecurringPayment> = emptyList()

        override suspend fun save(payment: RecurringPayment): Long = error("not implemented")

        override suspend fun delete(id: Long): Unit = error("not implemented")

        override suspend fun completeOccurrence(
            paymentId: Long,
            expectedDueDate: LocalDate,
            transaction: Transaction?,
            newNextDueDate: LocalDate,
        ): OccurrenceResult = error("not implemented")

        override fun observeGeneratedTransactions(
            paymentId: Long,
            limit: Int,
        ): Flow<List<TransactionDetails>> = flowOf(emptyList())

        override suspend fun countByAccount(accountId: Long): Int = 0

        override suspend fun countByCategory(categoryId: Long): Int = 0
    }
