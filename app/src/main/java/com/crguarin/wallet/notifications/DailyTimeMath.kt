package com.crguarin.wallet.notifications

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Pure date-time math for the daily background check.
 *
 * Computes how long to wait from [now] until the next occurrence of the user-chosen
 * [hour]:[minute]. When today's time already passed — or it is exactly now — the next
 * run is tomorrow, so scheduling at the exact time yields a 24-hour delay.
 * [ZonedDateTime] arithmetic keeps the result correct across DST transitions.
 */
object DailyTimeMath {
    fun delayUntilNext(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
    ): Duration {
        var next =
            now
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next)
    }
}
