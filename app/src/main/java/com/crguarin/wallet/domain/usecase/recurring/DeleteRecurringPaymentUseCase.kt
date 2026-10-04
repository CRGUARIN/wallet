package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Deletes a recurring payment rule. Already generated transactions are kept
 * (their `recurringId` is cleared by the repository, in the same transaction).
 */
class DeleteRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
    ) {
        suspend operator fun invoke(id: Long): DomainResult<Unit> {
            recurringRepository.get(id) ?: return DomainResult.Failure(DomainError.NotFound)
            recurringRepository.delete(id)
            return DomainResult.Success(Unit)
        }
    }
