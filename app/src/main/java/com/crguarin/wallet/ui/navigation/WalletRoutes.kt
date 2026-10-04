package com.crguarin.wallet.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
    val type: TransactionType? = null,
    /** Selected month in ISO `yyyy-MM` format; null means the current month. */
    val month: String? = null,
)

@Serializable
data class TransactionEdit(
    val transactionId: Long? = null,
    val initialType: TransactionType = TransactionType.EXPENSE,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val initialAmountMinor: Long? = null,
)

@Serializable
data object Reports

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
data class CardReconcile(
    val accountId: Long,
)

@Serializable
data object RecurringList

@Serializable
data class RecurringEdit(
    val paymentId: Long? = null,
)

@Serializable
data object Settings

/**
 * Key used to return the id of a category created from the transaction editor.
 * `CategoryEditScreen` sets it on the previous back stack entry's [androidx.lifecycle.SavedStateHandle]
 * and [com.crguarin.wallet.ui.transactions.TransactionEditViewModel] reads it to select the new category.
 */
const val NEW_CATEGORY_RESULT_KEY = "new_category_result_key"

/**
 * Navigates to a bottom-bar destination without duplicating it in the back stack: it pops up to the
 * graph's start destination saving state, and restores the destination state when returning.
 */
fun NavHostController.navigateToTopLevelDestination(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
