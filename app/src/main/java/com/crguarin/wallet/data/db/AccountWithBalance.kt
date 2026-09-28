package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class AccountWithBalance(
    @Embedded
    val account: AccountEntity,
    @ColumnInfo(name = "balance")
    val balance: Long,
)
