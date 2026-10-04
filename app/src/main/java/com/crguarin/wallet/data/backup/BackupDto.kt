package com.crguarin.wallet.data.backup

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import kotlinx.serialization.Serializable

/**
 * On-disk backup format (PLAN.md section 9). Money is in minor units, timestamps in epoch millis,
 * dates as `YYYY-MM-DD` and enums by name.
 *
 * Version 2 adds credit card fields ([AccountDto.creditLimit], [AccountDto.statementDay],
 * [AccountDto.paymentDueDay], [TransactionDto.installments]) and [BackupDto.cardStatements].
 * An older app version (schema 1) cannot import a version 2 backup: its parser reports
 * `BackupVersionUnsupported` for any version other than 1.
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
    val cardStatements: List<CardStatementDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 2
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
    val creditLimit: Long? = null,
    val statementDay: Int? = null,
    val paymentDueDay: Int? = null,
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
    val installments: Int? = null,
)

@Serializable
data class CardStatementDto(
    val id: Long,
    val accountId: Long,
    val closingDate: String,
    val dueDate: String,
    val statementBalance: Long,
    val minimumPayment: Long? = null,
    val statedInterestAndFees: Long? = null,
    val adjustmentTransactionId: Long? = null,
    val createdAt: Long,
)
