package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.debt.DebtPayment
import com.crguarin.wallet.domain.repository.DebtRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import java.time.LocalDate
import javax.inject.Inject

/**
 * Records a partial payment ("abono") of a debt. The amount may not exceed what is still
 * outstanding; a payment that equals the outstanding amount settles the debt. It cannot be
 * dated before the debt.
 */
class AddDebtPaymentUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) {
        suspend operator fun invoke(
            debtId: Long,
            amount: Money,
            date: LocalDate,
            note: String?,
        ): DomainResult<Long> = transactionRunner.run { addPayment(debtId, amount, date, note) }

        private suspend fun addPayment(
            debtId: Long,
            amount: Money,
            date: LocalDate,
            note: String?,
        ): DomainResult<Long> {
            val debt = debtRepository.getDebt(debtId) ?: return DomainResult.Failure(DomainError.NotFound)
            val remaining = debt.amount - debtRepository.getPaid(debtId)
            if (!remaining.isPositive) return DomainResult.Failure(DomainError.DebtAlreadySettled)
            if (amount <= Money.ZERO) return DomainResult.Failure(DomainError.AmountInvalid)
            if (amount > remaining) return DomainResult.Failure(DomainError.DebtPaymentExceedsRemaining)
            val trimmedNote = note?.trim()?.takeIf { it.isNotEmpty() }
            if (trimmedNote != null && trimmedNote.length > DomainLimits.NOTE_MAX_LENGTH) {
                return DomainResult.Failure(DomainError.NoteTooLong)
            }
            if (date.isAfter(DomainLimits.maxTransactionDate(timeProvider.today()))) {
                return DomainResult.Failure(DomainError.DateOutOfRange)
            }
            if (date.isBefore(debt.date)) return DomainResult.Failure(DomainError.DebtPaymentBeforeDate)
            val id =
                debtRepository.insertPayment(
                    DebtPayment(
                        debtId = debtId,
                        amount = amount,
                        date = date,
                        note = trimmedNote,
                        createdAt = timeProvider.now(),
                    ),
                )
            return DomainResult.Success(id)
        }
    }
