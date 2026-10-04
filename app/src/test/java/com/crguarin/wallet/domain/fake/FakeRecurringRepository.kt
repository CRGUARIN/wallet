package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * In-memory [RecurringRepository]. [completeOccurrence] enforces the optimistic
 * expected-date check like the real implementation; generated transactions are
 * served from [generatedTransactions] (newest first, honoring [limit]).
 */
class FakeRecurringRepository : RecurringRepository {
    private val payments = mutableMapOf<Long, RecurringPayment>()
    private val paymentsFlow = MutableStateFlow<List<RecurringPayment>>(emptyList())
    private var nextId: Long = 1L

    var generatedTransactions: List<TransactionDetails> = emptyList()

    fun setInitialData(paymentList: List<RecurringPayment>) {
        payments.clear()
        paymentList.forEach { payment ->
            payments[payment.id] = payment
            if (payment.id >= nextId) nextId = payment.id + 1
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        paymentsFlow.value = payments.values.sortedBy { it.id }
    }

    override fun observeAll(): Flow<List<RecurringPayment>> = paymentsFlow.asStateFlow()

    override fun observe(id: Long): Flow<RecurringPayment?> = paymentsFlow.map { list -> list.find { it.id == id } }

    override suspend fun get(id: Long): RecurringPayment? = payments[id]

    override suspend fun getAll(): List<RecurringPayment> = payments.values.sortedBy { it.id }

    override suspend fun save(payment: RecurringPayment): Long {
        val id = if (payment.id == 0L) nextId++ else payment.id
        payments[id] = payment.copy(id = id)
        notifyChanged()
        return id
    }

    override suspend fun delete(id: Long) {
        payments.remove(id)
        notifyChanged()
    }

    override suspend fun completeOccurrence(
        paymentId: Long,
        expectedDueDate: LocalDate,
        transaction: Transaction?,
        newNextDueDate: LocalDate,
    ): OccurrenceResult {
        val stored = payments[paymentId] ?: return OccurrenceResult.AlreadyHandled
        if (stored.nextDueDate != expectedDueDate) return OccurrenceResult.AlreadyHandled
        payments[paymentId] = stored.copy(nextDueDate = newNextDueDate, updatedAt = stored.updatedAt)
        notifyChanged()
        return if (transaction == null) {
            OccurrenceResult.Skipped
        } else {
            OccurrenceResult.Recorded(transactionId = transaction.id)
        }
    }

    override fun observeGeneratedTransactions(
        paymentId: Long,
        limit: Int,
    ): Flow<List<TransactionDetails>> =
        paymentsFlow.map {
            generatedTransactions
                .filter { details -> details.transaction.recurringId == paymentId }
                .take(limit)
        }

    override suspend fun countByAccount(accountId: Long): Int = payments.values.count { it.accountId == accountId }

    override suspend fun countByCategory(categoryId: Long): Int = payments.values.count { it.categoryId == categoryId }
}
