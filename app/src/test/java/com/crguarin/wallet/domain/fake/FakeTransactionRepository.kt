package com.crguarin.wallet.domain.fake

import androidx.paging.PagingData
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<Long, Transaction>()
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId: Long = 1L

    /** Number of times the paged query factory was invoked (used to assert search debounce). */
    var observeTransactionsPagedCallCount: Int = 0
        private set

    /** Last filter passed to the paged query. */
    var lastPagedFilter: TransactionFilter? = null
        private set

    fun setInitialData(transactionList: List<Transaction>) {
        transactions.clear()
        transactionList.forEach { tx ->
            transactions[tx.id] = tx
            if (tx.id >= nextId) nextId = tx.id + 1
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        transactionsFlow.value =
            transactions.values.sortedWith(
                compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt },
            )
    }

    private fun matches(
        tx: Transaction,
        filter: TransactionFilter,
    ): Boolean =
        (filter.startDate == null || tx.date >= filter.startDate) &&
            (filter.endDate == null || tx.date <= filter.endDate) &&
            (filter.accountId == null || tx.accountId == filter.accountId || tx.toAccountId == filter.accountId) &&
            (filter.categoryId == null || tx.categoryId == filter.categoryId) &&
            (filter.type == null || tx.type == filter.type) &&
            (
                filter.noteQuery.isNullOrEmpty() ||
                    (tx.note != null && tx.note.contains(filter.noteQuery, ignoreCase = true))
            )

    override fun observeTransactions(filter: TransactionFilter): Flow<List<TransactionDetails>> =
        transactionsFlow.map { list -> list.filter { matches(it, filter) }.map { toDetails(it) } }

    override fun observeTransactionsPaged(filter: TransactionFilter): Flow<PagingData<TransactionDetails>> {
        observeTransactionsPagedCallCount++
        lastPagedFilter = filter
        return flowOf(PagingData.from(transactions.values.filter { matches(it, filter) }.map { toDetails(it) }))
    }

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionDetails>> =
        transactionsFlow.map { list -> list.take(limit).map { toDetails(it) } }

    override fun observeDailyTotals(filter: TransactionFilter): Flow<Map<LocalDate, Money>> =
        transactionsFlow.map { list ->
            list
                .filter { matches(it, filter) }
                .groupBy { it.date }
                .mapValues { (_, dayTransactions) ->
                    dayTransactions.fold(Money.ZERO) { acc, tx -> acc + signedAmount(tx, filter.accountId) }
                }
        }

    private fun signedAmount(
        tx: Transaction,
        perspectiveAccountId: Long?,
    ): Money =
        when (tx.type) {
            TransactionType.INCOME -> tx.amount
            TransactionType.EXPENSE -> -tx.amount
            TransactionType.TRANSFER ->
                when {
                    perspectiveAccountId == null -> Money.ZERO
                    tx.accountId == perspectiveAccountId -> -tx.amount
                    tx.toAccountId == perspectiveAccountId -> tx.amount
                    else -> Money.ZERO
                }
        }

    override fun observeTransaction(id: Long): Flow<TransactionDetails?> = transactionsFlow.map { transactions[id]?.let { toDetails(it) } }

    override suspend fun getTransaction(id: Long): Transaction? = transactions[id]

    override suspend fun getTransactionDetails(id: Long): TransactionDetails? = transactions[id]?.let { toDetails(it) }

    override suspend fun saveTransaction(transaction: Transaction): Long {
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        val saved = transaction.copy(id = id)
        transactions[id] = saved
        notifyChanged()
        return id
    }

    override suspend fun restoreTransaction(transaction: Transaction) {
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        transactions[id] = transaction.copy(id = id)
        if (id >= nextId) nextId = id + 1
        notifyChanged()
    }

    override suspend fun deleteTransaction(id: Long) {
        transactions.remove(id)
        notifyChanged()
    }

    override fun observeMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): Flow<MonthSummary> =
        transactionsFlow.map { list ->
            calculateSummary(list, from, to, accountId)
        }

    override suspend fun getMonthSummary(
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): MonthSummary = calculateSummary(transactions.values.toList(), from, to, accountId)

    private fun calculateSummary(
        list: List<Transaction>,
        from: LocalDate,
        to: LocalDate,
        accountId: Long?,
    ): MonthSummary {
        val filtered =
            list.filter { tx ->
                tx.date in from..to && (accountId == null || tx.accountId == accountId)
            }
        val income =
            filtered
                .filter { it.type == TransactionType.INCOME }
                .fold(Money.ZERO) { acc, tx -> acc + tx.amount }
        val expense =
            filtered
                .filter { it.type == TransactionType.EXPENSE }
                .fold(Money.ZERO) { acc, tx -> acc + tx.amount }
        return MonthSummary(income = income, expense = expense)
    }

    private fun toDetails(tx: Transaction): TransactionDetails =
        TransactionDetails(
            transaction = tx,
            accountName = "Account ${tx.accountId}",
            accountColor = 0xFF123456.toInt(),
            accountIcon = "wallet",
            toAccountName = tx.toAccountId?.let { "Account $it" },
            toAccountColor = tx.toAccountId?.let { 0xFF654321.toInt() },
            toAccountIcon = tx.toAccountId?.let { "savings" },
            categoryName = tx.categoryId?.let { "Category $it" },
        )
}
