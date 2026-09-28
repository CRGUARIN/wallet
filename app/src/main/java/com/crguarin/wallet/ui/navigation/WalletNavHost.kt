package com.crguarin.wallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.crguarin.wallet.ui.accounts.AccountDetailRoute
import com.crguarin.wallet.ui.accounts.AccountEditRoute
import com.crguarin.wallet.ui.accounts.AccountsRoute
import com.crguarin.wallet.ui.categories.CategoriesRoute
import com.crguarin.wallet.ui.categories.CategoryEditRoute
import com.crguarin.wallet.ui.categories.CategoryPickerScreen
import com.crguarin.wallet.ui.home.HomeScreen
import com.crguarin.wallet.ui.onboarding.OnboardingRoute
import com.crguarin.wallet.ui.settings.SettingsScreen
import com.crguarin.wallet.ui.transactions.TransactionEditScreen
import com.crguarin.wallet.ui.transactions.TransactionsScreen

@Composable
fun WalletNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Home,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Onboarding> {
            OnboardingRoute(
                onNavigateToHome = {
                    navController.navigate(Home) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                },
            )
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
            AccountsRoute(
                onAccountClick = { accountId ->
                    navController.navigate(AccountDetail(accountId = accountId))
                },
                onNewAccountClick = {
                    navController.navigate(AccountEdit())
                },
            )
        }
        composable<AccountDetail> {
            AccountDetailRoute(
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { accountId ->
                    navController.navigate(AccountEdit(accountId = accountId))
                },
                onTransferClick = { accountId ->
                    navController.navigate(
                        TransactionEdit(
                            initialType = com.crguarin.wallet.domain.model.TransactionType.TRANSFER,
                            accountId = accountId,
                        ),
                    )
                },
            )
        }
        composable<AccountEdit> {
            AccountEditRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<Categories> {
            CategoriesRoute(
                onNavigateBack = { navController.popBackStack() },
                onCategoryClick = { categoryId, type ->
                    navController.navigate(CategoryEdit(categoryId = categoryId, type = type))
                },
                onNewCategoryClick = { type ->
                    navController.navigate(CategoryEdit(type = type))
                },
                onNewSubcategoryClick = { parentId, type ->
                    navController.navigate(CategoryEdit(parentId = parentId, type = type))
                },
            )
        }
        composable<CategoryEdit> {
            CategoryEditRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<CategoryPicker> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryPicker>()
            CategoryPickerScreen(
                type = route.type,
            )
        }
        composable<Settings> {
            SettingsScreen(
                onCategoriesClick = {
                    navController.navigate(Categories())
                },
            )
        }
    }
}
