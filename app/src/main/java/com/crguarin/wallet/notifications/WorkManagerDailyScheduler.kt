package com.crguarin.wallet.notifications

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily background check with WorkManager as unique periodic work.
 * No network or battery constraints: the app is offline and posting due charges is
 * the primary function. No exact alarms are used.
 */
@Singleton
class WorkManagerDailyScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) : DailyWorkScheduler {
        override fun schedule(
            hour: Int,
            minute: Int,
            replace: Boolean,
        ) {
            val delay = DailyTimeMath.delayUntilNext(ZonedDateTime.now(), hour, minute)
            val request =
                PeriodicWorkRequestBuilder<DailyChecksWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                    .setInitialDelay(delay)
                    .build()
            val policy =
                if (replace) {
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                }
            workManager.enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        companion object {
            const val WORK_NAME = "wallet_daily_checks"
            const val INTERVAL_HOURS = 24L
        }
    }
