package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.model.debt.DebtSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DebtRepository {
    /** Every debt with the sum of its payments. */
    fun observeSummaries(): Flow<List<DebtSummary>>

    fun observeSummary(id: Long): Flow<DebtSummary?>

    /** Payments of a debt, newest first (date DESC, id DESC). */
    fun observePayments(debtId: Long): Flow<List<DebtPayment>>

    suspend fun getDebt(id: Long): Debt?

    suspend fun getPaid(debtId: Long): Money

    suspend fun getPayment(id: Long): DebtPayment?

    /** Date of the oldest payment of the debt, or null when it has none. */
    suspend fun getFirstPaymentDate(debtId: Long): LocalDate?

    suspend fun countDebts(): Int

    /** Inserts when `debt.id == 0`, otherwise updates. Returns the id. */
    suspend fun saveDebt(debt: Debt): Long

    /** Deletes the debt; its payments are deleted by the foreign key (CASCADE). */
    suspend fun deleteDebt(id: Long)

    suspend fun insertPayment(payment: DebtPayment): Long

    suspend fun deletePayment(id: Long)
}
