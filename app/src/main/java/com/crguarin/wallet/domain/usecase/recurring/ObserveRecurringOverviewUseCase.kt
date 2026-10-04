package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.model.recurring.RecurringOverview
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements the pending/active/inactive sections.
 */
class ObserveRecurringOverviewUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(): Flow<RecurringOverview> =
            flowOf(
                RecurringOverview(
                    pending = emptyList<RecurringItem>(),
                    active = emptyList(),
                    inactive = emptyList(),
                    monthlyExpense = Money.ZERO,
                    monthlyIncome = Money.ZERO,
                ),
            )
    }
