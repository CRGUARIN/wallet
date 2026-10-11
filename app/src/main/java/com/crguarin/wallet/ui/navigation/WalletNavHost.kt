package com.crguarin.wallet.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.crguarin.wallet.ui.accounts.AccountDetailRoute
import com.crguarin.wallet.ui.accounts.AccountEditRoute
import com.crguarin.wallet.ui.accounts.AccountsRoute
import com.crguarin.wallet.ui.cards.CardReconcileRoute
import com.crguarin.wallet.ui.categories.CategoriesRoute
import com.crguarin.wallet.ui.categories.CategoryEditRoute
import com.crguarin.wallet.ui.debts.DebtDetailRoute
import com.crguarin.wallet.ui.debts.DebtEditRoute
import com.crguarin.wallet.ui.debts.DebtListRoute
import com.crguarin.wallet.ui.home.HomeRoute
import com.crguarin.wallet.ui.motion.LocalNavAnimatedVisibilityScope
import com.crguarin.wallet.ui.motion.LocalSharedTransitionScope
import com.crguarin.wallet.ui.onboarding.OnboardingRoute
import com.crguarin.wallet.ui.recurring.RecurringEditRoute
import com.crguarin.wallet.ui.recurring.RecurringListRoute
import com.crguarin.wallet.ui.reports.ReportsRoute
import com.crguarin.wallet.ui.settings.SettingsRoute
import com.crguarin.wallet.ui.transactions.TransactionEditRoute
import com.crguarin.wallet.ui.transactions.TransactionsRoute

@Composable
fun WalletNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Home,
) {
    // Notification deep links are only registered once onboarding is complete. The NavController
    // handles the hosting Activity's intent automatically when the graph is created
    // (checkDeepLinkHandled), so registering them during onboarding would skip it.
    val deepLinksEnabled = startDestination != Onboarding
    // The shared layout lets an account card fly from the Home deck to the detail header.
    // Only those two destinations publish their AnimatedVisibilityScope; the rest is untouched.
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = { walletEnterTransition() },
                exitTransition = { walletExitTransition() },
                popEnterTransition = { walletPopEnterTransition() },
                popExitTransition = { walletPopExitTransition() },
                // The system back button (and gesture) goes through predictive back, which otherwise uses
                // the library's near-instant fade instead of the pop transitions above.
                predictivePopEnterTransition = { walletPopEnterTransition() },
                predictivePopExitTransition = { walletPopExitTransition() },
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
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
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
                            onViewReports = {
                                navController.navigateToTopLevelDestination(Reports)
                            },
                            onUpcomingRecurringClick = { paymentId ->
                                navController.navigate(RecurringEdit(paymentId = paymentId))
                            },
                            onUpcomingPendingClick = {
                                navController.navigate(RecurringList)
                            },
                            onUpcomingCardClick = { accountId ->
                                navController.navigate(AccountDetail(accountId = accountId))
                            },
                            onViewAllRecurring = {
                                navController.navigate(RecurringList)
                            },
                            onViewDebts = {
                                navController.navigate(DebtList)
                            },
                            onOpenSettings = {
                                navController.navigate(Settings)
                            },
                        )
                    }
                }
                composable<Transactions> {
                    TransactionsRoute(
                        onTransactionClick = { transactionId ->
                            navController.navigate(TransactionEdit(transactionId = transactionId))
                        },
                    )
                }
                composable<Reports> {
                    ReportsRoute(
                        onViewTransactions = { categoryId, accountId, type, yearMonth ->
                            navController.navigate(
                                Transactions(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    type = type,
                                    month = yearMonth.toString(),
                                ),
                            )
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
                        onOpenCategories = {
                            navController.navigate(Categories())
                        },
                        onOpenRecurring = {
                            navController.navigate(RecurringList)
                        },
                        onOpenDebts = {
                            navController.navigate(DebtList)
                        },
                    )
                }
                composable<AccountDetail>(
                    deepLinks =
                        if (deepLinksEnabled) {
                            listOf(navDeepLink<AccountDetail>(basePath = "wallet://account"))
                        } else {
                            emptyList()
                        },
                ) {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
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
                            onNewTransactionClick = { accountId, type ->
                                navController.navigate(TransactionEdit(initialType = type, accountId = accountId))
                            },
                            onTransactionClick = { transactionId ->
                                navController.navigate(TransactionEdit(transactionId = transactionId))
                            },
                            onViewAllTransactions = { accountId ->
                                navController.navigate(Transactions(accountId = accountId))
                            },
                            onRechargeClick = { fromAccountId, toAccountId ->
                                navController.navigate(
                                    TransactionEdit(
                                        initialType = com.crguarin.wallet.domain.model.TransactionType.TRANSFER,
                                        accountId = fromAccountId,
                                        toAccountId = toAccountId,
                                    ),
                                )
                            },
                            onPayCardClick = { fromAccountId, toAccountId, initialAmountMinor ->
                                navController.navigate(
                                    TransactionEdit(
                                        initialType = com.crguarin.wallet.domain.model.TransactionType.TRANSFER,
                                        accountId = fromAccountId,
                                        toAccountId = toAccountId,
                                        initialAmountMinor = initialAmountMinor,
                                    ),
                                )
                            },
                            onReconcileClick = { accountId ->
                                navController.navigate(CardReconcile(accountId = accountId))
                            },
                        )
                    }
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
                        onNavigateBack = { navController.popBackStack() },
                        onCategoriesClick = {
                            navController.navigate(Categories())
                        },
                        onRecurringClick = {
                            navController.navigate(RecurringList)
                        },
                        onDebtsClick = {
                            navController.navigate(DebtList)
                        },
                    )
                }
                composable<CardReconcile> {
                    CardReconcileRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onReviewTransactions = { accountId, month ->
                            navController.navigate(Transactions(accountId = accountId, month = month))
                        },
                    )
                }
                composable<RecurringList>(
                    deepLinks =
                        if (deepLinksEnabled) {
                            listOf(navDeepLink<RecurringList>(basePath = "wallet://recurring"))
                        } else {
                            emptyList()
                        },
                ) {
                    RecurringListRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onAddClick = { navController.navigate(RecurringEdit()) },
                        onPaymentClick = { paymentId ->
                            navController.navigate(RecurringEdit(paymentId = paymentId))
                        },
                    )
                }
                composable<RecurringEdit> {
                    RecurringEditRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onTransactionClick = { transactionId ->
                            navController.navigate(TransactionEdit(transactionId = transactionId))
                        },
                    )
                }
                composable<DebtList> {
                    DebtListRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onAddClick = { navController.navigate(DebtEdit()) },
                        onDebtClick = { debtId ->
                            navController.navigate(DebtDetail(debtId = debtId))
                        },
                    )
                }
                composable<DebtDetail> {
                    DebtDetailRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onEditClick = { debtId ->
                            navController.navigate(DebtEdit(debtId = debtId))
                        },
                    )
                }
                composable<DebtEdit> {
                    DebtEditRoute(
                        onNavigateBack = { navController.popBackStack() },
                        // Deleting from the editor opened by a detail screen leaves the detail, which no longer exists.
                        onDeleted = {
                            navController.navigate(DebtList) {
                                popUpTo(DebtList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    }
}
