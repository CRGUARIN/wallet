package com.crguarin.wallet.ui.util

import androidx.annotation.StringRes
import com.crguarin.wallet.R
import com.crguarin.wallet.core.result.DomainError

/**
 * Maps any [DomainError] to its localized string resource.
 */
@StringRes
fun DomainError.asStringRes(): Int =
    when (this) {
        is DomainError.NameEmpty -> R.string.error_name_empty
        is DomainError.NameTooLong -> R.string.error_name_too_long
        is DomainError.NameDuplicated -> R.string.error_name_duplicated
        is DomainError.AmountInvalid -> R.string.error_amount_invalid
        is DomainError.SameAccountTransfer -> R.string.error_same_account_transfer
        is DomainError.CategoryRequired -> R.string.error_category_required
        is DomainError.CategoryTypeMismatch -> R.string.error_category_type_mismatch
        is DomainError.MaxDepthExceeded -> R.string.error_max_depth_exceeded
        is DomainError.AccountHasTransactions -> R.string.error_account_has_transactions
        is DomainError.CategoryHasChildren -> R.string.error_category_has_children
        is DomainError.CategoryHasTransactions -> R.string.error_category_has_transactions
        is DomainError.NotFound -> R.string.error_not_found
        is DomainError.NoteTooLong -> R.string.error_note_too_long
        is DomainError.DateOutOfRange -> R.string.error_date_out_of_range
        is DomainError.AccountArchived -> R.string.error_account_archived
        is DomainError.CategoryArchived -> R.string.error_category_archived
        is DomainError.SameCategoryReassignment -> R.string.error_same_category_reassignment
        is DomainError.InvalidTransaction -> R.string.error_invalid_transaction
        is DomainError.BackupInvalid -> R.string.error_backup_invalid
        is DomainError.BackupVersionUnsupported -> R.string.error_backup_version_unsupported
    }
