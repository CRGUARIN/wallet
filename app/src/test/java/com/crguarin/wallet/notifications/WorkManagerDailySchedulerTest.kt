package com.crguarin.wallet.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.crguarin.wallet.notifications.WorkManagerDailyScheduler.Companion.WORK_NAME
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkManagerDailySchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerDailyScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                Configuration
                    .Builder()
                    .setExecutor(SynchronousExecutor())
                    .build(),
            )
        } catch (_: IllegalStateException) {
            // Already initialized by a previous test in this class.
        }
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerDailyScheduler(workManager)
    }

    @Test
    fun `schedule twice without replace keeps the original job`() {
        scheduler.schedule(8, 0, replace = false)
        val before =
            workManager
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
                .single()
                .id

        scheduler.schedule(9, 30, replace = false)

        val infos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        assertThat(infos).hasSize(1)
        // KEEP must preserve the pending work: re-enqueuing on every app start must not reset the timer.
        assertThat(infos.single().id).isEqualTo(before)
    }

    @Test
    fun `schedule with replace reschedules the job`() {
        scheduler.schedule(8, 0, replace = false)
        val before =
            workManager
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
                .single()
                .id

        scheduler.schedule(20, 15, replace = true)

        val after = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        assertThat(after).hasSize(1)
        assertThat(after.single().id).isNotEqualTo(before)
    }
}
