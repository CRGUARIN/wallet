package com.crguarin.wallet.ui.navigation

import com.crguarin.wallet.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun `bottom bar has the four daily destinations in order`() {
        assertThat(TopLevelDestination.entries)
            .containsExactly(
                TopLevelDestination.HOME,
                TopLevelDestination.TRANSACTIONS,
                TopLevelDestination.ACCOUNTS,
                TopLevelDestination.REPORTS,
            ).inOrder()
        assertThat(TopLevelDestination.entries.map { it.labelRes })
            .containsExactly(R.string.nav_home, R.string.nav_transactions, R.string.nav_accounts, R.string.nav_reports)
            .inOrder()
    }

    @Test
    fun `settings is not a top level destination`() {
        assertThat(TopLevelDestination.entries.map { it.routeClass }).doesNotContain(Settings::class)
        assertThat(TopLevelDestination.entries.map { it.route }).doesNotContain(Settings)
    }

    @Test
    fun `a stack with settings on top of home is owned by home`() {
        // [Home, Settings]: Settings is not a tab (null), so Home keeps the stack.
        val stack = listOf(TopLevelDestination.HOME, null)
        assertThat(ownerTopLevelDestination(stack)).isEqualTo(TopLevelDestination.HOME)
        assertThat(ownerRootIndex(stack)).isEqualTo(0)
    }
}
