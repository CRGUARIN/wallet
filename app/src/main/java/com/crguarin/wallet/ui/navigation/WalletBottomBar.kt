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
 * Tab that owns the current back stack: the first tab destination after the initial one (Home),
 * or HOME when the stack holds no other tab. For example `[Home, Reports, Transactions]` belongs
 * to REPORTS, so the Transactions list opened from Reports keeps the Reports tab selected.
 *
 * [routeClasses] are the route classes of the back stack entries in order, oldest first.
 * Non-tab entries are ignored.
 */
fun ownerTopLevelDestination(routeClasses: List<KClass<*>>): TopLevelDestination {
    val afterInitial = if (routeClasses.isEmpty()) emptyList() else routeClasses.drop(1)
    return afterInitial.firstNotNullOfOrNull { routeClass ->
        TopLevelDestination.entries.firstOrNull { it.routeClass == routeClass }
    } ?: TopLevelDestination.HOME
}

@Composable
fun WalletBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // NavController.currentBackStack is restricted; the public back stack of the composable
    // destinations lives on ComposeNavigator ("composable" is its name; NAME is internal).
    val composeNavigator =
        remember(navController) {
            navController.navigatorProvider.getNavigator<ComposeNavigator>("composable")
        }
    val backStack by composeNavigator.backStack.collectAsStateWithLifecycle()
    val routeClasses =
        backStack.mapNotNull { entry ->
            TopLevelDestination.entries
                .firstOrNull { entry.destination.hasRoute(it.routeClass) }
                ?.routeClass
        }
    val ownerTab = ownerTopLevelDestination(routeClasses)

    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == ownerTab,
                onClick = {
                    if (destination == ownerTab) {
                        // Tapping the owning tab pops back to its root instead of restoring the
                        // saved stack (which would show the nested screen again).
                        val rootEntry =
                            backStack.firstOrNull { entry ->
                                entry.destination.hasRoute(destination.routeClass)
                            }
                        if (rootEntry != null && rootEntry != backStack.lastOrNull()) {
                            navController.popBackStack(rootEntry.destination.id, inclusive = false)
                        } else if (rootEntry == null) {
                            navController.navigateToTopLevelDestination(destination.route)
                        }
                        // Already at the tab root: nothing to do.
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
