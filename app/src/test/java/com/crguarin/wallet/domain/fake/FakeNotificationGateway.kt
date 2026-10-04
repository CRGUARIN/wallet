package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.repository.NotificationGateway

class FakeNotificationGateway : NotificationGateway {
    val shownBatches = mutableListOf<List<NotificationEvent>>()
    var testShown: Boolean = false
        private set

    override fun show(events: List<NotificationEvent>) {
        shownBatches.add(events.toList())
    }

    override fun showTest() {
        testShown = true
    }

    val shownEvents: List<NotificationEvent> get() = shownBatches.flatten()
}
