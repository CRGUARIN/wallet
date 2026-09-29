package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import com.crguarin.wallet.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Serializes all data and writes it to the document identified by [uri].
 */
class ExportBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val documentStore: BackupDocumentStore,
    ) {
        suspend operator fun invoke(uri: String): DomainResult<Unit> =
            when (val exported = backupRepository.exportBackup()) {
                is DomainResult.Success -> documentStore.write(uri, exported.value)
                is DomainResult.Failure -> exported
            }
    }
