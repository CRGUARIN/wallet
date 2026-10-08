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
    /**
     * Total credit limit. Only applies to [AccountType.CREDIT_CARD]; null for other types.
     */
    val creditLimit: Money? = null,
    /**
     * Day of month (1..31) when the statement closes. Only applies to [AccountType.CREDIT_CARD].
     */
    val statementDay: Int? = null,
    /**
     * Day of month (1..31) when payment is due. Only applies to [AccountType.CREDIT_CARD].
     */
    val paymentDueDay: Int? = null,
    /**
     * Fare deducted per trip. Only applies to [AccountType.TRANSIT_CARD]; null for other types.
     */
    val fare: Money? = null,
)
