package com.crguarin.wallet.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationChannelsTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)
    }

    @Test
    fun `wallet app startup creates both channels from strings`() {
        // Robolectric instantiates WalletApp and calls onCreate before the test body.
        val recurring = manager.getNotificationChannel(NotificationChannels.RECURRING)
        val cards = manager.getNotificationChannel(NotificationChannels.CARDS)

        assertThat(recurring).isNotNull()
        assertThat(cards).isNotNull()
        assertThat(recurring.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(cards.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(recurring.name.toString()).isEqualTo(context.getString(R.string.notif_channel_recurring_name))
        assertThat(recurring.description).isEqualTo(context.getString(R.string.notif_channel_recurring_desc))
        assertThat(cards.name.toString()).isEqualTo(context.getString(R.string.notif_channel_cards_name))
        assertThat(cards.description).isEqualTo(context.getString(R.string.notif_channel_cards_desc))
    }

    @Test
    fun `createAll is idempotent`() {
        NotificationChannels.createAll(context)
        NotificationChannels.createAll(context)

        assertThat(manager.getNotificationChannel(NotificationChannels.RECURRING)).isNotNull()
        assertThat(manager.getNotificationChannel(NotificationChannels.CARDS)).isNotNull()
    }
}
