package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.repository.BackupRepository

class FakeBackupRepository(
    var exportResult: DomainResult<String> = DomainResult.Success("{}"),
    var parseResult: DomainResult<BackupSummary> = DomainResult.Success(BackupSummary(0, 0, 0)),
    var importResult: DomainResult<BackupSummary> = DomainResult.Success(BackupSummary(0, 0, 0)),
) : BackupRepository {
    val parsedJson = mutableListOf<String>()
    val importedJson = mutableListOf<String>()
    var exportCalls = 0

    /** Optional hook run inside each suspend call, to hold an operation "in progress" in tests. */
    var onCall: suspend () -> Unit = {}

    override suspend fun exportBackup(): DomainResult<String> {
        exportCalls++
        onCall()
        return exportResult
    }

    override suspend fun parseBackup(json: String): DomainResult<BackupSummary> {
        parsedJson += json
        onCall()
        return parseResult
    }

    override suspend fun importBackup(json: String): DomainResult<BackupSummary> {
        importedJson += json
        onCall()
        return importResult
    }
}
