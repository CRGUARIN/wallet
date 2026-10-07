package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.card.CardStatement
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CardStatementRepository {
    /** Statements of the account, newest closing first. */
    fun observeStatements(accountId: Long): Flow<List<CardStatement>>

    fun observeLastStatement(accountId: Long): Flow<CardStatement?>

    /** Amount of the adjustment movement of every statement of the account that has one, by statement id. */
    fun observeAdjustmentAmounts(accountId: Long): Flow<Map<Long, Money>>

    /** Transactions with account_id = card OR to_account_id = card, date >= from. */
    fun observeCardTransactions(
        accountId: Long,
        from: LocalDate,
    ): Flow<List<Transaction>>

    /** Signed sum of effects with date <= date (without initialBalance). */
    suspend fun getAccountEffectUpTo(
        accountId: Long,
        date: LocalDate,
    ): Money

    /**
     * Saves [statement] (upsert by accountId + closingDate) and, if not null, inserts [adjustment]
     * first and links it; all in one DB transaction. Returns the statement id.
     */
    suspend fun saveReconciliation(
        statement: CardStatement,
        adjustment: Transaction?,
    ): Long
}
