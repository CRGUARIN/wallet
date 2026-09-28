package com.crguarin.wallet.ui.navigation

import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

@Serializable
data object Home

@Serializable
data class Transactions(
    val accountId: Long? = null,
    val categoryId: Long? = null,
)

@Serializable
data class TransactionEdit(
    val transactionId: Long? = null,
    val initialType: TransactionType = TransactionType.EXPENSE,
    val accountId: Long? = null,
)

@Serializable
data object Accounts

@Serializable
data class AccountDetail(
    val accountId: Long,
)

@Serializable
data class AccountEdit(
    val accountId: Long? = null,
)

@Serializable
data class Categories(
    val type: CategoryType = CategoryType.EXPENSE,
)

@Serializable
data class CategoryEdit(
    val categoryId: Long? = null,
    val parentId: Long? = null,
    val type: CategoryType,
)

@Serializable
data object Settings

/**
 * Key used to return the id of a category created from the transaction editor.
 * `CategoryEditScreen` sets it on the previous back stack entry's [androidx.lifecycle.SavedStateHandle]
 * and [com.crguarin.wallet.ui.transactions.TransactionEditViewModel] reads it to select the new category.
 */
const val NEW_CATEGORY_RESULT_KEY = "new_category_result_key"
