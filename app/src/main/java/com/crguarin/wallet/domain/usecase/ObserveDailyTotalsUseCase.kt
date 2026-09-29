package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Net amount per day for the given filter. Used to show the total of each day header in a
 * transactions list grouped by day.
 */
class ObserveDailyTotalsUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        operator fun invoke(filter: TransactionFilter = TransactionFilter()): Flow<Map<LocalDate, Money>> =
            transactionRepository.observeDailyTotals(filter)
    }
