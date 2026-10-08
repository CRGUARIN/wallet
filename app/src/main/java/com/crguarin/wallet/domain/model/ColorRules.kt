package com.crguarin.wallet.domain.model

/** Color uniqueness rules for accounts and categories (see PLAN §5.6). */
object ColorRules {
    /** Colors taken by other non-archived accounts. */
    fun usedAccountColors(
        accounts: List<Account>,
        editingId: Long?,
    ): Set<Int> =
        accounts
            .filter { !it.isArchived && it.id != editingId }
            .map { it.color }
            .toSet()

    /**
     * Colors taken by non-archived categories of [type] outside the family of the edited category.
     * The family is the root itself, or the parent of a subcategory. A new root (no [editingId],
     * no [parentId]) has no family, so every other category of the type counts.
     */
    fun usedCategoryColors(
        categories: List<Category>,
        type: CategoryType,
        editingId: Long?,
        parentId: Long?,
    ): Set<Int> {
        val family = familyOf(parentId, editingId)
        val otherFamilies =
            categories.filter { category ->
                category.type == type &&
                    !category.isArchived &&
                    category.id != editingId &&
                    familyOf(category.parentId, category.id) != family
            }
        return otherFamilies.map { it.color }.toSet()
    }

    /** First color of [palette] not in [used]; the first palette color when all of them are used. */
    fun firstFreeColor(
        palette: List<Int>,
        used: Set<Int>,
    ): Int = palette.firstOrNull { it !in used } ?: palette.first()

    private fun familyOf(
        parentId: Long?,
        id: Long?,
    ): Long? = parentId ?: id?.takeIf { it != 0L }
}
