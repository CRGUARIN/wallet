package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import javax.inject.Inject

/**
 * Provisional stub: Task 16 (domain) implements process (without notifying) + schedule(KEEP).
 */
class OnAppStartUseCase
    @Inject
    constructor(
        private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
        private val preferencesRepository: PreferencesRepository,
        private val dailyWorkScheduler: DailyWorkScheduler,
    ) {
        suspend operator fun invoke() {
            // No-op until the domain implementation lands.
        }
    }
