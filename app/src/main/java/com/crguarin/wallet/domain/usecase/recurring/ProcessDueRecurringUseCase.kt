package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.result.runSuspendCatching
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.PostedOccurrence
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Records every due AUTO occurrence with `nextDueDate <= today`.
 *
 * Each occurrence is validated with [SaveTransactionUseCase.validate] and
 * recorded through `completeOccurrence` (optimistic `nextDueDate` check, so a
 * concurrent run never records the same charge twice). Late charges are caught
 * up with their real dates, capped at [CATCH_UP_LIMIT] per payment and run.
 * Balance warnings do not apply: a real charge is recorded anyway. A payment
 * whose validation fails is left untouched and never throws.
 */
class ProcessDueRecurringUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(): List<PostedOccurrence> {
            val today = timeProvider.today()
            val posted = mutableListOf<PostedOccurrence>()
            val payments = runSuspendCatching { recurringRepository.getAll() }.getOrDefault(emptyList())
            for (payment in payments) {
                runSuspendCatching { processPayment(payment.id, today, posted) }
            }
            return posted
        }

        private suspend fun processPayment(
            paymentId: Long,
            today: LocalDate,
            posted: MutableList<PostedOccurrence>,
        ) {
            val payment = recurringRepository.get(paymentId) ?: return
            if (!payment.isActive || payment.mode != RecurringMode.AUTO || payment.isFinished) return
            val account = accountRepository.getAccount(payment.accountId) ?: return
            val category = categoryRepository.getCategory(payment.categoryId) ?: return
            if (account.isArchived || category.isArchived) return

            var dueDate = payment.nextDueDate
            var processed = 0
            while (processed < CATCH_UP_LIMIT &&
                !dueDate.isAfter(today) &&
                (payment.endDate == null || !dueDate.isAfter(payment.endDate))
            ) {
                val now = timeProvider.now()
                val transaction =
                    Transaction(
                        type = payment.type,
                        amount = payment.amount,
                        accountId = payment.accountId,
                        categoryId = payment.categoryId,
                        date = dueDate,
                        note = payment.note ?: payment.name,
                        createdAt = now,
                        updatedAt = now,
                        recurringId = payment.id,
                    )
                val validated =
                    when (val result = saveTransactionUseCase.validate(transaction)) {
                        is DomainResult.Success -> result.value
                        is DomainResult.Failure -> return
                    }
                val nextDueDate = RecurrenceMath.nextAfter(payment, dueDate)
                when (val result = recurringRepository.completeOccurrence(payment.id, dueDate, validated, nextDueDate)) {
                    is OccurrenceResult.Recorded ->
                        posted.add(
                            PostedOccurrence(
                                paymentId = payment.id,
                                name = payment.name,
                                type = payment.type,
                                transactionId = result.transactionId,
                                date = dueDate,
                                amount = payment.amount,
                                accountId = account.id,
                                accountName = account.name,
                            ),
                        )
                    is OccurrenceResult.Skipped -> return
                    is OccurrenceResult.AlreadyHandled -> return
                }
                dueDate = nextDueDate
                processed++
            }
        }

        companion object {
            const val CATCH_UP_LIMIT = 120
        }
    }
