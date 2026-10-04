package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

/**
 * Creates or edits a recurring payment.
 *
 * On creation `nextDueDate = startDate`. On edition, when `frequency`,
 * `interval` or `startDate` change, `nextDueDate` is recalculated as
 * `firstOnOrAfter(today)` of the new calendar; otherwise it is preserved.
 * Already recorded transactions are never modified.
 */
class SaveRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(payment: RecurringPayment): DomainResult<Long> {
            if (payment.name.isBlank()) return DomainResult.Failure(DomainError.NameEmpty)
            if (payment.name.length > DomainLimits.NAME_MAX_LENGTH) {
                return DomainResult.Failure(DomainError.NameTooLong)
            }
            if (payment.type != TransactionType.EXPENSE && payment.type != TransactionType.INCOME) {
                return DomainResult.Failure(DomainError.InvalidTransaction)
            }
            if (payment.amount <= Money.ZERO || payment.amount > Money.MAX) {
                return DomainResult.Failure(DomainError.AmountInvalid)
            }
            val account =
                accountRepository.getAccount(payment.accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (account.isArchived) return DomainResult.Failure(DomainError.AccountArchived)
            val category =
                categoryRepository.getCategory(payment.categoryId)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (category.isArchived) return DomainResult.Failure(DomainError.CategoryArchived)
            val expectedCategoryType =
                if (payment.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            if (category.type != expectedCategoryType) {
                return DomainResult.Failure(DomainError.CategoryTypeMismatch)
            }
            if (payment.interval !in 1..12) return DomainResult.Failure(DomainError.RecurringIntervalInvalid)
            val reminder = payment.reminderDaysBefore
            if (reminder != null && reminder !in 1..7) return DomainResult.Failure(DomainError.InvalidTransaction)
            if (payment.note != null && payment.note.length > DomainLimits.NOTE_MAX_LENGTH) {
                return DomainResult.Failure(DomainError.NoteTooLong)
            }

            val today = timeProvider.today()
            val maxStartDate = DomainLimits.maxTransactionDate(today)
            val now = timeProvider.now()
            return if (payment.id == 0L) {
                if (payment.startDate.isBefore(today)) {
                    return DomainResult.Failure(DomainError.RecurringDateInPast)
                }
                if (payment.startDate.isAfter(maxStartDate)) {
                    return DomainResult.Failure(DomainError.DateOutOfRange)
                }
                val endDate = payment.endDate
                if (endDate != null && endDate.isBefore(payment.startDate)) {
                    return DomainResult.Failure(DomainError.RecurringEndBeforeStart)
                }
                val id =
                    recurringRepository.save(
                        payment.copy(nextDueDate = payment.startDate, createdAt = now, updatedAt = now),
                    )
                DomainResult.Success(id)
            } else {
                val existing =
                    recurringRepository.get(payment.id)
                        ?: return DomainResult.Failure(DomainError.NotFound)
                val calendarChanged =
                    payment.frequency != existing.frequency ||
                        payment.interval != existing.interval ||
                        payment.startDate != existing.startDate
                val nextDueDate =
                    if (calendarChanged) {
                        if (payment.startDate != existing.startDate) {
                            if (payment.startDate.isBefore(today)) {
                                return DomainResult.Failure(DomainError.RecurringDateInPast)
                            }
                            if (payment.startDate.isAfter(maxStartDate)) {
                                return DomainResult.Failure(DomainError.DateOutOfRange)
                            }
                        }
                        RecurrenceMath.firstOnOrAfter(
                            payment.startDate,
                            payment.frequency,
                            payment.interval,
                            today,
                        )
                    } else {
                        existing.nextDueDate
                    }
                val endDate = payment.endDate
                if (endDate != null && (endDate.isBefore(payment.startDate) || endDate.isBefore(nextDueDate))) {
                    return DomainResult.Failure(DomainError.RecurringEndBeforeStart)
                }
                val id =
                    recurringRepository.save(
                        payment.copy(nextDueDate = nextDueDate, createdAt = existing.createdAt, updatedAt = now),
                    )
                DomainResult.Success(id)
            }
        }
    }
