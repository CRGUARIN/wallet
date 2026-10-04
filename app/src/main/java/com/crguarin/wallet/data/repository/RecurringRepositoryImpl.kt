package com.crguarin.wallet.data.repository

import androidx.room.withTransaction
import com.crguarin.wallet.data.db.RecurringPaymentDao
import com.crguarin.wallet.data.db.TransactionDao
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl
    @Inject
    constructor(
        private val recurringPaymentDao: RecurringPaymentDao,
        private val transactionDao: TransactionDao,
        private val walletDatabase: WalletDatabase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : RecurringRepository {
        override fun observeAll(): Flow<List<RecurringPayment>> =
            recurringPaymentDao
                .observeAll()
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observe(id: Long): Flow<RecurringPayment?> =
            recurringPaymentDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

        override suspend fun get(id: Long): RecurringPayment? =
            withContext(ioDispatcher) {
                recurringPaymentDao.getById(id)?.toDomain()
            }

        override suspend fun getAll(): List<RecurringPayment> =
            withContext(ioDispatcher) {
                recurringPaymentDao.getAll().map { it.toDomain() }
            }

        override suspend fun save(payment: RecurringPayment): Long =
            withContext(ioDispatcher) {
                if (payment.id == 0L) {
                    recurringPaymentDao.insert(payment.toEntity())
                } else {
                    recurringPaymentDao.update(payment.toEntity())
                    payment.id
                }
            }

        override suspend fun delete(id: Long) {
            withContext(ioDispatcher) {
                walletDatabase.withTransaction {
                    transactionDao.clearRecurringId(id)
                    recurringPaymentDao.deleteById(id)
                }
            }
        }

        override suspend fun completeOccurrence(
            paymentId: Long,
            expectedDueDate: LocalDate,
            transaction: Transaction?,
            newNextDueDate: LocalDate,
        ): OccurrenceResult =
            withContext(ioDispatcher) {
                walletDatabase.withTransaction {
                    val stored = recurringPaymentDao.getById(paymentId)
                    if (stored == null || stored.nextDueDate != expectedDueDate) {
                        return@withTransaction OccurrenceResult.AlreadyHandled
                    }
                    if (transaction == null) {
                        recurringPaymentDao.updateNextDueDate(
                            id = paymentId,
                            nextDueDate = newNextDueDate,
                            updatedAt = stored.updatedAt,
                        )
                        return@withTransaction OccurrenceResult.Skipped
                    }
                    val transactionId =
                        transactionDao.insert(
                            transaction.toEntity().copy(id = 0, recurringId = paymentId),
                        )
                    recurringPaymentDao.updateNextDueDate(
                        id = paymentId,
                        nextDueDate = newNextDueDate,
                        updatedAt = transaction.updatedAt,
                    )
                    OccurrenceResult.Recorded(transactionId)
                }
            }

        override fun observeGeneratedTransactions(
            paymentId: Long,
            limit: Int,
        ): Flow<List<TransactionDetails>> =
            transactionDao
                .observeGeneratedByPayment(paymentId, limit)
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun countByAccount(accountId: Long): Int =
            withContext(ioDispatcher) {
                recurringPaymentDao.countByAccount(accountId)
            }

        override suspend fun countByCategory(categoryId: Long): Int =
            withContext(ioDispatcher) {
                recurringPaymentDao.countByCategory(categoryId)
            }
    }
