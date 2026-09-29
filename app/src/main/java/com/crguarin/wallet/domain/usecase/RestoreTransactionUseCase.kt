package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Restores a deleted transaction preserving its id, dates, note, accounts and category, so the
 * "undo delete" action can put it back exactly as it was.
 */
class RestoreTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(transaction: Transaction): DomainResult<Unit> {
            transactionRepository.restoreTransaction(transaction)
            return DomainResult.Success(Unit)
        }
    }
