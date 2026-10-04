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
    /**
     * Number of installments (1..48). Only set on EXPENSE transactions of a credit card
     * account; informational only (it does not change the amount, the debt or reports).
     */
    val installments: Int? = null,
    /**
     * Id of the recurring payment that generated this transaction; null when it was
     * recorded by hand or when the rule was deleted.
     */
    val recurringId: Long? = null,
)
