package com.crguarin.wallet.core.resources

import androidx.annotation.StringRes
import com.crguarin.wallet.R

/**
 * Maps the `systemKey` of seeded categories to their translated string resource.
 *
 * An explicit map (instead of `Resources.getIdentifier`) keeps the strings visible to lint and
 * to resource shrinking in release builds. Shared by the data layer and the UI.
 */
object SystemCategoryStrings {
    private val byKey: Map<String, Int> =
        mapOf(
            "food" to R.string.category_food,
            "groceries" to R.string.category_groceries,
            "restaurants" to R.string.category_restaurants,
            "transport" to R.string.category_transport,
            "public_transport" to R.string.category_public_transport,
            "fuel" to R.string.category_fuel,
            "taxi" to R.string.category_taxi,
            "housing" to R.string.category_housing,
            "rent" to R.string.category_rent,
            "utilities" to R.string.category_utilities,
            "health" to R.string.category_health,
            "education" to R.string.category_education,
            "entertainment" to R.string.category_entertainment,
            "shopping" to R.string.category_shopping,
            "other_expense" to R.string.category_other_expense,
            "salary" to R.string.category_salary,
            "freelance" to R.string.category_freelance,
            "gifts" to R.string.category_gifts,
            "interest" to R.string.category_interest,
            "other_income" to R.string.category_other_income,
        )

    val keys: Set<String> get() = byKey.keys

    @StringRes
    fun resIdFor(systemKey: String): Int? = byKey[systemKey]
}
