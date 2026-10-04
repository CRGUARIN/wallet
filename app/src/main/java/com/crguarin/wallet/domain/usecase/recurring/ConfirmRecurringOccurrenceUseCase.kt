package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Records the oldest pending occurrence of a CONFIRM payment with an edited
 * amount and date, and advances `nextDueDate`. A concurrent change
 * ([OccurrenceResult.AlreadyHandled]) surfaces as [DomainError.NotFound].
 */
class ConfirmRecurringOccurrenceUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(
            id: Long,
            amount: Money,
            date: LocalDate,
        ): DomainResult<Long> {
            val payment =
                recurringRepository.get(id)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (amount <= Money.ZERO || amount > Money.MAX) {
                return DomainResult.Failure(DomainError.AmountInvalid)
            }
            if (date.isAfter(DomainLimits.maxTransactionDate(timeProvider.today()))) {
                return DomainResult.Failure(DomainError.DateOutOfRange)
            }
            val now = timeProvider.now()
            val transaction =
                Transaction(
                    type = payment.type,
                    amount = amount,
                    accountId = payment.accountId,
                    categoryId = payment.categoryId,
                    date = date,
                    note = payment.note ?: payment.name,
                    createdAt = now,
                    updatedAt = now,
                    recurringId = payment.id,
                )
            val validated =
                when (val result = saveTransactionUseCase.validate(transaction)) {
                    is DomainResult.Success -> result.value
                    is DomainResult.Failure -> return result
                }
            val nextDueDate = RecurrenceMath.nextAfter(payment, payment.nextDueDate)
            return when (
                val result = recurringRepository.completeOccurrence(payment.id, payment.nextDueDate, validated, nextDueDate)
            ) {
                is OccurrenceResult.Recorded -> DomainResult.Success(result.transactionId)
                is OccurrenceResult.Skipped -> DomainResult.Failure(DomainError.NotFound)
                is OccurrenceResult.AlreadyHandled -> DomainResult.Failure(DomainError.NotFound)
            }
        }
    }
