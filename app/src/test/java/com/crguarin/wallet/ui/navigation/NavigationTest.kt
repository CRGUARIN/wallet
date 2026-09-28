package com.crguarin.wallet.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.crguarin.wallet.ui.WalletAppContent
import com.crguarin.wallet.ui.theme.WalletTheme
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
            WalletTheme {
                WalletAppContent(
                    navController = navController,
                    startDestination = startDestination,
                )
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

        // Destination is now TransactionEdit
        composeRule.onAllNodesWithText("Movimiento").onFirst().assertIsDisplayed()

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
}
