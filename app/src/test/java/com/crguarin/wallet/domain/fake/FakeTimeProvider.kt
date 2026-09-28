package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakeTimeProvider(
    private var currentInstant: Instant = Instant.parse("2026-09-28T12:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("UTC"),
) : TimeProvider {
    fun setTime(instant: Instant) {
        currentInstant = instant
    }

    fun setDate(date: LocalDate) {
        currentInstant = date.atStartOfDay(zoneId).toInstant()
    }

    override fun now(): Instant = currentInstant

    override fun today(zoneId: ZoneId): LocalDate = currentInstant.atZone(zoneId).toLocalDate()
}
