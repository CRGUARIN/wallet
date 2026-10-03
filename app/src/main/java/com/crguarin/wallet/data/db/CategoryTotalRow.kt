package com.crguarin.wallet.data.db

import androidx.room.ColumnInfo

/**
 * One row of the aggregated category totals query: a leaf category, or a root category with direct
 * movements.
 *
 * The category columns are nullable: `category_id` may be null (uncategorized movements) or the
 * referenced category may not exist. `parent_*` columns are filled when the category has a parent.
 */
data class CategoryTotalRow(
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    @ColumnInfo(name = "category_name")
    val categoryName: String?,
    @ColumnInfo(name = "category_system_key")
    val categorySystemKey: String?,
    @ColumnInfo(name = "category_color")
    val categoryColor: Int?,
    @ColumnInfo(name = "category_icon")
    val categoryIcon: String?,
    @ColumnInfo(name = "parent_id")
    val parentId: Long?,
    @ColumnInfo(name = "parent_name")
    val parentName: String?,
    @ColumnInfo(name = "parent_system_key")
    val parentSystemKey: String?,
    @ColumnInfo(name = "parent_color")
    val parentColor: Int?,
    @ColumnInfo(name = "parent_icon")
    val parentIcon: String?,
    @ColumnInfo(name = "total")
    val total: Long,
    @ColumnInfo(name = "tx_count")
    val transactionCount: Int,
)
