package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Replaces all current data with the content of a previously previewed backup [json].
 */
class ImportBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
    ) {
        suspend operator fun invoke(json: String): DomainResult<BackupSummary> = backupRepository.importBackup(json)
    }
