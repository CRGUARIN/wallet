package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.result.runSuspendCatching
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import javax.inject.Inject

/**
 * Cold-start processing: records due charges without notifying (the user is
 * already looking at the app) and keeps the daily background check scheduled
 * without replacing it.
 */
class OnAppStartUseCase
    @Inject
    constructor(
        private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
        private val preferencesRepository: PreferencesRepository,
        private val dailyWorkScheduler: DailyWorkScheduler,
    ) {
        suspend operator fun invoke() {
            runSuspendCatching { processDueRecurringUseCase() }
            val settings =
                runSuspendCatching { preferencesRepository.getNotificationSettings() }
                    .getOrDefault(NotificationSettings())
            runSuspendCatching { dailyWorkScheduler.schedule(settings.hour, settings.minute, replace = false) }
        }
    }
