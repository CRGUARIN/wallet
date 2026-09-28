package com.crguarin.wallet.domain.model

data class SaveTransactionResult(
    val transactionId: Long,
    val willBeNegative: Boolean,
)
