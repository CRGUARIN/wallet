package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.DebtDao
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.domain.repository.DebtRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepositoryImpl
    @Inject
    constructor(
        private val debtDao: DebtDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : DebtRepository {
        override fun observeSummaries(): Flow<List<DebtSummary>> =
            debtDao
                .observeAllWithPaid()
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeSummary(id: Long): Flow<DebtSummary?> =
            debtDao
                .observeWithPaid(id)
                .map { it?.toDomain() }
                .flowOn(ioDispatcher)

        override fun observePayments(debtId: Long): Flow<List<DebtPayment>> =
            debtDao
                .observePayments(debtId)
                .map { list -> list.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun getDebt(id: Long): Debt? = debtDao.getById(id)?.toDomain()

        override suspend fun getPaid(debtId: Long): Money = Money(debtDao.getPaid(debtId))

        override suspend fun getPayment(id: Long): DebtPayment? = debtDao.getPaymentById(id)?.toDomain()

        override suspend fun saveDebt(debt: Debt): Long =
            if (debt.id == 0L) {
                debtDao.insert(debt.toEntity())
            } else {
                debtDao.update(debt.toEntity())
                debt.id
            }

        override suspend fun deleteDebt(id: Long) {
            debtDao.deleteById(id)
        }

        override suspend fun insertPayment(payment: DebtPayment): Long = debtDao.insertPayment(payment.toEntity())

        override suspend fun deletePayment(id: Long) {
            debtDao.deletePaymentById(id)
        }
    }
