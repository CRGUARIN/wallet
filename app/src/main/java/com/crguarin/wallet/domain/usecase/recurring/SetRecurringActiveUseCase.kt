package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Pauses (`active = false`) or resumes a recurring payment. Resuming sets
 * `nextDueDate = firstOnOrAfter(today)`, so paused periods are never caught
 * up; it never moves `nextDueDate` backwards over an occurrence that was
 * already recorded or skipped. A finished payment cannot resume without
 * changing its `endDate`.
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
        ): DomainResult<Unit> {
            val payment =
                recurringRepository.get(id)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (!active) {
                recurringRepository.save(payment.copy(isActive = false, updatedAt = timeProvider.now()))
                return DomainResult.Success(Unit)
            }
            if (payment.isFinished) return DomainResult.Failure(DomainError.InvalidTransaction)
            // Paused periods are skipped, but an occurrence already recorded or skipped must not
            // come back (e.g. pausing and resuming on the same day the auto charge ran).
            val nextDueDate =
                maxOf(
                    RecurrenceMath.firstOnOrAfter(payment, timeProvider.today()),
                    payment.nextDueDate,
                )
            recurringRepository.save(payment.copy(isActive = true, nextDueDate = nextDueDate, updatedAt = timeProvider.now()))
            return DomainResult.Success(Unit)
        }
    }
