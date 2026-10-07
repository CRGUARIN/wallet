package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.repository.CardStatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeCardStatementRepository : CardStatementRepository {
    private val statements = mutableMapOf<Long, CardStatement>()
    private val transactions = mutableMapOf<Long, Transaction>()
    private val statementsFlow = MutableStateFlow<List<CardStatement>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextStatementId: Long = 1L
    private var nextTransactionId: Long = 1L

    fun setInitialData(
        statementList: List<CardStatement>,
        transactionList: List<Transaction> = emptyList(),
    ) {
        statements.clear()
        transactions.clear()
        statementList.forEach { statement ->
            statements[statement.id] = statement
            if (statement.id >= nextStatementId) nextStatementId = statement.id + 1
        }
        transactionList.forEach { tx ->
            transactions[tx.id] = tx
            if (tx.id >= nextTransactionId) nextTransactionId = tx.id + 1
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        statementsFlow.value =
            statements.values.sortedWith(
                compareByDescending<CardStatement> { it.closingDate }.thenByDescending { it.id },
            )
        transactionsFlow.value =
            transactions.values.sortedWith(
                compareBy<Transaction> { it.date }.thenBy { it.id },
            )
    }

    override fun observeStatements(accountId: Long): Flow<List<CardStatement>> =
        statementsFlow.map { list -> list.filter { it.accountId == accountId } }

    override fun observeLastStatement(accountId: Long): Flow<CardStatement?> =
        statementsFlow.map { list -> list.firstOrNull { it.accountId == accountId } }

    override fun observeAdjustmentAmounts(accountId: Long): Flow<Map<Long, Money>> =
        combine(statementsFlow, transactionsFlow) { statementList, transactionList ->
            val amounts = transactionList.associate { it.id to it.amount }
            statementList
                .filter { it.accountId == accountId }
                .mapNotNull { statement ->
                    val amount = statement.adjustmentTransactionId?.let { amounts[it] } ?: return@mapNotNull null
                    statement.id to amount
                }.toMap()
        }

    override fun observeCardTransactions(
        accountId: Long,
        from: LocalDate,
    ): Flow<List<Transaction>> =
        transactionsFlow.map { list ->
            list.filter {
                (it.accountId == accountId || it.toAccountId == accountId) && it.date >= from
            }
        }

    override suspend fun getAccountEffectUpTo(
        accountId: Long,
        date: LocalDate,
    ): Money =
        transactions.values
            .filter { it.date <= date }
            .fold(Money.ZERO) { acc, tx -> acc + signedEffect(tx, accountId) }

    override suspend fun getStatementIdsAdjustedBy(transactionId: Long): List<Long> =
        statements.values
            .filter { it.adjustmentTransactionId == transactionId }
            .map { it.id }
            .sorted()

    override suspend fun relinkAdjustment(
        transactionId: Long,
        statementIds: List<Long>,
    ) {
        for (id in statementIds) {
            val statement = statements[id] ?: continue
            if (statement.adjustmentTransactionId == null) {
                statements[id] = statement.copy(adjustmentTransactionId = transactionId)
            }
        }
        notifyChanged()
    }

    /** Mirrors the ON DELETE SET NULL of the adjustment link when the adjustment is deleted elsewhere. */
    fun clearAdjustment(transactionId: Long) {
        statements.replaceAll { _, statement ->
            if (statement.adjustmentTransactionId == transactionId) statement.copy(adjustmentTransactionId = null) else statement
        }
        notifyChanged()
    }

    override suspend fun saveReconciliation(
        statement: CardStatement,
        adjustment: Transaction?,
    ): Long {
        val adjustmentId =
            if (adjustment != null) {
                val id = nextTransactionId++
                transactions[id] = adjustment.copy(id = id)
                id
            } else {
                statements.values
                    .firstOrNull { it.accountId == statement.accountId && it.closingDate == statement.closingDate }
                    ?.adjustmentTransactionId
            }
        val existing =
            statements.values
                .firstOrNull { it.accountId == statement.accountId && it.closingDate == statement.closingDate }
        val id = existing?.id ?: nextStatementId++
        statements[id] = statement.copy(id = id, adjustmentTransactionId = adjustmentId)
        notifyChanged()
        return id
    }

    private fun signedEffect(
        tx: Transaction,
        accountId: Long,
    ): Money =
        when {
            tx.type == TransactionType.INCOME && tx.accountId == accountId -> tx.amount
            tx.type == TransactionType.EXPENSE && tx.accountId == accountId -> -tx.amount
            tx.type == TransactionType.TRANSFER && tx.accountId == accountId -> -tx.amount
            tx.type == TransactionType.TRANSFER && tx.toAccountId == accountId -> tx.amount
            else -> Money.ZERO
        }
}
