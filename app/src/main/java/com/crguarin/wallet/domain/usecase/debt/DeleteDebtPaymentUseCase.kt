package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Deletes one payment of a debt, which raises the outstanding amount again.
 */
class DeleteDebtPaymentUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
    ) {
        suspend operator fun invoke(paymentId: Long): DomainResult<Unit> {
            debtRepository.getPayment(paymentId) ?: return DomainResult.Failure(DomainError.NotFound)
            debtRepository.deletePayment(paymentId)
            return DomainResult.Success(Unit)
        }
    }
