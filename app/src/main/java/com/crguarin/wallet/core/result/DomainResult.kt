package com.crguarin.wallet.core.result

/**
 * Result wrapper for domain operations, encapsulating either a successful [value]
 * or a domain-specific [error].
 */
sealed interface DomainResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : DomainResult<T>

    data class Failure(
        val error: DomainError,
    ) : DomainResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    fun errorOrNull(): DomainError? =
        when (this) {
            is Success -> null
            is Failure -> error
        }

    companion object {
        val UnitSuccess: DomainResult<Unit> = Success(Unit)
    }
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R,
): R =
    when (this) {
        is DomainResult.Success -> onSuccess(value)
        is DomainResult.Failure -> onFailure(error)
    }

inline fun <T> DomainResult<T>.onSuccess(action: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) action(value)
    return this
}

inline fun <T> DomainResult<T>.onFailure(action: (DomainError) -> Unit): DomainResult<T> {
    if (this is DomainResult.Failure) action(error)
    return this
}
