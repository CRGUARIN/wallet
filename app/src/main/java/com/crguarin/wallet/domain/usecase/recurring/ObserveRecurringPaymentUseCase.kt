package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.recurring.RecurringItem
import com.crguarin.wallet.domain.model.recurring.toItem
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * A single recurring payment with its account, category, pending count, block
 * reason and monthly equivalent. Emits null when the payment does not exist.
 */
class ObserveRecurringPaymentUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(id: Long): Flow<RecurringItem?> =
            combine(
                recurringRepository.observe(id),
                accountRepository.observeAllAccounts(),
                categoryRepository.observeCategories(includeArchived = true),
                timeProvider.todayFlow(),
            ) { payment, accounts, categories, today ->
                if (payment == null) {
                    null
                } else {
                    payment.toItem(
                        accounts.find { it.id == payment.accountId },
                        categories.find { it.id == payment.categoryId },
                        today,
                    )
                }
            }
    }
