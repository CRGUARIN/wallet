package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.repository.CardStatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provisional implementation of [CardStatementRepository]; replaced with the Room-backed
 * implementation in the credit card task.
 */
@Singleton
class CardStatementRepositoryImpl
    @Inject
    constructor() : CardStatementRepository {
        override fun observeStatements(accountId: Long): Flow<List<CardStatement>> = flowOf(emptyList())

        override fun observeLastStatement(accountId: Long): Flow<CardStatement?> = flowOf(null)

        override fun observeCardTransactions(
            accountId: Long,
            from: LocalDate,
        ): Flow<List<Transaction>> = flowOf(emptyList())

        override suspend fun getAccountEffectUpTo(
            accountId: Long,
            date: LocalDate,
        ): Money = Money.ZERO

        override suspend fun saveReconciliation(
            statement: CardStatement,
            adjustment: Transaction?,
        ): Long = error("not implemented")
    }
