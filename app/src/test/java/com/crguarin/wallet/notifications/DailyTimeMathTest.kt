package com.crguarin.wallet.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyTimeMathTest {
    private val zone = ZoneId.of("America/Bogota")

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zoneId: ZoneId = zone,
    ): ZonedDateTime = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zoneId)

    @Test
    fun `before the hour waits until today`() {
        val delay = DailyTimeMath.delayUntilNext(at(2026, 10, 4, 7, 0), 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `after the hour waits until tomorrow`() {
        val delay = DailyTimeMath.delayUntilNext(at(2026, 10, 4, 9, 30), 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(22).plusMinutes(30))
    }

    @Test
    fun `exactly on time waits a full day`() {
        val delay = DailyTimeMath.delayUntilNext(at(2026, 10, 4, 8, 0), 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(24))
    }

    @Test
    fun `seconds past the hour waits until tomorrow`() {
        val now = at(2026, 10, 4, 8, 0).plusSeconds(30)
        val delay = DailyTimeMath.delayUntilNext(now, 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(24).minusSeconds(30))
    }

    @Test
    fun `non-round minutes are honored`() {
        val delay = DailyTimeMath.delayUntilNext(at(2026, 10, 4, 8, 0), 8, 30)

        assertThat(delay).isEqualTo(Duration.ofMinutes(30))
    }

    @Test
    fun `spring forward shortens the wait`() {
        // US DST starts 2026-03-08: the night is 23 hours long.
        val eastern = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 10, 0, 0, 0, eastern)
        val delay = DailyTimeMath.delayUntilNext(now, 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(21))
    }

    @Test
    fun `fall back lengthens the wait`() {
        // US DST ends 2026-11-01: the repeated hour adds one hour of elapsed time.
        val eastern = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 10, 31, 10, 0, 0, 0, eastern)
        val delay = DailyTimeMath.delayUntilNext(now, 8, 0)

        assertThat(delay).isEqualTo(Duration.ofHours(23))
    }
}
