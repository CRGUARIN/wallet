package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.DeletedTransaction
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * Restores a deleted transaction preserving its id, dates, note, accounts and category, so the
 * "undo delete" action can put it back exactly as it was. A card reconciliation adjustment is
 * linked again to the statements it adjusted.
 */
class RestoreTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val cardStatementRepository: CardStatementRepository,
        private val transactionRunner: TransactionRunner,
    ) {
        suspend operator fun invoke(deleted: DeletedTransaction): DomainResult<Unit> =
            transactionRunner.run {
                if (transactionRepository.restoreTransaction(deleted.transaction)) {
                    cardStatementRepository.relinkAdjustment(deleted.transaction.id, deleted.adjustedStatementIds)
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Failure(DomainError.NotFound)
                }
            }
    }
