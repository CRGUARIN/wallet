package com.crguarin.wallet.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyChecksWorkerTest {
    @Test
    fun `doWork runs through the hilt entry point and returns success`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<DailyChecksWorker>(context).build()

        val result = runBlocking { worker.doWork() }

        // Never retry: a retry could repeat notifications the user already saw.
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }
}
