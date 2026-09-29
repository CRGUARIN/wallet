package com.crguarin.wallet.domain.model

import javax.inject.Inject

/**
 * Single implementation of the "same name among siblings" rule for categories. System categories
 * collide through every translation of their name.
 */
class CategoryNameMatcher
    @Inject
    constructor(
        private val systemCategoryNames: SystemCategoryNames,
    ) {
        /** Normalized names of [category]; empty when it has neither a custom name nor a system key. */
        fun namesOf(category: Category): List<String> =
            when {
                category.name != null -> listOf(category.name.normalizedName())
                category.systemKey != null ->
                    systemCategoryNames.namesFor(category.systemKey).map { it.normalizedName() }
                else -> emptyList()
            }

        /** True when any of [siblings] shares a name with any of [candidateNames]. */
        fun collides(
            candidateNames: Collection<String>,
            siblings: Collection<Category>,
        ): Boolean {
            val candidates = candidateNames.map { it.normalizedName() }.toSet()
            if (candidates.isEmpty()) return false
            return siblings.any { sibling -> namesOf(sibling).any { it in candidates } }
        }
    }
