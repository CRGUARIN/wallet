package com.crguarin.wallet.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import com.crguarin.wallet.R
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(
        route = Home,
        routeClass = Home::class,
        labelRes = R.string.nav_home,
        icon = Icons.Default.Home,
    ),
    TRANSACTIONS(
        route = Transactions(),
        routeClass = Transactions::class,
        labelRes = R.string.nav_transactions,
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
    ),
    REPORTS(
        route = Reports,
        routeClass = Reports::class,
        labelRes = R.string.nav_reports,
        icon = Icons.Default.PieChart,
    ),
    ACCOUNTS(
        route = Accounts,
        routeClass = Accounts::class,
        labelRes = R.string.nav_accounts,
        icon = Icons.Default.AccountBalanceWallet,
    ),
    SETTINGS(
        route = Settings,
        routeClass = Settings::class,
        labelRes = R.string.nav_settings,
        icon = Icons.Default.Settings,
    ),
}

/**
 * Index of the back stack entry that roots the current tab, given the tab of each entry (null for
 * screens that are not tabs), oldest first; -1 for an empty stack.
 *
 * The entry right above the start destination (Home) roots the stack when it is a tab: in
 * `[Home, Reports, Transactions]` it is Reports, so the Transactions list opened from Reports keeps
 * the Reports tab selected. Otherwise the start entry itself is the root: in
 * `[Home, AccountDetail, Transactions]` (an account opened from Home) the stack belongs to Home.
 */
fun ownerRootIndex(stackTabs: List<TopLevelDestination?>): Int =
    when {
        stackTabs.isEmpty() -> -1
        stackTabs.first() == TopLevelDestination.HOME && stackTabs.getOrNull(1) != null -> 1
        else -> 0
    }

/** Tab that owns the current back stack (see [ownerRootIndex]); HOME when the root is not a tab. */
fun ownerTopLevelDestination(stackTabs: List<TopLevelDestination?>): TopLevelDestination =
    stackTabs.getOrNull(ownerRootIndex(stackTabs)) ?: TopLevelDestination.HOME

@Composable
fun WalletBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // NavController.currentBackStack is restricted; the public back stack of the composable
    // destinations lives on ComposeNavigator.
    val composeNavigator =
        remember(navController) {
            navController.navigatorProvider.getNavigator(ComposeNavigator::class.java)
        }
    val backStack by composeNavigator.backStack.collectAsStateWithLifecycle()
    val stackTabs =
        backStack.map { entry ->
            TopLevelDestination.entries.firstOrNull { entry.destination.hasRoute(it.routeClass) }
        }
    val rootIndex = ownerRootIndex(stackTabs)
    val ownerTab = ownerTopLevelDestination(stackTabs)

    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == ownerTab,
                onClick = {
                    val rootEntry = backStack.getOrNull(rootIndex)
                    if (destination == ownerTab && rootEntry != null) {
                        // Tapping the owning tab pops back to its root instead of restoring the
                        // saved stack (which would show the nested screen again). Already at the
                        // root: nothing to do.
                        if (rootEntry != backStack.last()) {
                            navController.popBackStack(rootEntry.destination.id, inclusive = false)
                        }
                    } else {
                        navController.navigateToTopLevelDestination(destination.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = {
                    // With 5 destinations and font scale 2.0 the label must not wrap or overlap.
                    Text(
                        text = stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
