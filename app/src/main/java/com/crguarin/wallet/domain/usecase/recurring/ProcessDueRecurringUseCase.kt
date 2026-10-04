package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.PostedOccurrence
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements due-date processing with catch-up.
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
        suspend operator fun invoke(): List<PostedOccurrence> = emptyList()
    }
