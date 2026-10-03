package com.crguarin.wallet.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OwnerTopLevelDestinationTest {
    @Test
    fun `home stack is owned by home`() {
        assertThat(ownerTopLevelDestination(listOf(Home::class)))
            .isEqualTo(TopLevelDestination.HOME)
    }

    @Test
    fun `empty stack is owned by home`() {
        assertThat(ownerTopLevelDestination(emptyList()))
            .isEqualTo(TopLevelDestination.HOME)
    }

    @Test
    fun `transactions opened from reports belongs to reports`() {
        assertThat(
            ownerTopLevelDestination(listOf(Home::class, Reports::class, Transactions::class)),
        ).isEqualTo(TopLevelDestination.REPORTS)
    }

    @Test
    fun `transactions opened from account detail belongs to accounts`() {
        assertThat(
            ownerTopLevelDestination(
                listOf(Home::class, Accounts::class, AccountDetail::class, Transactions::class),
            ),
        ).isEqualTo(TopLevelDestination.ACCOUNTS)
    }

    @Test
    fun `tab destination right after home owns the stack`() {
        assertThat(ownerTopLevelDestination(listOf(Home::class, Transactions::class)))
            .isEqualTo(TopLevelDestination.TRANSACTIONS)
    }

    @Test
    fun `detail screen without a tab belongs to home`() {
        assertThat(ownerTopLevelDestination(listOf(Home::class, TransactionEdit::class)))
            .isEqualTo(TopLevelDestination.HOME)
    }
}
