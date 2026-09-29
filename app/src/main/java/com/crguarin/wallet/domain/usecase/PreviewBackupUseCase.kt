package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.BackupPreview
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import com.crguarin.wallet.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Reads the backup at [uri] and validates it without modifying any data.
 */
class PreviewBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val documentStore: BackupDocumentStore,
    ) {
        suspend operator fun invoke(uri: String): DomainResult<BackupPreview> =
            when (val read = documentStore.read(uri)) {
                is DomainResult.Failure -> read
                is DomainResult.Success ->
                    when (val parsed = backupRepository.parseBackup(read.value)) {
                        is DomainResult.Success -> DomainResult.Success(BackupPreview(read.value, parsed.value))
                        is DomainResult.Failure -> parsed
                    }
            }
    }
