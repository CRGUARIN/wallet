package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.BackupSummary

interface BackupRepository {
    /** Serializes all current data as a backup JSON document. */
    suspend fun exportBackup(): DomainResult<String>

    /** Validates [json] without touching the database. */
    suspend fun parseBackup(json: String): DomainResult<BackupSummary>

    /** Validates [json] and, only if valid, atomically replaces all current data with it. */
    suspend fun importBackup(json: String): DomainResult<BackupSummary>
}
