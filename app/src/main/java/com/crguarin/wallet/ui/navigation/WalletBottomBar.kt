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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
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

@Composable
fun WalletBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any {
                    it.hasRoute(destination.routeClass)
                } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigateToTopLevelDestination(destination.route)
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
