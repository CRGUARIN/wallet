package com.crguarin.wallet.core.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class TimeProviderTest {
    @Test
    fun `default time provider provides current time and date`() {
        val provider = DefaultTimeProvider()
        val beforeMillis = System.currentTimeMillis()
        val nowInstant = provider.now()
        val today = provider.today()
        val nowEpochMillis = provider.nowEpochMillis()
        val afterMillis = System.currentTimeMillis()

        assertThat(nowInstant).isNotNull()
        assertThat(today).isNotNull()
        assertThat(nowEpochMillis).isAtLeast(beforeMillis)
        assertThat(nowEpochMillis).isAtMost(afterMillis)
    }

    @Test
    fun `default time provider respects specified zone id`() {
        val provider = DefaultTimeProvider()
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val utcZone = ZoneOffset.UTC

        val tokyoDate = provider.today(tokyoZone)
        val utcDate = provider.today(utcZone)

        assertThat(tokyoDate).isNotNull()
        assertThat(utcDate).isNotNull()
    }

    @Test
    fun `custom time provider allows deterministic time in tests`() {
        val fixedInstant = Instant.parse("2026-09-28T10:00:00Z")
        val fixedZone = ZoneOffset.UTC

        val testProvider =
            object : TimeProvider {
                override fun now(): Instant = fixedInstant

                override fun today(zoneId: ZoneId): LocalDate = fixedInstant.atZone(zoneId).toLocalDate()

                override fun nowEpochMillis(): Long = fixedInstant.toEpochMilli()
            }

        assertThat(testProvider.now()).isEqualTo(fixedInstant)
        assertThat(testProvider.today(fixedZone)).isEqualTo(LocalDate.of(2026, 9, 28))
        assertThat(testProvider.nowEpochMillis()).isEqualTo(fixedInstant.toEpochMilli())
    }
}
