package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements CONFIRM recording with edited amount/date.
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
        ): DomainResult<Long> = error("not implemented")
    }
