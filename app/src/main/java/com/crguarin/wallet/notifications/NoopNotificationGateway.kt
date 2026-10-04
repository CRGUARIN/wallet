package com.crguarin.wallet.notifications

import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.repository.NotificationGateway
import javax.inject.Inject

/**
 * Provisional no-op: Task 16 (platform) replaces it with the Android implementation.
 */
class NoopNotificationGateway
    @Inject
    constructor() : NotificationGateway {
        override fun show(events: List<NotificationEvent>) {
            // No-op until the Android implementation lands.
        }

        override fun showTest() {
            // No-op until the Android implementation lands.
        }
    }
