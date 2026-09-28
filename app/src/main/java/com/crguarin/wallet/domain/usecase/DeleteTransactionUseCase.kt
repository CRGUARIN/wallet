package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(transactionId: Long): DomainResult<Unit> {
            val transaction =
                transactionRepository.getTransaction(transactionId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            transactionRepository.deleteTransaction(transaction.id)
            return DomainResult.Success(Unit)
        }
    }
