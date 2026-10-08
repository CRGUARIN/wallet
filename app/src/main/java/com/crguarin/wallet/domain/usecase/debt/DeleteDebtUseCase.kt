package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Deletes a debt. Its payments are removed by the repository (foreign key CASCADE).
 */
class DeleteDebtUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
    ) {
        suspend operator fun invoke(id: Long): DomainResult<Unit> {
            debtRepository.getDebt(id) ?: return DomainResult.Failure(DomainError.NotFound)
            debtRepository.deleteDebt(id)
            return DomainResult.Success(Unit)
        }
    }
