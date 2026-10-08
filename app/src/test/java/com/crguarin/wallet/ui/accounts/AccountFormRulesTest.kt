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
    fun `selecting a card swaps the default icons for credit_card`() {
        assertThat(iconAfterTypeChange(AccountType.CREDIT_CARD, "wallet")).isEqualTo("credit_card")
        assertThat(iconAfterTypeChange(AccountType.CREDIT_CARD, "savings")).isEqualTo("credit_card")
        assertThat(iconAfterTypeChange(AccountType.CREDIT_CARD, "credit_card")).isEqualTo("credit_card")
    }

    @Test
    fun `leaving a card restores the default icon of the new type`() {
        assertThat(iconAfterTypeChange(AccountType.CASH, "credit_card")).isEqualTo("wallet")
        assertThat(iconAfterTypeChange(AccountType.SAVINGS, "credit_card")).isEqualTo("savings")
    }

    @Test
    fun `custom icons survive card type changes`() {
        assertThat(iconAfterTypeChange(AccountType.CREDIT_CARD, "bank")).isEqualTo("bank")
        assertThat(iconAfterTypeChange(AccountType.CASH, "bank")).isEqualTo("bank")
    }

    @Test
    fun `selecting a transit card swaps the default icons for directions_bus`() {
        assertThat(iconAfterTypeChange(AccountType.TRANSIT_CARD, "wallet")).isEqualTo("directions_bus")
        assertThat(iconAfterTypeChange(AccountType.TRANSIT_CARD, "savings")).isEqualTo("directions_bus")
        assertThat(iconAfterTypeChange(AccountType.TRANSIT_CARD, "credit_card")).isEqualTo("directions_bus")
    }

    @Test
    fun `leaving a transit card restores the default icon of the new type`() {
        assertThat(iconAfterTypeChange(AccountType.CASH, "directions_bus")).isEqualTo("wallet")
        assertThat(iconAfterTypeChange(AccountType.SAVINGS, "directions_bus")).isEqualTo("savings")
        assertThat(iconAfterTypeChange(AccountType.CREDIT_CARD, "directions_bus")).isEqualTo("credit_card")
    }

    @Test
    fun `custom icons survive transit card type changes`() {
        assertThat(iconAfterTypeChange(AccountType.TRANSIT_CARD, "bank")).isEqualTo("bank")
        assertThat(iconAfterTypeChange(AccountType.CASH, "bank")).isEqualTo("bank")
    }

    @Test
    fun `parseCardDay accepts 1 to 31`() {
        assertThat(parseCardDay("1")).isEqualTo(1)
        assertThat(parseCardDay("15")).isEqualTo(15)
        assertThat(parseCardDay("31")).isEqualTo(31)
        assertThat(parseCardDay(" 7 ")).isEqualTo(7)
    }

    @Test
    fun `parseCardDay rejects out of range and non numeric input`() {
        assertThat(parseCardDay("0")).isNull()
        assertThat(parseCardDay("32")).isNull()
        assertThat(parseCardDay("-5")).isNull()
        assertThat(parseCardDay("")).isNull()
        assertThat(parseCardDay("abc")).isNull()
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
