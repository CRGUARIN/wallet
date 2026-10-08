package com.crguarin.wallet.ui.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.result.DomainError
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DomainErrorStringTest {
    @Test
    fun `every domain error resolves to a valid readable string resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val allErrors: List<DomainError> =
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
                DomainError.CategoryHasTransactions(5),
                DomainError.NotFound,
                DomainError.NoteTooLong,
                DomainError.DateOutOfRange,
                DomainError.AccountArchived,
                DomainError.CategoryArchived,
                DomainError.SameCategoryReassignment,
                DomainError.InvalidTransaction,
                DomainError.DebtAmountBelowPaid,
                DomainError.DebtPaymentExceedsRemaining,
                DomainError.DebtDueBeforeDate,
                DomainError.DebtAlreadySettled,
                DomainError.CardDayInvalid,
                DomainError.AccountTypeChangeNotAllowed,
                DomainError.BackupInvalid("corrupted"),
                DomainError.BackupVersionUnsupported,
                DomainError.BackupFailed,
            )

        for (error in allErrors) {
            val resId = error.asStringRes()
            assertThat(resId).isGreaterThan(0)
            val text = context.getString(resId)
            assertThat(text).isNotEmpty()
        }
    }
}
