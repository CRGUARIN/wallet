package com.crguarin.wallet.domain.model

import com.crguarin.wallet.core.money.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class ColorRulesTest {
    private val red = 0xFFEF5350.toInt()
    private val blue = 0xFF42A5F5.toInt()
    private val green = 0xFF66BB6A.toInt()

    @Test
    fun `usedAccountColors ignores archived accounts and the edited account`() {
        val accounts =
            listOf(
                account(id = 1L, color = red),
                account(id = 2L, color = blue, isArchived = true),
                account(id = 3L, color = green),
            )

        assertThat(ColorRules.usedAccountColors(accounts, editingId = 1L)).containsExactly(green)
    }

    @Test
    fun `usedAccountColors lists every active account when creating`() {
        val accounts = listOf(account(id = 1L, color = red), account(id = 2L, color = blue))

        assertThat(ColorRules.usedAccountColors(accounts, editingId = null)).containsExactly(red, blue)
    }

    @Test
    fun `usedCategoryColors only counts categories of the same type`() {
        val categories =
            listOf(
                category(id = 1L, color = red),
                category(id = 2L, color = blue, type = CategoryType.INCOME),
            )

        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, null, null)).containsExactly(red)
        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.INCOME, null, null)).containsExactly(blue)
    }

    @Test
    fun `usedCategoryColors lets a root and its subcategories share a color family`() {
        val categories =
            listOf(
                category(id = 1L, color = red),
                category(id = 2L, color = green, parentId = 1L),
                category(id = 3L, color = blue),
            )

        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, editingId = 1L, parentId = null))
            .containsExactly(blue)
        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, editingId = 2L, parentId = 1L))
            .containsExactly(blue)
        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, editingId = null, parentId = 1L))
            .containsExactly(blue)
    }

    @Test
    fun `usedCategoryColors counts every category when creating a root`() {
        val categories =
            listOf(
                category(id = 1L, color = red),
                category(id = 2L, color = green, parentId = 1L),
                category(id = 3L, color = blue),
            )

        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, editingId = null, parentId = null))
            .containsExactly(red, green, blue)
    }

    @Test
    fun `usedCategoryColors skips archived categories`() {
        val categories =
            listOf(
                category(id = 1L, color = red, isArchived = true),
                category(id = 2L, color = blue),
            )

        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, null, null)).containsExactly(blue)
    }

    @Test
    fun `usedCategoryColors does not count the edited category itself when its parent changes`() {
        val categories =
            listOf(
                category(id = 1L, color = red),
                category(id = 2L, color = blue, parentId = 1L),
            )

        assertThat(ColorRules.usedCategoryColors(categories, CategoryType.EXPENSE, editingId = 2L, parentId = null))
            .containsExactly(red)
    }

    @Test
    fun `firstFreeColor returns the first palette color that is not used`() {
        val palette = listOf(red, blue, green)

        assertThat(ColorRules.firstFreeColor(palette, setOf(red))).isEqualTo(blue)
        assertThat(ColorRules.firstFreeColor(palette, emptySet())).isEqualTo(red)
    }

    @Test
    fun `firstFreeColor falls back to the first palette color when all are used`() {
        assertThat(ColorRules.firstFreeColor(listOf(red, blue), setOf(red, blue))).isEqualTo(red)
    }

    private fun account(
        id: Long,
        color: Int,
        isArchived: Boolean = false,
    ): Account =
        Account(
            id = id,
            name = "Cuenta $id",
            type = AccountType.CASH,
            initialBalance = Money.ZERO,
            color = color,
            icon = "wallet",
            isArchived = isArchived,
            createdAt = Instant.parse("2026-09-28T12:00:00Z"),
        )

    private fun category(
        id: Long,
        color: Int,
        type: CategoryType = CategoryType.EXPENSE,
        parentId: Long? = null,
        isArchived: Boolean = false,
    ): Category =
        Category(
            id = id,
            name = "Categoría $id",
            type = type,
            parentId = parentId,
            color = color,
            icon = "home",
            isArchived = isArchived,
        )
}
