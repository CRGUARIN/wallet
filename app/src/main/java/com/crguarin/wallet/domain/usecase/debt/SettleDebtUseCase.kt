package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.repository.DebtRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * Settles a debt in one step: records a payment of exactly the outstanding amount, dated today
 * (or on the debt date, if that is later).
 */
class SettleDebtUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) {
        suspend operator fun invoke(debtId: Long): DomainResult<Long> = transactionRunner.run { settle(debtId) }

        private suspend fun settle(debtId: Long): DomainResult<Long> {
            val debt = debtRepository.getDebt(debtId) ?: return DomainResult.Failure(DomainError.NotFound)
            val remaining = debt.amount - debtRepository.getPaid(debtId)
            if (!remaining.isPositive) return DomainResult.Failure(DomainError.DebtAlreadySettled)
            val id =
                debtRepository.insertPayment(
                    DebtPayment(
                        debtId = debtId,
                        amount = remaining,
                        // A debt dated in the future is settled on its own date, never before it.
                        date = maxOf(timeProvider.today(), debt.date),
                        note = null,
                        createdAt = timeProvider.now(),
                    ),
                )
            return DomainResult.Success(id)
        }
    }
