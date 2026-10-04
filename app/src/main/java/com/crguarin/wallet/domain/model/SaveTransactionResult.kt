package com.crguarin.wallet.domain.model

import com.crguarin.wallet.domain.model.card.BalanceWarning

data class SaveTransactionResult(
    val transactionId: Long,
    val balanceWarning: BalanceWarning,
)
