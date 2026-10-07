package com.crguarin.wallet.data.db

import androidx.room.withTransaction
import com.crguarin.wallet.domain.repository.TransactionRunner
import javax.inject.Inject
import javax.inject.Singleton

/** [TransactionRunner] backed by [withTransaction]; nested calls join the outer transaction. */
@Singleton
class RoomTransactionRunner
    @Inject
    constructor(
        private val database: WalletDatabase,
    ) : TransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
    }
