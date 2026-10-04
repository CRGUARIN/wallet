package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.repository.DailyWorkScheduler

class FakeDailyWorkScheduler : DailyWorkScheduler {
    data class ScheduleCall(
        val hour: Int,
        val minute: Int,
        val replace: Boolean,
    )

    val calls = mutableListOf<ScheduleCall>()

    override fun schedule(
        hour: Int,
        minute: Int,
        replace: Boolean,
    ) {
        calls.add(ScheduleCall(hour, minute, replace))
    }
}
