package com.crguarin.wallet.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.crguarin.wallet.R

/**
 * Creates the app notification channels. Idempotent: safe to call from
 * [com.crguarin.wallet.WalletApp.onCreate] and again before notifying.
 * Channels always exist on minSdk 26, so no version check is needed.
 */
object NotificationChannels {
    const val RECURRING = "recurring"
    const val CARDS = "cards"

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                RECURRING,
                context.getString(R.string.notif_channel_recurring_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_recurring_desc)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CARDS,
                context.getString(R.string.notif_channel_cards_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_cards_desc)
            },
        )
    }
}
