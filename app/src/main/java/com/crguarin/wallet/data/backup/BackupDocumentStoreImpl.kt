package com.crguarin.wallet.data.backup

import android.content.Context
import androidx.core.net.toUri
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** [BackupDocumentStore] backed by the Storage Access Framework through [android.content.ContentResolver]. */
@Singleton
class BackupDocumentStoreImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupDocumentStore {
        override suspend fun read(uri: String): DomainResult<String> =
            withContext(ioDispatcher) {
                try {
                    val input =
                        context.contentResolver.openInputStream(uri.toUri())
                            ?: return@withContext DomainResult.Failure(DomainError.BackupFailed)
                    val bytes =
                        input.use { BoundedReader.readAtMost(it, BackupDocumentStore.MAX_FILE_BYTES) }
                            ?: return@withContext DomainResult.Failure(
                                DomainError.BackupInvalid("file larger than ${BackupDocumentStore.MAX_FILE_BYTES} bytes"),
                            )
                    DomainResult.Success(bytes.toString(Charsets.UTF_8))
                } catch (_: IOException) {
                    DomainResult.Failure(DomainError.BackupFailed)
                } catch (_: SecurityException) {
                    DomainResult.Failure(DomainError.BackupFailed)
                }
            }

        override suspend fun write(
            uri: String,
            content: String,
        ): DomainResult<Unit> =
            withContext(ioDispatcher) {
                try {
                    val stream =
                        context.contentResolver.openOutputStream(uri.toUri(), "wt")
                            ?: return@withContext DomainResult.Failure(DomainError.BackupFailed)
                    stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    DomainResult.UnitSuccess
                } catch (_: IOException) {
                    DomainResult.Failure(DomainError.BackupFailed)
                } catch (_: SecurityException) {
                    DomainResult.Failure(DomainError.BackupFailed)
                }
            }
    }
