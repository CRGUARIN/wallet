package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import java.time.Instant

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currencyCode: String = APP_CURRENCY,
    val initialBalance: Money,
    val color: Int,
    val icon: String,
    val includeInTotal: Boolean = true,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Instant,
)
