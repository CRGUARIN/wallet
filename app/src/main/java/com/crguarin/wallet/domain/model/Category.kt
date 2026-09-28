package com.crguarin.wallet.domain.model

data class Category(
    val id: Long = 0,
    val name: String? = null,
    val systemKey: String? = null,
    val type: CategoryType,
    val parentId: Long? = null,
    val color: Int,
    val icon: String,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)
