package com.crguarin.wallet.domain.model

import com.crguarin.wallet.domain.fake.FakeSystemCategoryNames
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryNameMatcherTest {
    private val matcher = CategoryNameMatcher(FakeSystemCategoryNames())

    private fun custom(
        id: Long,
        name: String,
    ) = Category(id = id, name = name, type = CategoryType.EXPENSE, color = 1, icon = "x")

    private fun system(
        id: Long,
        key: String,
    ) = Category(id = id, name = null, systemKey = key, type = CategoryType.EXPENSE, color = 1, icon = "x")

    @Test
    fun `namesOf a custom category is its normalized name`() {
        assertThat(matcher.namesOf(custom(1, "  Café "))).containsExactly("café")
    }

    @Test
    fun `namesOf a system category is every normalized translation`() {
        assertThat(matcher.namesOf(system(1, "food"))).containsExactly("alimentación", "food")
    }

    @Test
    fun `namesOf a category without name or key is empty`() {
        val category = Category(id = 1, name = null, systemKey = null, type = CategoryType.EXPENSE, color = 1, icon = "x")
        assertThat(matcher.namesOf(category)).isEmpty()
    }

    @Test
    fun `collides with a custom sibling ignoring case and spaces`() {
        assertThat(matcher.collides(listOf(" CAFÉ"), listOf(custom(1, "café")))).isTrue()
    }

    @Test
    fun `collides with any translation of a system sibling`() {
        assertThat(matcher.collides(listOf("food"), listOf(system(1, "food")))).isTrue()
        assertThat(matcher.collides(listOf("alimentación"), listOf(system(1, "food")))).isTrue()
    }

    @Test
    fun `does not collide with different names or no siblings`() {
        assertThat(matcher.collides(listOf("otro"), listOf(custom(1, "café"), system(2, "food")))).isFalse()
        assertThat(matcher.collides(listOf("café"), emptyList())).isFalse()
    }

    @Test
    fun `empty candidates never collide`() {
        assertThat(matcher.collides(emptyList(), listOf(custom(1, "café")))).isFalse()
    }
}
