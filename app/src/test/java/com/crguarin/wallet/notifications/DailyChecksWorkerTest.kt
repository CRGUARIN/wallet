package com.crguarin.wallet.notifications

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

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
        // The checks really ran: their last step always stores today as the last run date.
        val preferences = context.preferencesDataStoreFile("wallet_user_preferences")
        assertThat(preferences.exists()).isTrue()
        assertThat(String(preferences.readBytes(), Charsets.ISO_8859_1)).contains(LocalDate.now().toString())
    }
}
