package com.crguarin.wallet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R
import com.crguarin.wallet.core.resources.SystemCategoryStrings
import com.crguarin.wallet.domain.model.Category

/**
 * Resolves the user-visible name of a category.
 * If [name] is non-null, uses it; otherwise resolves [systemKey] via [SystemCategoryStrings].
 */
@Composable
fun categoryDisplayName(
    name: String?,
    systemKey: String?,
): String {
    if (!name.isNullOrBlank()) return name
    val resId = systemKey?.let { SystemCategoryStrings.resIdFor(it) }
    return if (resId != null) stringResource(resId) else systemKey.orEmpty()
}

@Composable
fun Category.displayName(): String = categoryDisplayName(name, systemKey)

/**
 * Builds the user-visible label for a category, prefixing its parent when it is a
 * subcategory ("Parent › Child").
 */
@Composable
fun categoryFullDisplayName(
    parent: Category?,
    category: Category?,
): String =
    when {
        category == null -> ""
        parent == null -> category.displayName()
        else -> stringResource(R.string.category_breadcrumb, parent.displayName(), category.displayName())
    }
