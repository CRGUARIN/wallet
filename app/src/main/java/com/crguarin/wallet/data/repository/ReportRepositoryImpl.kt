package com.crguarin.wallet.data.repository

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.CategoryTotalRow
import com.crguarin.wallet.data.db.DailyIncomeExpenseRow
import com.crguarin.wallet.data.db.TransactionDao
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.BalanceDeltas
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.DailyIncomeExpense
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ReportRepository {
        override fun observeCategoryTotals(
            from: LocalDate,
            to: LocalDate,
            type: TransactionType,
            accountId: Long?,
        ): Flow<List<CategoryTotal>> {
            require(type != TransactionType.TRANSFER) { "Transfers have no category totals" }
            return transactionDao
                .observeCategoryTotals(from, to, type, accountId)
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)
        }

        override fun observeDailyIncomeExpense(
            from: LocalDate,
            to: LocalDate,
            accountId: Long?,
        ): Flow<List<DailyIncomeExpense>> =
            transactionDao
                .observeDailyIncomeExpense(from, to, accountId)
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        /** Contract stub: the body is implemented by the "datos y dominio" owner. */
        override fun observeBalanceDeltas(
            accountIds: Set<Long>,
            from: LocalDate,
            to: LocalDate,
        ): Flow<BalanceDeltas> = flowOf(BalanceDeltas.EMPTY)
    }

private fun CategoryTotalRow.toDomain(): CategoryTotal =
    CategoryTotal(
        categoryId = categoryId,
        name = categoryName,
        systemKey = categorySystemKey,
        color = categoryColor,
        icon = categoryIcon,
        parentId = parentId,
        parentName = parentName,
        parentSystemKey = parentSystemKey,
        parentColor = parentColor,
        parentIcon = parentIcon,
        total = Money(total),
        transactionCount = transactionCount,
    )

private fun DailyIncomeExpenseRow.toDomain(): DailyIncomeExpense =
    DailyIncomeExpense(
        date = date,
        income = Money(income),
        expense = Money(expense),
    )
