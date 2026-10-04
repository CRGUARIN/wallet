package com.crguarin.wallet.core.result

/**
 * Domain-level errors returned by use cases and domain operations.
 */
sealed interface DomainError {
    data object NameEmpty : DomainError

    data object NameTooLong : DomainError

    data object NameDuplicated : DomainError

    data object AmountInvalid : DomainError

    data object SameAccountTransfer : DomainError

    data object CategoryRequired : DomainError

    data object CategoryTypeMismatch : DomainError

    data object MaxDepthExceeded : DomainError

    data object AccountHasTransactions : DomainError

    data object CategoryHasChildren : DomainError

    data class CategoryHasTransactions(
        val count: Int,
    ) : DomainError

    data object NotFound : DomainError

    data object NoteTooLong : DomainError

    data object DateOutOfRange : DomainError

    data object AccountArchived : DomainError

    data object CategoryArchived : DomainError

    data object SameCategoryReassignment : DomainError

    data object InvalidTransaction : DomainError

    data object RecurringDateInPast : DomainError

    data object RecurringEndBeforeStart : DomainError

    data object RecurringIntervalInvalid : DomainError

    data object AccountHasRecurring : DomainError

    data object CategoryHasRecurring : DomainError

    data object CardDayInvalid : DomainError

    data object AccountTypeChangeNotAllowed : DomainError

    data class BackupInvalid(
        val reason: String,
    ) : DomainError

    data object BackupVersionUnsupported : DomainError

    data object BackupFailed : DomainError
}
