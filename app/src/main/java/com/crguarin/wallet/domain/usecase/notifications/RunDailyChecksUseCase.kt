package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.repository.NotificationGateway
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements process + compute + show + lastRun.
 */
class RunDailyChecksUseCase
    @Inject
    constructor(
        private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
        private val preferencesRepository: PreferencesRepository,
        private val computeNotificationEventsUseCase: ComputeNotificationEventsUseCase,
        private val notificationGateway: NotificationGateway,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke() {
            // No-op until the domain implementation lands.
        }
    }
