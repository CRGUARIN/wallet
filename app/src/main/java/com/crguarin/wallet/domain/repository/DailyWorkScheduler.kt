package com.crguarin.wallet.domain.repository

interface DailyWorkScheduler {
    /** Schedules the daily background check at [hour]:[minute]. [replace] = true reschedules an existing one. */
    fun schedule(
        hour: Int,
        minute: Int,
        replace: Boolean,
    )
}
