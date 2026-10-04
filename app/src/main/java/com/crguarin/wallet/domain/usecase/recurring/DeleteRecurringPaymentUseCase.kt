package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements it.
 */
class DeleteRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
    ) {
        suspend operator fun invoke(id: Long): DomainResult<Unit> = error("not implemented")
    }
