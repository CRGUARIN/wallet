package com.crguarin.wallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.crguarin.wallet.ui.accounts.AccountDetailRoute
import com.crguarin.wallet.ui.accounts.AccountEditRoute
import com.crguarin.wallet.ui.accounts.AccountsRoute
import com.crguarin.wallet.ui.categories.CategoriesRoute
import com.crguarin.wallet.ui.categories.CategoryEditRoute
import com.crguarin.wallet.ui.home.HomeRoute
import com.crguarin.wallet.ui.onboarding.OnboardingRoute
import com.crguarin.wallet.ui.settings.SettingsRoute
import com.crguarin.wallet.ui.transactions.TransactionEditRoute
import com.crguarin.wallet.ui.transactions.TransactionsRoute

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
        enterTransition = { walletEnterTransition() },
        exitTransition = { walletExitTransition() },
        popEnterTransition = { walletPopEnterTransition() },
        popExitTransition = { walletPopExitTransition() },
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
            HomeRoute(
                onAccountClick = { accountId ->
                    navController.navigate(AccountDetail(accountId = accountId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionEdit(transactionId = transactionId))
                },
                onViewAllTransactions = {
                    navController.navigateToTopLevelDestination(Transactions())
                },
                onQuickAction = { type ->
                    navController.navigate(TransactionEdit(initialType = type))
                },
            )
        }
        composable<Transactions> {
            TransactionsRoute(
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionEdit(transactionId = transactionId))
                },
            )
        }
        composable<TransactionEdit> {
            TransactionEditRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategoryEdit = { type ->
                    navController.navigate(CategoryEdit(type = type))
                },
                onNavigateToAccountEdit = {
                    navController.navigate(AccountEdit())
                },
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
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionEdit(transactionId = transactionId))
                },
                onViewAllTransactions = { accountId ->
                    navController.navigate(Transactions(accountId = accountId))
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
                onCategorySaved = { savedCategoryId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NEW_CATEGORY_RESULT_KEY, savedCategoryId)
                    navController.popBackStack()
                },
            )
        }
        composable<Settings> {
            SettingsRoute(
                onCategoriesClick = {
                    navController.navigate(Categories())
                },
            )
        }
    }
}
