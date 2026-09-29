package com.crguarin.wallet.ui.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BackupFileNameTest {
    private val instant = Instant.parse("2026-09-28T03:05:59Z")

    @Test
    fun `uses the pattern with date and time`() {
        assertThat(backupFileName(instant, ZoneId.of("UTC"))).isEqualTo("wallet-backup-20260928-0305.json")
    }

    @Test
    fun `uses the local time of the given zone`() {
        assertThat(backupFileName(instant, ZoneId.of("America/Bogota"))).isEqualTo("wallet-backup-20260927-2205.json")
    }
}
