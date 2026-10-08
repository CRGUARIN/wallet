package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [DebtRepository]. Observers re-read the maps whenever [changes] is bumped. Payments
 * are served newest first, and deleting a debt also deletes its payments, like the foreign key
 * CASCADE does in the real database.
 */
class FakeDebtRepository : DebtRepository {
    private val debts = mutableMapOf<Long, Debt>()
    private val payments = mutableMapOf<Long, DebtPayment>()
    private val changes = MutableStateFlow(0)
    private var nextDebtId: Long = 1L
    private var nextPaymentId: Long = 1L

    fun setInitialData(
        debtList: List<Debt>,
        paymentList: List<DebtPayment> = emptyList(),
    ) {
        debts.clear()
        payments.clear()
        debtList.forEach { debt ->
            debts[debt.id] = debt
            if (debt.id >= nextDebtId) nextDebtId = debt.id + 1
        }
        paymentList.forEach { payment ->
            payments[payment.id] = payment
            if (payment.id >= nextPaymentId) nextPaymentId = payment.id + 1
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        changes.value += 1
    }

    private fun paidOf(debtId: Long): Money = paymentsOf(debtId).fold(Money.ZERO) { acc, payment -> acc + payment.amount }

    private fun summaryOf(debt: Debt): DebtSummary = DebtSummary(debt = debt, paid = paidOf(debt.id))

    private fun paymentsOf(debtId: Long): List<DebtPayment> =
        payments.values
            .filter { it.debtId == debtId }
            .sortedWith(compareByDescending<DebtPayment> { it.date }.thenByDescending { it.id })

    override fun observeSummaries(): Flow<List<DebtSummary>> =
        changes.map {
            debts.values
                .sortedWith(compareBy<Debt> { it.date }.thenBy { it.id })
                .map { summaryOf(it) }
        }

    override fun observeSummary(id: Long): Flow<DebtSummary?> = changes.map { debts[id]?.let { summaryOf(it) } }

    override fun observePayments(debtId: Long): Flow<List<DebtPayment>> = changes.map { paymentsOf(debtId) }

    override suspend fun getDebt(id: Long): Debt? = debts[id]

    override suspend fun getPaid(debtId: Long): Money = paidOf(debtId)

    override suspend fun getPayment(id: Long): DebtPayment? = payments[id]

    override suspend fun saveDebt(debt: Debt): Long {
        val id = if (debt.id == 0L) nextDebtId++ else debt.id
        debts[id] = debt.copy(id = id)
        notifyChanged()
        return id
    }

    override suspend fun deleteDebt(id: Long) {
        debts.remove(id)
        payments.values.removeAll { it.debtId == id }
        notifyChanged()
    }

    override suspend fun insertPayment(payment: DebtPayment): Long {
        require(debts.containsKey(payment.debtId)) { "Foreign key: debt ${payment.debtId} does not exist" }
        val id = nextPaymentId++
        payments[id] = payment.copy(id = id)
        notifyChanged()
        return id
    }

    override suspend fun deletePayment(id: Long) {
        payments.remove(id)
        notifyChanged()
    }
}
