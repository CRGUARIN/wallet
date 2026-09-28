package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money

data class MonthSummary(
    val income: Money,
    val expense: Money,
) {
    val net: Money
        get() = income - expense
}
