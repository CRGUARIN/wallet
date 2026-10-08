package com.crguarin.wallet.core.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DomainResultTest {
    @Test
    fun `success properties and getters`() {
        val result: DomainResult<String> = DomainResult.Success("wallet")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo("wallet")
        assertThat(result.errorOrNull()).isNull()
    }

    @Test
    fun `failure properties and getters`() {
        val error = DomainError.AmountInvalid
        val result: DomainResult<String> = DomainResult.Failure(error)

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
        assertThat(result.getOrNull()).isNull()
        assertThat(result.errorOrNull()).isEqualTo(error)
    }

    @Test
    fun `map transforms value on success`() {
        val success: DomainResult<Int> = DomainResult.Success(10)
        val mapped = success.map { it * 2 }

        assertThat(mapped.getOrNull()).isEqualTo(20)
    }

    @Test
    fun `map propagates failure unchanged`() {
        val failure: DomainResult<Int> = DomainResult.Failure(DomainError.NameEmpty)
        val mapped = failure.map { it * 2 }

        assertThat(mapped.errorOrNull()).isEqualTo(DomainError.NameEmpty)
    }

    @Test
    fun `fold branches correctly for success and failure`() {
        val success: DomainResult<Int> = DomainResult.Success(42)
        val successFolded =
            success.fold(
                onSuccess = { "Value: $it" },
                onFailure = { "Error" },
            )
        assertThat(successFolded).isEqualTo("Value: 42")

        val failure: DomainResult<Int> = DomainResult.Failure(DomainError.NotFound)
        val failureFolded =
            failure.fold(
                onSuccess = { "Value: $it" },
                onFailure = { "Error: ${it::class.simpleName}" },
            )
        assertThat(failureFolded).isEqualTo("Error: NotFound")
    }

    @Test
    fun `onSuccess and onFailure callbacks work conditionally`() {
        var successCalled = false
        var failureCalled = false

        DomainResult
            .Success("ok")
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }

        assertThat(successCalled).isTrue()
        assertThat(failureCalled).isFalse()

        successCalled = false
        failureCalled = false

        DomainResult
            .Failure(DomainError.CategoryRequired)
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }

        assertThat(successCalled).isFalse()
        assertThat(failureCalled).isTrue()
    }

    @Test
    fun `unit success constant provides Unit value`() {
        val unitResult = DomainResult.UnitSuccess
        assertThat(unitResult.isSuccess).isTrue()
        assertThat(unitResult.getOrNull()).isEqualTo(Unit)
    }

    @Test
    fun `all domain errors are represented and match sealed hierarchy`() {
        val errors: List<DomainError> =
            listOf(
                DomainError.NameEmpty,
                DomainError.NameTooLong,
                DomainError.NameDuplicated,
                DomainError.AmountInvalid,
                DomainError.SameAccountTransfer,
                DomainError.CategoryRequired,
                DomainError.CategoryTypeMismatch,
                DomainError.MaxDepthExceeded,
                DomainError.AccountHasTransactions,
                DomainError.CategoryHasChildren,
                DomainError.CategoryHasTransactions(count = 5),
                DomainError.NotFound,
                DomainError.NoteTooLong,
                DomainError.DateOutOfRange,
                DomainError.AccountArchived,
                DomainError.CategoryArchived,
                DomainError.SameCategoryReassignment,
                DomainError.InvalidTransaction,
                DomainError.RecurringDateInPast,
                DomainError.RecurringEndBeforeStart,
                DomainError.RecurringIntervalInvalid,
                DomainError.AccountHasRecurring,
                DomainError.CategoryHasRecurring,
                DomainError.DebtAmountBelowPaid,
                DomainError.DebtPaymentExceedsRemaining,
                DomainError.DebtDueBeforeDate,
                DomainError.DebtAlreadySettled,
                DomainError.CardDayInvalid,
                DomainError.AccountTypeChangeNotAllowed,
                DomainError.BackupInvalid(reason = "Corrupt JSON"),
                DomainError.BackupVersionUnsupported,
                DomainError.BackupFailed,
            )

        assertThat(errors).hasSize(32)

        for (err in errors) {
            val description =
                when (err) {
                    DomainError.NameEmpty -> "NameEmpty"
                    DomainError.NameTooLong -> "NameTooLong"
                    DomainError.NameDuplicated -> "NameDuplicated"
                    DomainError.AmountInvalid -> "AmountInvalid"
                    DomainError.SameAccountTransfer -> "SameAccountTransfer"
                    DomainError.CategoryRequired -> "CategoryRequired"
                    DomainError.CategoryTypeMismatch -> "CategoryTypeMismatch"
                    DomainError.MaxDepthExceeded -> "MaxDepthExceeded"
                    DomainError.AccountHasTransactions -> "AccountHasTransactions"
                    DomainError.CategoryHasChildren -> "CategoryHasChildren"
                    is DomainError.CategoryHasTransactions -> "CategoryHasTransactions(${err.count})"
                    DomainError.NotFound -> "NotFound"
                    DomainError.NoteTooLong -> "NoteTooLong"
                    DomainError.DateOutOfRange -> "DateOutOfRange"
                    DomainError.AccountArchived -> "AccountArchived"
                    DomainError.CategoryArchived -> "CategoryArchived"
                    DomainError.SameCategoryReassignment -> "SameCategoryReassignment"
                    DomainError.InvalidTransaction -> "InvalidTransaction"
                    DomainError.RecurringDateInPast -> "RecurringDateInPast"
                    DomainError.RecurringEndBeforeStart -> "RecurringEndBeforeStart"
                    DomainError.RecurringIntervalInvalid -> "RecurringIntervalInvalid"
                    DomainError.AccountHasRecurring -> "AccountHasRecurring"
                    DomainError.CategoryHasRecurring -> "CategoryHasRecurring"
                    DomainError.DebtAmountBelowPaid -> "DebtAmountBelowPaid"
                    DomainError.DebtPaymentExceedsRemaining -> "DebtPaymentExceedsRemaining"
                    DomainError.DebtDueBeforeDate -> "DebtDueBeforeDate"
                    DomainError.DebtAlreadySettled -> "DebtAlreadySettled"
                    DomainError.CardDayInvalid -> "CardDayInvalid"
                    DomainError.AccountTypeChangeNotAllowed -> "AccountTypeChangeNotAllowed"
                    is DomainError.BackupInvalid -> "BackupInvalid(${err.reason})"
                    DomainError.BackupVersionUnsupported -> "BackupVersionUnsupported"
                    DomainError.BackupFailed -> "BackupFailed"
                }
            assertThat(description).isNotEmpty()
        }
    }
}
