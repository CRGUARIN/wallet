package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money

data class AccountWithBalance(
    val account: Account,
    val balance: Money,
)
