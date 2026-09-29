package com.crguarin.wallet.data.backup

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import kotlinx.serialization.Serializable

/**
 * On-disk backup format (PLAN.md section 9). Money is in minor units, timestamps in epoch millis,
 * dates as `YYYY-MM-DD` and enums by name.
 */
@Serializable
data class BackupDto(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: String,
    val currency: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalance: Long,
    val color: Int,
    val icon: String,
    val includeInTotal: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String? = null,
    val systemKey: String? = null,
    val type: CategoryType,
    val parentId: Long? = null,
    val color: Int,
    val icon: String,
    val isArchived: Boolean,
    val sortOrder: Int,
)

@Serializable
data class TransactionDto(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: String,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
