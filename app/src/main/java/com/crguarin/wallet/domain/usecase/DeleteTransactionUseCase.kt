package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.DeletedTransaction
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * Deletes a transaction and returns what [RestoreTransactionUseCase] needs to undo it, including
 * the card statements whose reconciliation adjustment it was.
 */
class DeleteTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val cardStatementRepository: CardStatementRepository,
        private val transactionRunner: TransactionRunner,
    ) {
        suspend operator fun invoke(transactionId: Long): DomainResult<DeletedTransaction> =
            transactionRunner.run {
                val transaction = transactionRepository.getTransaction(transactionId)
                if (transaction == null) {
                    DomainResult.Failure(DomainError.NotFound)
                } else {
                    val adjustedStatementIds = cardStatementRepository.getStatementIdsAdjustedBy(transaction.id)
                    transactionRepository.deleteTransaction(transaction.id)
                    DomainResult.Success(DeletedTransaction(transaction, adjustedStatementIds))
                }
            }
    }
