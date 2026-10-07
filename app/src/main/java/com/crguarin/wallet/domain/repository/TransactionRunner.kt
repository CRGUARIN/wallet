package com.crguarin.wallet.domain.repository

/**
 * Runs repository calls in a single database transaction (not a money [com.crguarin.wallet.domain.model.Transaction]):
 * either every write in the block is kept or none is, and no other write can land between the
 * reads and the writes of the block. Use cases rely on it for read-modify-write sequences.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
