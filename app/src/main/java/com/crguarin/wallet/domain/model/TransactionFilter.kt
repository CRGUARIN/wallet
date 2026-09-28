package com.crguarin.wallet.domain.model

import java.time.LocalDate

data class TransactionFilter(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val type: TransactionType? = null,
    val noteQuery: String? = null,
)
