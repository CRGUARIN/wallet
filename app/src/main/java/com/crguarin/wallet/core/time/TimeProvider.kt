package com.crguarin.wallet.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over time and system clock to enable deterministic unit testing.
 */
interface TimeProvider {
    /**
     * Returns the current [Instant].
     */
    fun now(): Instant

    /**
     * Returns the current [LocalDate] in the specified or system default [ZoneId].
     */
    fun today(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate = now().atZone(zoneId).toLocalDate()

    /**
     * Returns the current epoch timestamp in milliseconds.
     */
    fun nowEpochMillis(): Long = now().toEpochMilli()

    /**
     * The current date in the system default zone, emitted again whenever the day changes while
     * it is collected. Screens that show "today" or the current month observe it instead of
     * reading [today] once, so they never keep yesterday's date while the app stays open.
     *
     * The clock is checked at the next midnight and at least every [DAY_CHECK_INTERVAL_MILLIS],
     * because a delay does not advance while the device sleeps.
     */
    fun todayFlow(): Flow<LocalDate> =
        flow {
            while (true) {
                val zone = ZoneId.systemDefault()
                val now = now().atZone(zone)
                emit(now.toLocalDate())
                val nextDayStart = now.toLocalDate().plusDays(1).atStartOfDay(zone)
                val untilNextDay = Duration.between(now, nextDayStart).toMillis()
                delay(untilNextDay.coerceIn(1L, DAY_CHECK_INTERVAL_MILLIS))
            }
        }.distinctUntilChanged()

    companion object {
        const val DAY_CHECK_INTERVAL_MILLIS: Long = 60_000L
    }
}

/**
 * Default implementation of [TimeProvider] backed by java.time system clock.
 */
@Singleton
class DefaultTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun now(): Instant = Instant.now()

        override fun today(zoneId: ZoneId): LocalDate = LocalDate.now(zoneId)

        override fun nowEpochMillis(): Long = System.currentTimeMillis()
    }
