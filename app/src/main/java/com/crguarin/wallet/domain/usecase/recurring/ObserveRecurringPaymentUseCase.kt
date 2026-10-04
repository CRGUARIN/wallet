package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements it.
 */
class ObserveRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(id: Long): Flow<RecurringItem?> = flowOf(null)
    }
