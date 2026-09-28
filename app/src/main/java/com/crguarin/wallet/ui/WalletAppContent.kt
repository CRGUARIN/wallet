package com.crguarin.wallet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.navigation.Home
import com.crguarin.wallet.ui.navigation.TopLevelDestination
import com.crguarin.wallet.ui.navigation.TransactionEdit
import com.crguarin.wallet.ui.navigation.Transactions
import com.crguarin.wallet.ui.navigation.WalletBottomBar
import com.crguarin.wallet.ui.navigation.WalletNavHost

@Composable
fun WalletAppContent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination =
        TopLevelDestination.entries.any { destination ->
            currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
        }

    val isFabDestination =
        currentDestination?.hierarchy?.any {
            it.hasRoute(Home::class) || it.hasRoute(Transactions::class)
        } == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                WalletBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                )
            }
        },
        floatingActionButton = {
            if (isFabDestination) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(TransactionEdit())
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_new_transaction),
                    )
                }
            }
        },
    ) { innerPadding ->
        WalletNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
