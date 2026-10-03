package com.crguarin.wallet.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.WalletAppContent
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    private fun setAppContent(startDestination: Any = Home) {
        composeRule.setContent {
            navController = rememberNavController()
            // Inspection mode renders the screens without Hilt ViewModels.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WalletTheme {
                    WalletAppContent(
                        navController = navController,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "es")
    fun `starts at Onboarding destination when startDestination is Onboarding`() {
        setAppContent(startDestination = Onboarding)

        // Welcome screen content is displayed
        composeRule.onNodeWithText("Bienvenido a Wallet").assertIsDisplayed()
        // Bottom bar is hidden on Onboarding
        composeRule.onNodeWithText("Inicio").assertDoesNotExist()
        composeRule.onNodeWithText("Ajustes").assertDoesNotExist()
        // FAB is hidden on Onboarding
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es")
    fun `starts at Home destination`() {
        setAppContent()

        // Home title is displayed
        composeRule.onAllNodesWithText("Inicio").onFirst().assertIsDisplayed()
        // FAB is visible on Home
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun `touching each tab displays its screen and updates destination`() {
        setAppContent()

        // Click Movimientos tab
        composeRule.onNodeWithText("Movimientos").performClick()
        composeRule.onAllNodesWithText("Movimientos").onFirst().assertIsDisplayed()
        // FAB is visible on Transactions
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertIsDisplayed()

        // Click Cuentas tab
        composeRule.onNodeWithText("Cuentas").performClick()
        composeRule.onAllNodesWithText("Cuentas").onFirst().assertIsDisplayed()
        // FAB is hidden on Accounts
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertDoesNotExist()

        // Click Ajustes tab
        composeRule.onNodeWithText("Ajustes").performClick()
        composeRule.onAllNodesWithText("Ajustes").onFirst().assertIsDisplayed()
        // FAB is hidden on Settings
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertDoesNotExist()

        // Click Inicio tab
        composeRule.onAllNodesWithText("Inicio").onFirst().performClick()
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun `FAB opens TransactionEdit and bottom bar is hidden`() {
        setAppContent()

        // Click FAB
        composeRule.onNodeWithContentDescription("Nuevo movimiento").performClick()

        // Destination is now the new transaction editor
        composeRule.onNodeWithText("Nuevo movimiento").assertIsDisplayed()
        composeRule.onNodeWithText("Gasto").assertIsDisplayed()
        composeRule.onNodeWithText("Ingreso").assertIsDisplayed()
        composeRule.onNodeWithText("Transferencia").assertIsDisplayed()

        // Bottom bar destinations should be hidden
        composeRule.onNodeWithText("Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Cuentas").assertDoesNotExist()
        // FAB should also be hidden
        composeRule.onNodeWithContentDescription("Nuevo movimiento").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es")
    fun `bottom bar is hidden on AccountDetail detail screen`() {
        setAppContent()

        composeRule.runOnUiThread {
            navController.navigate(AccountDetail(accountId = 42L))
        }

        // Detail screen content is displayed
        composeRule.onNodeWithText("Detalle de cuenta").assertIsDisplayed()

        // Bottom bar is hidden
        composeRule.onNodeWithText("Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Cuentas").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es")
    fun `tab texts are in Spanish when locale is Spanish`() {
        setAppContent()

        composeRule.onAllNodesWithText("Inicio").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Movimientos").assertIsDisplayed()
        composeRule.onNodeWithText("Cuentas").assertIsDisplayed()
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `tab texts are in English when locale is English`() {
        setAppContent()

        composeRule.onAllNodesWithText("Home").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeRule.onNodeWithText("Accounts").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `bottom bar has five destinations with reports between transactions and accounts`() {
        setAppContent()

        val transactionsX =
            composeRule
                .onNodeWithContentDescription("Movimientos", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left
        val reportsX =
            composeRule
                .onNodeWithContentDescription("Reportes", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left
        val accountsX =
            composeRule
                .onNodeWithContentDescription("Cuentas", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left

        assertThat(transactionsX).isLessThan(reportsX)
        assertThat(reportsX).isLessThan(accountsX)

        composeRule.onNodeWithText("Reportes").assertIsDisplayed()
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun `navigating from Settings to Categories opens Categories screen`() {
        setAppContent()

        // Click Ajustes tab
        composeRule.onNodeWithText("Ajustes").performClick()
        composeRule.onAllNodesWithText("Ajustes").onFirst().assertIsDisplayed()

        // Click Categorías item in settings (the screen scrolls, so it may start below the fold)
        composeRule.onNodeWithText("Categorías").performScrollTo().performClick()

        // Destination is now Categories screen
        composeRule.onAllNodesWithText("Categorías").onFirst().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-w1000dp-h3000dp")
    fun `view all from Home opens the transactions tab without duplicating it`() {
        setAppContent()

        // Home shows the "Ver todos" link next to recent movements
        composeRule.onNodeWithText("Ver todos").performClick()

        // Transactions tab is displayed
        composeRule.onAllNodesWithText("Movimientos").onFirst().assertIsDisplayed()

        val transactionsEntries =
            navController.currentBackStack.value.count { entry ->
                entry.destination.hasRoute(Transactions::class)
            }
        assertThat(transactionsEntries).isEqualTo(1)

        // Tapping the bottom bar tab again does not add another Transactions entry
        composeRule.onNodeWithContentDescription("Movimientos", useUnmergedTree = true).performClick()

        val afterTappingTab =
            navController.currentBackStack.value.count { entry ->
                entry.destination.hasRoute(Transactions::class)
            }
        assertThat(afterTappingTab).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "es")
    fun `reports tab stays selected on filtered transactions and tapping it pops back to reports`() {
        setAppContent()

        // Go to the Reports tab
        composeRule.onNodeWithText("Reportes").performClick()
        composeRule.onAllNodesWithText("Reportes").onFirst().assertIsDisplayed()

        // Open "Ver movimientos" as the NavHost does
        composeRule.runOnUiThread {
            navController.navigate(
                Transactions(categoryId = 7L, type = TransactionType.EXPENSE, month = "2026-10"),
            )
        }

        // The filtered entry keeps its arguments
        val route = navController.currentBackStackEntry?.toRoute<Transactions>()
        assertThat(route?.categoryId).isEqualTo(7L)
        assertThat(route?.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(route?.month).isEqualTo("2026-10")

        // The owning tab (Reports) stays selected, not Movimientos
        assertTabSelected("Reportes", selected = true)
        assertTabSelected("Movimientos", selected = false)

        // Tapping Reports pops back to the Reports root instead of restoring the filtered list
        composeRule.onNodeWithContentDescription("Reportes", useUnmergedTree = true).performClick()
        composeRule.onAllNodesWithText("Reportes").onFirst().assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.hasRoute(Reports::class)).isTrue()
    }

    @Test
    @Config(qualifiers = "es")
    fun `account transactions opened from home keep home selected and tapping transactions opens the tab`() {
        setAppContent()

        // Home -> account card -> "Ver todos", as the NavHost does
        composeRule.runOnUiThread {
            navController.navigate(AccountDetail(accountId = 42L))
            navController.navigate(Transactions(accountId = 42L))
        }
        composeRule.waitForIdle()

        // The filtered list belongs to Home's stack, not to the Movimientos tab
        assertTabSelected("Inicio", selected = true)
        assertTabSelected("Movimientos", selected = false)

        // Tapping Movimientos opens the unfiltered tab
        composeRule.onNodeWithContentDescription("Movimientos", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        val route = navController.currentBackStackEntry?.toRoute<Transactions>()
        assertThat(route?.accountId).isNull()
        assertTabSelected("Movimientos", selected = true)
    }

    private fun assertTabSelected(
        contentDescription: String,
        selected: Boolean,
    ) {
        var node: SemanticsNode? =
            composeRule
                .onNodeWithContentDescription(contentDescription, useUnmergedTree = true)
                .fetchSemanticsNode()
        while (node != null && !node.config.contains(SemanticsProperties.Selected)) {
            node = node.parent
        }
        assertThat(node?.config?.getOrNull(SemanticsProperties.Selected)).isEqualTo(selected)
    }
}
