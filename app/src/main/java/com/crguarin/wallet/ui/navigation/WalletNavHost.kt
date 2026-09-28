package com.crguarin.wallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.crguarin.wallet.ui.accounts.AccountDetailScreen
import com.crguarin.wallet.ui.accounts.AccountEditScreen
import com.crguarin.wallet.ui.accounts.AccountsScreen
import com.crguarin.wallet.ui.categories.CategoriesScreen
import com.crguarin.wallet.ui.categories.CategoryEditScreen
import com.crguarin.wallet.ui.categories.CategoryPickerScreen
import com.crguarin.wallet.ui.home.HomeScreen
import com.crguarin.wallet.ui.onboarding.OnboardingScreen
import com.crguarin.wallet.ui.settings.SettingsScreen
import com.crguarin.wallet.ui.transactions.TransactionEditScreen
import com.crguarin.wallet.ui.transactions.TransactionsScreen

@Composable
fun WalletNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Onboarding> {
            OnboardingScreen()
        }
        composable<Home> {
            HomeScreen()
        }
        composable<Transactions> { backStackEntry ->
            val route = backStackEntry.toRoute<Transactions>()
            TransactionsScreen(
                accountId = route.accountId,
                categoryId = route.categoryId,
            )
        }
        composable<TransactionEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<TransactionEdit>()
            TransactionEditScreen(
                transactionId = route.transactionId,
                initialType = route.initialType,
                accountId = route.accountId,
            )
        }
        composable<Accounts> {
            AccountsScreen()
        }
        composable<AccountDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountDetail>()
            AccountDetailScreen(
                accountId = route.accountId,
            )
        }
        composable<AccountEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountEdit>()
            AccountEditScreen(
                accountId = route.accountId,
            )
        }
        composable<Categories> { backStackEntry ->
            val route = backStackEntry.toRoute<Categories>()
            CategoriesScreen(
                type = route.type,
            )
        }
        composable<CategoryEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryEdit>()
            CategoryEditScreen(
                categoryId = route.categoryId,
                parentId = route.parentId,
                type = route.type,
            )
        }
        composable<CategoryPicker> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryPicker>()
            CategoryPickerScreen(
                type = route.type,
            )
        }
        composable<Settings> {
            SettingsScreen()
        }
    }
}
