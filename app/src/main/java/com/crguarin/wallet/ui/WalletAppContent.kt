package com.crguarin.wallet.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.navigation.Home
import com.crguarin.wallet.ui.navigation.Onboarding
import com.crguarin.wallet.ui.navigation.TransactionEdit
import com.crguarin.wallet.ui.navigation.Transactions
import com.crguarin.wallet.ui.navigation.WalletBottomBar
import com.crguarin.wallet.ui.navigation.WalletNavHost
import com.crguarin.wallet.ui.navigation.isTopLevel

@Composable
fun WalletAppContent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Home,
    /**
     * Launch intent to handle notification deep links from. Null (production) uses the hosting
     * Activity's intent; tests pass a crafted one. Ignored while onboarding is incomplete.
     */
    deepLinkIntent: Intent? = null,
) {
    val context = LocalContext.current
    val launchIntent = deepLinkIntent ?: remember { context.findActivity()?.intent }
    if (startDestination != Onboarding) {
        // Navigation-compose does not handle the Activity intent on its own: the notification
        // PendingIntent recreates the Activity (CLEAR_TASK), so handle it once the graph exists.
        LaunchedEffect(navController, launchIntent) {
            if (launchIntent != null) {
                navController.handleDeepLink(launchIntent)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination = currentDestination.isTopLevel()

    val isFabDestination =
        currentDestination?.hierarchy?.any {
            it.hasRoute(Home::class) || it.hasRoute(Transactions::class)
        } == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Each screen's TopAppBar pads for the status bar; here only the navigation bar is reserved.
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            // Animated so the bar does not pop in or out while the screens cross-fade.
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                WalletBottomBar(
                    navController = navController,
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
