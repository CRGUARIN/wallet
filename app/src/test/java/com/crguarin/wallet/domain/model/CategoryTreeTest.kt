package com.crguarin.wallet.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryTreeTest {
    private fun category(
        id: Long,
        parentId: Long? = null,
        sortOrder: Int = 0,
    ) = Category(
        id = id,
        name = "c$id",
        type = CategoryType.EXPENSE,
        parentId = parentId,
        color = 1,
        icon = "x",
        sortOrder = sortOrder,
    )

    @Test
    fun `roots are ordered by sortOrder then id`() {
        val tree = buildCategoryTree(listOf(category(3, sortOrder = 1), category(2, sortOrder = 0), category(1, sortOrder = 1)))

        assertThat(tree.map { it.category.id }).containsExactly(2L, 1L, 3L).inOrder()
    }

    @Test
    fun `children are attached to their root and ordered by sortOrder then id`() {
        val tree =
            buildCategoryTree(
                listOf(
                    category(1),
                    category(2),
                    category(10, parentId = 1, sortOrder = 2),
                    category(11, parentId = 1, sortOrder = 1),
                    category(12, parentId = 1, sortOrder = 1),
                    category(20, parentId = 2),
                ),
            )

        assertThat(tree.map { it.category.id }).containsExactly(1L, 2L).inOrder()
        assertThat(tree[0].children.map { it.category.id }).containsExactly(11L, 12L, 10L).inOrder()
        assertThat(tree[1].children.map { it.category.id }).containsExactly(20L)
        assertThat(tree[0].children.all { it.children.isEmpty() }).isTrue()
    }

    @Test
    fun `children whose root is missing are dropped`() {
        val tree = buildCategoryTree(listOf(category(1), category(10, parentId = 99)))

        assertThat(tree.map { it.category.id }).containsExactly(1L)
        assertThat(tree.single().children).isEmpty()
    }

    @Test
    fun `empty list gives an empty tree`() {
        assertThat(buildCategoryTree(emptyList())).isEmpty()
    }
}
