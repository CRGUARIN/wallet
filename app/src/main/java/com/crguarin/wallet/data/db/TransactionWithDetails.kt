package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,
    @ColumnInfo(name = "account_name")
    val accountName: String,
    @ColumnInfo(name = "account_color")
    val accountColor: Int,
    @ColumnInfo(name = "account_icon")
    val accountIcon: String,
    @ColumnInfo(name = "to_account_name")
    val toAccountName: String? = null,
    @ColumnInfo(name = "to_account_color")
    val toAccountColor: Int? = null,
    @ColumnInfo(name = "to_account_icon")
    val toAccountIcon: String? = null,
    @ColumnInfo(name = "category_name")
    val categoryName: String? = null,
    @ColumnInfo(name = "category_system_key")
    val categorySystemKey: String? = null,
    @ColumnInfo(name = "category_color")
    val categoryColor: Int? = null,
    @ColumnInfo(name = "category_icon")
    val categoryIcon: String? = null,
    @ColumnInfo(name = "category_parent_id")
    val categoryParentId: Long? = null,
    @ColumnInfo(name = "parent_category_name")
    val parentCategoryName: String? = null,
    @ColumnInfo(name = "parent_category_system_key")
    val parentCategorySystemKey: String? = null,
)
