package com.crguarin.wallet.ui.navigation

import com.crguarin.wallet.ui.navigation.TopLevelDestination.ACCOUNTS
import com.crguarin.wallet.ui.navigation.TopLevelDestination.HOME
import com.crguarin.wallet.ui.navigation.TopLevelDestination.REPORTS
import com.crguarin.wallet.ui.navigation.TopLevelDestination.TRANSACTIONS
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Stacks are given as the tab of each entry, oldest first; null marks a screen that is not a tab. */
class OwnerTopLevelDestinationTest {
    @Test
    fun `home stack is owned by home`() {
        assertThat(ownerTopLevelDestination(listOf(HOME))).isEqualTo(HOME)
        assertThat(ownerRootIndex(listOf(HOME))).isEqualTo(0)
    }

    @Test
    fun `empty stack is owned by home`() {
        assertThat(ownerTopLevelDestination(emptyList())).isEqualTo(HOME)
        assertThat(ownerRootIndex(emptyList())).isEqualTo(-1)
    }

    @Test
    fun `transactions opened from reports belongs to reports`() {
        val stack = listOf(HOME, REPORTS, TRANSACTIONS)
        assertThat(ownerTopLevelDestination(stack)).isEqualTo(REPORTS)
        assertThat(ownerRootIndex(stack)).isEqualTo(1)
    }

    @Test
    fun `transactions opened from account detail belongs to accounts`() {
        // [Home, Accounts, AccountDetail, Transactions(accountId)]
        val stack = listOf(HOME, ACCOUNTS, null, TRANSACTIONS)
        assertThat(ownerTopLevelDestination(stack)).isEqualTo(ACCOUNTS)
        assertThat(ownerRootIndex(stack)).isEqualTo(1)
    }

    @Test
    fun `account opened from home keeps the stack in home`() {
        // [Home, AccountDetail, Transactions(accountId)]: the filtered list is not the Transactions tab.
        val stack = listOf(HOME, null, TRANSACTIONS)
        assertThat(ownerTopLevelDestination(stack)).isEqualTo(HOME)
        assertThat(ownerRootIndex(stack)).isEqualTo(0)
    }

    @Test
    fun `tab destination right after home owns the stack`() {
        assertThat(ownerTopLevelDestination(listOf(HOME, TRANSACTIONS))).isEqualTo(TRANSACTIONS)
    }

    @Test
    fun `detail screen without a tab belongs to home`() {
        assertThat(ownerTopLevelDestination(listOf(HOME, null))).isEqualTo(HOME)
    }

    @Test
    fun `stack started on another tab is owned by that tab`() {
        assertThat(ownerTopLevelDestination(listOf(REPORTS, TRANSACTIONS))).isEqualTo(REPORTS)
        assertThat(ownerRootIndex(listOf(REPORTS, TRANSACTIONS))).isEqualTo(0)
    }
}
