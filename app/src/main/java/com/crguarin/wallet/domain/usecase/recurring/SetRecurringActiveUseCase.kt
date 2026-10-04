package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements pause/resume with nextDueDate recalculation.
 */
class SetRecurringActiveUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(
            id: Long,
            active: Boolean,
        ): DomainResult<Unit> = error("not implemented")
    }
