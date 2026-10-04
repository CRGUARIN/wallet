package com.crguarin.wallet.notifications

import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import javax.inject.Inject

/**
 * Provisional no-op: Task 16 (platform) replaces it with the WorkManager scheduler.
 */
class NoopDailyWorkScheduler
    @Inject
    constructor() : DailyWorkScheduler {
        override fun schedule(
            hour: Int,
            minute: Int,
            replace: Boolean,
        ) {
            // No-op until the WorkManager implementation lands.
        }
    }
