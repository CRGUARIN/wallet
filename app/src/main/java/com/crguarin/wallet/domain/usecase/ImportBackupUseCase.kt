package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.result.onSuccess
import com.crguarin.wallet.core.result.runSuspendCatching
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.repository.BackupRepository
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import javax.inject.Inject

/**
 * Replaces all current data with the content of a previously previewed backup [json], then records
 * the automatic recurring charges already due in the imported data, as an app start would, instead
 * of leaving them for the next daily check.
 */
class ImportBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
    ) {
        suspend operator fun invoke(json: String): DomainResult<BackupSummary> =
            backupRepository.importBackup(json).onSuccess {
                // The import already succeeded: a failure recording charges must not report it as failed.
                runSuspendCatching { processDueRecurringUseCase() }
            }
    }
