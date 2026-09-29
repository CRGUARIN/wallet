package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.core.result.DomainResult

/**
 * Reads and writes the backup file the user picked. [uri] is an opaque document identifier
 * (a content URI on Android), so the domain layer stays free of platform types.
 */
interface BackupDocumentStore {
    suspend fun read(uri: String): DomainResult<String>

    suspend fun write(
        uri: String,
        content: String,
    ): DomainResult<Unit>

    companion object {
        const val MAX_FILE_BYTES: Int = 10 * 1024 * 1024
    }
}
