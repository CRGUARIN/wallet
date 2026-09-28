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

    data class BackupInvalid(
        val reason: String,
    ) : DomainError

    data object BackupVersionUnsupported : DomainError
}
