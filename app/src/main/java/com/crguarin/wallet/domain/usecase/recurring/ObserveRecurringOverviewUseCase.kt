package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringOverview
import com.crguarin.wallet.domain.model.recurring.toItem
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Pending (CONFIRM due, oldest first), active (by next due date) and inactive
 * (paused or finished, by name) sections, plus the monthly totals of the
 * active, unfinished and unblocked payments.
 */
class ObserveRecurringOverviewUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(): Flow<RecurringOverview> =
            combine(
                recurringRepository.observeAll(),
                accountRepository.observeAllAccounts(),
                categoryRepository.observeCategories(includeArchived = true),
            ) { payments, accounts, categories ->
                val today = timeProvider.today()
                val accountsById = accounts.associateBy { it.id }
                val categoriesById = categories.associateBy { it.id }
                val items =
                    payments.map { payment ->
                        payment.toItem(accountsById[payment.accountId], categoriesById[payment.categoryId], today)
                    }
                val pending =
                    items
                        .filter { item ->
                            item.payment.isActive &&
                                !item.payment.isFinished &&
                                item.payment.mode == RecurringMode.CONFIRM &&
                                item.pendingCount > 0
                        }.sortedBy { it.payment.nextDueDate }
                val pendingIds = pending.map { it.payment.id }.toSet()
                val active =
                    items
                        .filter { item ->
                            item.payment.isActive && !item.payment.isFinished && item.payment.id !in pendingIds
                        }.sortedBy { it.payment.nextDueDate }
                val inactive =
                    items
                        .filter { item -> !item.payment.isActive || item.payment.isFinished }
                        .sortedBy { it.payment.name }
                val billable = items.filter { it.payment.isActive && !it.payment.isFinished && it.blockReason == null }
                RecurringOverview(
                    pending = pending,
                    active = active,
                    inactive = inactive,
                    monthlyExpense =
                        billable
                            .filter { it.payment.type == TransactionType.EXPENSE }
                            .fold(Money.ZERO) { acc, item -> acc + item.monthlyEquivalent },
                    monthlyIncome =
                        billable
                            .filter { it.payment.type == TransactionType.INCOME }
                            .fold(Money.ZERO) { acc, item -> acc + item.monthlyEquivalent },
                )
            }
    }
