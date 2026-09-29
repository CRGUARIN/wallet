package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.BackupDocumentStore

class FakeBackupDocumentStore(
    var readResult: DomainResult<String> = DomainResult.Success("{}"),
    var writeResult: DomainResult<Unit> = DomainResult.UnitSuccess,
) : BackupDocumentStore {
    val readUris = mutableListOf<String>()
    val writes = mutableListOf<Pair<String, String>>()

    override suspend fun read(uri: String): DomainResult<String> {
        readUris += uri
        return readResult
    }

    override suspend fun write(
        uri: String,
        content: String,
    ): DomainResult<Unit> {
        writes += uri to content
        return writeResult
    }
}
