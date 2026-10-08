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

    data object DebtAmountBelowPaid : DomainError

    data object DebtPaymentExceedsRemaining : DomainError

    data object DebtDueBeforeDate : DomainError

    data object DebtAlreadySettled : DomainError

    data object DebtPaymentBeforeDate : DomainError

    data object DebtDateAfterPayments : DomainError

    data object DebtDirectionLocked : DomainError

    data object CardDayInvalid : DomainError

    data object FareInvalid : DomainError

    /** A transit card cannot start owing more than one fare (the single credit trip). */
    data object TransitCreditTooLarge : DomainError

    /** The credit trip is already used: the transit card needs a top-up before another trip. */
    data object TransitCreditUsed : DomainError

    data object ColorInUse : DomainError

    data object AccountTypeChangeNotAllowed : DomainError

    data class BackupInvalid(
        val reason: String,
    ) : DomainError

    data object BackupVersionUnsupported : DomainError

    data object BackupFailed : DomainError
}
