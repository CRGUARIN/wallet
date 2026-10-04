package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements the 7-day upcoming list (recurring + cards).
 */
class ObserveUpcomingPaymentsUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(days: Int = 7): Flow<List<UpcomingPayment>> = flowOf(emptyList())
    }
