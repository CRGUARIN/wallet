package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.debt.Debt
import com.crguarin.wallet.domain.repository.DebtRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * Creates or edits a debt. Editing cannot lower the amount below what was already paid, change
 * the direction once there are payments or move the date after the first payment, and keeps
 * the original `createdAt`. Dates may be in the past, but not beyond one year ahead.
 */
class SaveDebtUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) {
        /** Validation and the paid-amount check read the stored debt, so they run in the same transaction as the write. */
        suspend operator fun invoke(debt: Debt): DomainResult<Long> = transactionRunner.run { save(debt) }

        private suspend fun save(debt: Debt): DomainResult<Long> {
            val name = debt.personName.trim()
            if (name.isEmpty()) return DomainResult.Failure(DomainError.NameEmpty)
            if (name.length > DomainLimits.NAME_MAX_LENGTH) return DomainResult.Failure(DomainError.NameTooLong)
            if (debt.amount <= Money.ZERO || debt.amount > Money.MAX) return DomainResult.Failure(DomainError.AmountInvalid)
            val note = debt.note?.trim()?.takeIf { it.isNotEmpty() }
            if (note != null && note.length > DomainLimits.NOTE_MAX_LENGTH) return DomainResult.Failure(DomainError.NoteTooLong)
            val today = timeProvider.today()
            if (debt.date.isAfter(DomainLimits.maxTransactionDate(today))) return DomainResult.Failure(DomainError.DateOutOfRange)
            val dueDate = debt.dueDate
            if (dueDate != null && dueDate.isBefore(debt.date)) return DomainResult.Failure(DomainError.DebtDueBeforeDate)

            val now = timeProvider.now()
            if (debt.id == 0L) {
                val id = debtRepository.saveDebt(debt.copy(personName = name, note = note, createdAt = now, updatedAt = now))
                return DomainResult.Success(id)
            }
            val existing = debtRepository.getDebt(debt.id) ?: return DomainResult.Failure(DomainError.NotFound)
            val paid = debtRepository.getPaid(debt.id)
            if (debt.amount < paid) return DomainResult.Failure(DomainError.DebtAmountBelowPaid)
            if (debt.direction != existing.direction && paid.isPositive) {
                return DomainResult.Failure(DomainError.DebtDirectionLocked)
            }
            val firstPayment = debtRepository.getFirstPaymentDate(debt.id)
            if (firstPayment != null && debt.date.isAfter(firstPayment)) {
                return DomainResult.Failure(DomainError.DebtDateAfterPayments)
            }
            val id =
                debtRepository.saveDebt(
                    debt.copy(personName = name, note = note, createdAt = existing.createdAt, updatedAt = now),
                )
            return DomainResult.Success(id)
        }
    }
