package com.crguarin.wallet.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.motion.LocalMotionPolicy
import com.crguarin.wallet.ui.motion.rememberSystemMotionPolicy
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
     * Deep link intent to handle explicitly. Production does not need it: when the NavHost sets
     * the graph, [NavHostController] handles the hosting Activity's intent on its own (once, and
     * only if the graph registers deep links). Tests inject an intent here because the test
     * Activity has no deep link intent of its own. Ignored while onboarding is incomplete.
     */
    deepLinkIntent: Intent? = null,
) {
    if (deepLinkIntent != null && startDestination != Onboarding) {
        LaunchedEffect(navController, deepLinkIntent) {
            navController.handleDeepLink(deepLinkIntent)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination = currentDestination.isTopLevel()

    val isFabDestination =
        currentDestination?.hierarchy?.any {
            it.hasRoute(Home::class) || it.hasRoute(Transactions::class)
        } == true

    // The real motion policy of the device: animations follow the system "remove animations"
    // setting and depth falls back to flat with a screen reader or a large font.
    val motionPolicy = rememberSystemMotionPolicy()
    CompositionLocalProvider(LocalMotionPolicy provides motionPolicy) {
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
                    // A pebble (rounded square) in primary with the default FAB shadow, the only shadow of the style.
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(TransactionEdit())
                        },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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
                // Consumed so screens can pad for the keyboard (imePadding) without counting the bars twice.
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            )
        }
    }
}
