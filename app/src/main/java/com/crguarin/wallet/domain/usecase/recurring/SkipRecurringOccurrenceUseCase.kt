package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Skips the oldest pending occurrence of a payment: `nextDueDate` advances
 * without recording anything. A concurrent change
 * ([OccurrenceResult.AlreadyHandled]) surfaces as [DomainError.NotFound].
 */
class SkipRecurringOccurrenceUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(id: Long): DomainResult<Unit> {
            val payment =
                recurringRepository.get(id)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            val nextDueDate = RecurrenceMath.nextAfter(payment, payment.nextDueDate)
            return when (
                recurringRepository.completeOccurrence(payment.id, payment.nextDueDate, null, nextDueDate)
            ) {
                is OccurrenceResult.Skipped -> DomainResult.Success(Unit)
                is OccurrenceResult.Recorded -> DomainResult.Success(Unit)
                is OccurrenceResult.AlreadyHandled -> DomainResult.Failure(DomainError.NotFound)
            }
        }
    }
