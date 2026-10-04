package com.crguarin.wallet.data.repository

import androidx.room.withTransaction
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.CardStatementDao
import com.crguarin.wallet.data.db.TransactionDao
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.repository.CardStatementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardStatementRepositoryImpl
    @Inject
    constructor(
        private val walletDatabase: WalletDatabase,
        private val cardStatementDao: CardStatementDao,
        private val transactionDao: TransactionDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CardStatementRepository {
        override fun observeStatements(accountId: Long): Flow<List<CardStatement>> =
            cardStatementDao
                .observeByAccount(accountId)
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeLastStatement(accountId: Long): Flow<CardStatement?> =
            cardStatementDao
                .observeLastByAccount(accountId)
                .map { it?.toDomain() }
                .flowOn(ioDispatcher)

        override fun observeCardTransactions(
            accountId: Long,
            from: LocalDate,
        ): Flow<List<Transaction>> =
            transactionDao
                .observeCardTransactions(accountId, from)
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun getAccountEffectUpTo(
            accountId: Long,
            date: LocalDate,
        ): Money =
            withContext(ioDispatcher) {
                Money(transactionDao.getAccountEffectUpTo(accountId, date))
            }

        override suspend fun saveReconciliation(
            statement: CardStatement,
            adjustment: Transaction?,
        ): Long =
            withContext(ioDispatcher) {
                walletDatabase.withTransaction {
                    var adjustmentId: Long? = null
                    if (adjustment != null) {
                        adjustmentId = transactionDao.insert(adjustment.toEntity().copy(id = 0))
                    }
                    val existing =
                        cardStatementDao.getByAccountAndClosing(
                            statement.accountId,
                            statement.closingDate,
                        )
                    if (existing == null) {
                        cardStatementDao.insert(
                            statement.toEntity().copy(id = 0, adjustmentTransactionId = adjustmentId),
                        )
                    } else {
                        val linkedAdjustmentId = adjustmentId ?: existing.adjustmentTransactionId
                        cardStatementDao.update(
                            statement.toEntity().copy(
                                id = existing.id,
                                adjustmentTransactionId = linkedAdjustmentId,
                            ),
                        )
                        existing.id
                    }
                }
            }
    }
