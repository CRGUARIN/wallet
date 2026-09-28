package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject

class ObserveMonthSummaryUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        operator fun invoke(
            yearMonth: YearMonth,
            accountId: Long? = null,
        ): Flow<MonthSummary> {
            val from = yearMonth.atDay(1)
            val to = yearMonth.atEndOfMonth()
            return transactionRepository.observeMonthSummary(from, to, accountId)
        }
    }
