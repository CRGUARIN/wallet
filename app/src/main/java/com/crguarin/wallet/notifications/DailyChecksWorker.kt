package com.crguarin.wallet.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crguarin.wallet.domain.usecase.notifications.RunDailyChecksUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for [DailyChecksWorker]. WorkManager instantiates workers itself, so
 * dependencies are looked up instead of injected (no hilt-work).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyChecksEntryPoint {
    fun runDailyChecks(): RunDailyChecksUseCase
}

/**
 * Runs the daily checks (post due recurring payments, compute and show notifications).
 * Always returns [Result.success], even on unexpected failure: retrying could repeat
 * notifications, and the next scheduled run covers any missed work.
 */
class DailyChecksWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        try {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    DailyChecksEntryPoint::class.java,
                )
            entryPoint.runDailyChecks()
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
}
