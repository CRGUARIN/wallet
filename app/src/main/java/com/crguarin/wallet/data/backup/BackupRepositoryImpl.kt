package com.crguarin.wallet.data.backup

import androidx.room.withTransaction
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.data.db.BackupDao
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.di.AppVersionName
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.repository.BackupRepository
import com.crguarin.wallet.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        private val database: WalletDatabase,
        private val backupDao: BackupDao,
        private val preferencesRepository: PreferencesRepository,
        private val timeProvider: TimeProvider,
        private val parser: BackupParser,
        @AppVersionName private val appVersion: String,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupRepository {
        override suspend fun exportBackup(): DomainResult<String> =
            withContext(ioDispatcher) {
                guarded {
                    val dto = database.withTransaction { readSnapshot() }
                    DomainResult.Success(parser.encode(dto))
                }
            }

        override suspend fun parseBackup(json: String): DomainResult<BackupSummary> =
            withContext(ioDispatcher) {
                when (val decoded = parser.decode(json)) {
                    is DomainResult.Success -> DomainResult.Success(decoded.value.toSummary())
                    is DomainResult.Failure -> decoded
                }
            }

        override suspend fun importBackup(json: String): DomainResult<BackupSummary> =
            withContext(ioDispatcher) {
                when (val decoded = parser.decode(json)) {
                    is DomainResult.Failure -> decoded
                    is DomainResult.Success ->
                        guarded {
                            database.withTransaction { replaceAll(decoded.value) }
                            clearStaleLastUsedAccount(decoded.value)
                            DomainResult.Success(decoded.value.toSummary())
                        }
                }
            }

        private suspend fun readSnapshot(): BackupDto =
            BackupDto(
                schemaVersion = BackupDto.SCHEMA_VERSION,
                appVersion = appVersion,
                exportedAt = timeProvider.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                currency = APP_CURRENCY,
                accounts = backupDao.getAccounts().map { it.toBackupDto() },
                categories = backupDao.getCategories().map { it.toBackupDto() },
                transactions = backupDao.getTransactions().map { it.toBackupDto() },
                cardStatements = backupDao.getCardStatements().map { it.toBackupDto() },
                recurringPayments = backupDao.getRecurringPayments().map { it.toBackupDto() },
            )

        /** Must run inside a transaction: any exception rolls everything back. */
        private suspend fun replaceAll(dto: BackupDto) {
            backupDao.deleteAllCardStatements()
            backupDao.deleteAllTransactions()
            backupDao.deleteAllRecurringPayments()
            backupDao.deleteSubcategories()
            backupDao.deleteRootCategories()
            backupDao.deleteAllAccounts()

            val (roots, subcategories) = dto.categories.partition { it.parentId == null }
            backupDao.insertAccounts(dto.accounts.map { it.toEntity() })
            backupDao.insertCategories(roots.map { it.toEntity() })
            backupDao.insertCategories(subcategories.map { it.toEntity() })
            backupDao.insertRecurringPayments(dto.recurringPayments.map { it.toEntity() })
            backupDao.insertTransactions(dto.transactions.map { it.toEntity() })
            backupDao.insertCardStatements(dto.cardStatements.map { it.toEntity() })
        }

        /** The last-used account may not exist in the imported data; leaving it would point to nothing. */
        private suspend fun clearStaleLastUsedAccount(dto: BackupDto) {
            val lastUsed = preferencesRepository.getUserPreferences().lastUsedAccountId ?: return
            if (dto.accounts.none { it.id == lastUsed }) {
                preferencesRepository.setLastUsedAccountId(null)
            }
        }

        private fun BackupDto.toSummary() =
            BackupSummary(
                accounts = accounts.size,
                categories = categories.size,
                transactions = transactions.size,
                cardStatements = cardStatements.size,
                recurringPayments = recurringPayments.size,
            )

        private inline fun <T> guarded(block: () -> DomainResult<T>): DomainResult<T> =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DomainResult.Failure(DomainError.BackupFailed)
            }
    }
