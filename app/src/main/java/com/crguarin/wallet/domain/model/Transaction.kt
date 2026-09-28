package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money
import java.time.Instant
import java.time.LocalDate

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Money,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDate,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
