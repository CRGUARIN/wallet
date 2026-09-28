package com.crguarin.wallet.core.time

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
