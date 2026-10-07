package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.repository.TransactionRunner

/** Runs the block directly and counts the calls, so tests can check what runs atomically. */
class FakeTransactionRunner : TransactionRunner {
    var runs: Int = 0
        private set

    override suspend fun <T> run(block: suspend () -> T): T {
        runs++
        return block()
    }
}
