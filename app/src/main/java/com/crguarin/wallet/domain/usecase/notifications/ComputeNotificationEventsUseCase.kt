package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) computes every event except RecurringPosted.
 */
class ComputeNotificationEventsUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(
            lastRun: LocalDate?,
            today: LocalDate,
            settings: NotificationSettings,
        ): List<NotificationEvent> = emptyList()
    }
