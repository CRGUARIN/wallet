package com.crguarin.wallet.domain.model

/**
 * Builds root -> children nodes ordered by (sortOrder, id). Children whose parent is not in
 * [categories] are dropped.
 */
fun buildCategoryTree(categories: List<Category>): List<CategoryNode> {
    val order = compareBy<Category>({ it.sortOrder }, { it.id })
    return categories
        .filter { it.parentId == null }
        .sortedWith(order)
        .map { root ->
            val children =
                categories
                    .filter { it.parentId == root.id }
                    .sortedWith(order)
                    .map { CategoryNode(it) }
            CategoryNode(category = root, children = children)
        }
}
