package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakeTimeProvider(
    private var currentInstant: Instant = Instant.parse("2026-09-28T12:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("UTC"),
) : TimeProvider {
    // Driven by setTime/setDate instead of real delays, so collecting it never keeps virtual time busy.
    private val todayState = MutableStateFlow(today())

    fun setTime(instant: Instant) {
        currentInstant = instant
        todayState.value = today()
    }

    fun setDate(date: LocalDate) {
        currentInstant = date.atStartOfDay(zoneId).toInstant()
        todayState.value = today()
    }

    override fun now(): Instant = currentInstant

    override fun today(zoneId: ZoneId): LocalDate = currentInstant.atZone(zoneId).toLocalDate()

    override fun todayFlow(): Flow<LocalDate> = todayState
}
