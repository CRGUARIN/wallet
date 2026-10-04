package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.notifications.NotificationEvent

interface NotificationGateway {
    /** Shows [events]. No-op when the system blocks notifications. */
    fun show(events: List<NotificationEvent>)

    fun showTest()
}
