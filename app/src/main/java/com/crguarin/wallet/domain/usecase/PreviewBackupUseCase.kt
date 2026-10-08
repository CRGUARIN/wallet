package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.BackupPreview
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import com.crguarin.wallet.domain.repository.BackupRepository
import com.crguarin.wallet.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Reads the backup at [uri] and validates it without modifying any data. Also counts the stored
 * debts the import would delete when the backup has none.
 */
class PreviewBackupUseCase
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val documentStore: BackupDocumentStore,
        private val debtRepository: DebtRepository,
    ) {
        suspend operator fun invoke(uri: String): DomainResult<BackupPreview> =
            when (val read = documentStore.read(uri)) {
                is DomainResult.Failure -> read
                is DomainResult.Success ->
                    when (val parsed = backupRepository.parseBackup(read.value)) {
                        is DomainResult.Success -> {
                            val debtsLost = if (parsed.value.debts == 0) debtRepository.countDebts() else 0
                            DomainResult.Success(BackupPreview(read.value, parsed.value, debtsLost))
                        }
                        is DomainResult.Failure -> parsed
                    }
            }
    }
