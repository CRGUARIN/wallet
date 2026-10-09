package com.crguarin.wallet.ui.transactions

import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.model.card.CardOverview
import java.time.LocalDate

/**
 * Transaction types offered in the creation tabs: Gasto | Ingreso | Transferencia.
 */
val TRANSACTION_CREATION_TYPES: List<TransactionType> =
    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

/** How many frequent categories the form offers as chips at most. */
const val FREQUENT_CATEGORIES_LIMIT = 6

/** The frequent categories are the most used ones in this many days. */
const val FREQUENT_CATEGORIES_DAYS = 90L

/**
 * A category offered as a chip under the category selector. [parent] is kept so the chip can read
 * "Parent › Child" like the selector does.
 */
data class FrequentCategory(
    val category: Category,
    val parent: Category? = null,
)

data class TransactionEditUiState(
    val isLoading: Boolean = true,
    /** Editing an id that no longer exists (e.g. deleted from another screen). */
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val editingTransactionId: Long? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val amountErrorRes: Int? = null,
    val accountId: Long? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val archivedAccount: Account? = null,
    val toAccountId: Long? = null,
    val archivedToAccount: Account? = null,
    val categoryId: Long? = null,
    val categoryNodes: List<CategoryNode> = emptyList(),
    val selectedCategory: Category? = null,
    val selectedParentCategory: Category? = null,
    val categoryErrorRes: Int? = null,
    /** Most used active categories of the current type in the last 90 days; empty for transfers. */
    val frequentCategories: List<FrequentCategory> = emptyList(),
    val date: LocalDate? = null,
    val dateErrorRes: Int? = null,
    val note: String = "",
    val noteErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
    val isSaving: Boolean = false,
    /** Warning that triggered the confirmation dialog; NONE when the dialog is not about balance. */
    val balanceWarning: BalanceWarning = BalanceWarning.NONE,
    /** Card debt and available credit, shown for cards in the account pickers. */
    val cardOverviews: Map<Long, CardOverview> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    /** Number of installments for a card purchase; 1 means a single payment (saved as null). */
    val installments: Int = 1,
    val showNegativeBalanceDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showToAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showDatePicker: Boolean = false,
    /** Name of the recurring rule that generated this movement, when it still exists. */
    val recurringSourceName: String? = null,
    /** Recurring rule that generated the edited movement; preserved on save. */
    val recurringId: Long? = null,
) {
    /** Active accounts; transfers need at least two of them. */
    val activeAccountCount: Int
        get() = accounts.size

    val showNoAccounts: Boolean
        get() =
            !isLoading &&
                !isEditMode &&
                selectedType != TransactionType.TRANSFER &&
                accounts.isEmpty() &&
                generalErrorRes == null

    /** Type of the selected origin account, looking in archived accounts too. */
    val selectedAccountType: AccountType?
        get() =
            (
                accounts.firstOrNull { it.account.id == accountId }?.account
                    ?: archivedAccount
            )?.type

    /** The installments field is only offered for card purchases. */
    val showInstallments: Boolean
        get() = selectedType == TransactionType.EXPENSE && selectedAccountType == AccountType.CREDIT_CARD

    /** A transfer needs a second active account; otherwise the transfer tab offers to create one. */
    val showTransferNeedsAccounts: Boolean
        get() =
            !isLoading &&
                !isEditMode &&
                selectedType == TransactionType.TRANSFER &&
                accounts.size < 2 &&
                generalErrorRes == null

    val isSaveEnabled: Boolean
        get() {
            val destinationValid =
                if (selectedType == TransactionType.TRANSFER) {
                    toAccountId != null && toAccountId != accountId
                } else {
                    categoryId != null
                }
            return !isLoading &&
                !isSaving &&
                !showNoAccounts &&
                !showTransferNeedsAccounts &&
                MoneyParser.parse(amountText) != null &&
                accountId != null &&
                destinationValid &&
                noteErrorRes == null &&
                date != null &&
                dateErrorRes == null
        }
}

sealed interface TransactionEditEvent {
    data object NavigateBack : TransactionEditEvent

    data class OpenCategoryEdit(
        val type: CategoryType,
    ) : TransactionEditEvent

    data object OpenAccountEdit : TransactionEditEvent
}
