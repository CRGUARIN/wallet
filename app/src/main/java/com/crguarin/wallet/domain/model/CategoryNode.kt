package com.crguarin.wallet.domain.model

data class CategoryNode(
    val category: Category,
    val children: List<CategoryNode> = emptyList(),
)
