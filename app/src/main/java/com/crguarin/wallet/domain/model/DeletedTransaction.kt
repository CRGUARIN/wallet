package com.crguarin.wallet.domain.model

/**
 * A deleted [transaction] and what "undo" must put back with it: the card statements it was the
 * reconciliation adjustment of (deleting it cleared their link).
 */
data class DeletedTransaction(
    val transaction: Transaction,
    val adjustedStatementIds: List<Long> = emptyList(),
)
