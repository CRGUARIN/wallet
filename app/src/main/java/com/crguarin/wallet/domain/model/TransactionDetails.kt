package com.crguarin.wallet.domain.model

data class TransactionDetails(
    val transaction: Transaction,
    val accountName: String,
    val accountColor: Int,
    val accountIcon: String,
    val toAccountName: String? = null,
    val toAccountColor: Int? = null,
    val toAccountIcon: String? = null,
    val categoryName: String? = null,
    val categorySystemKey: String? = null,
    val categoryColor: Int? = null,
    val categoryIcon: String? = null,
    val categoryParentId: Long? = null,
    val parentCategoryName: String? = null,
    val parentCategorySystemKey: String? = null,
)
