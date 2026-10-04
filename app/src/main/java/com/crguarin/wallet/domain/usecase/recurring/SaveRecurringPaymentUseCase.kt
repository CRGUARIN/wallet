package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements creation/update validation and nextDueDate rules.
 */
class SaveRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(payment: RecurringPayment): DomainResult<Long> = error("not implemented")
    }
