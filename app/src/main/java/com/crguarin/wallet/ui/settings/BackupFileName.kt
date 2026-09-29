package com.crguarin.wallet.ui.settings

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ROOT)

/** Suggested name for an exported backup, e.g. `wallet-backup-20260928-1530.json`, in the given [zone]. */
fun backupFileName(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "wallet-backup-${TIMESTAMP_FORMAT.format(instant.atZone(zone))}.json"
