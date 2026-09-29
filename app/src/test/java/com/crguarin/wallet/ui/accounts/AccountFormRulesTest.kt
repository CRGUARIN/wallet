package com.crguarin.wallet.ui.accounts

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.ui.util.asStringRes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccountFormRulesTest {
    @Test
    fun `default icon follows the type`() {
        assertThat(iconAfterTypeChange(AccountType.SAVINGS, "wallet")).isEqualTo("savings")
        assertThat(iconAfterTypeChange(AccountType.CASH, "savings")).isEqualTo("wallet")
    }

    @Test
    fun `custom icons are kept`() {
        assertThat(iconAfterTypeChange(AccountType.SAVINGS, "bank")).isEqualTo("bank")
        assertThat(iconAfterTypeChange(AccountType.CASH, "bank")).isEqualTo("bank")
        assertThat(iconAfterTypeChange(AccountType.CASH, "wallet")).isEqualTo("wallet")
    }

    @Test
    fun `name input error covers too long and blank names`() {
        assertThat(accountNameInputError("a".repeat(DomainLimits.NAME_MAX_LENGTH + 1)))
            .isEqualTo(DomainError.NameTooLong.asStringRes())
        assertThat(accountNameInputError("   ")).isEqualTo(DomainError.NameEmpty.asStringRes())
    }

    @Test
    fun `valid or empty names have no input error`() {
        assertThat(accountNameInputError("")).isNull()
        assertThat(accountNameInputError("Efectivo")).isNull()
        assertThat(accountNameInputError("a".repeat(DomainLimits.NAME_MAX_LENGTH))).isNull()
    }
}
