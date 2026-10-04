package com.crguarin.wallet.ui.cards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class CardReconcileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val closingDate = LocalDate.of(2026, 9, 15)

    private fun formState() =
        CardReconcileUiState(
            isLoading = false,
            accountId = 1L,
            accountName = "RappiCard",
            closingDate = closingDate,
            dueDate = LocalDate.of(2026, 10, 5),
            statementBalanceText = "11000",
        )

    private fun previewState(
        difference: Money,
        interestMismatch: Boolean = false,
    ) = formState().copy(
        preview =
            ReconciliationPreview(
                accountId = 1L,
                closingDate = closingDate,
                appDebt = Money(1_000_000L),
                statementBalance = Money(1_000_000L) + difference,
                difference = difference,
                interestMismatch = interestMismatch,
            ),
    )

    private fun setScreen(
        uiState: CardReconcileUiState,
        onCalculateClick: () -> Unit = {},
        onSaveConfirmed: () -> Unit = {},
        onForceAdjustConfirmed: () -> Unit = {},
        onReviewTransactionsClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                CardReconcileScreen(
                    uiState = uiState,
                    onClosingDateSelected = {},
                    onDueDateSelected = {},
                    onOpenClosingDatePicker = {},
                    onDismissClosingDatePicker = {},
                    onOpenDueDatePicker = {},
                    onDismissDueDatePicker = {},
                    onStatementBalanceChanged = {},
                    onMinimumPaymentChanged = {},
                    onInterestChanged = {},
                    onCalculateClick = onCalculateClick,
                    onSaveConfirmed = onSaveConfirmed,
                    onForceAdjustConfirmed = onForceAdjustConfirmed,
                    onReviewTransactionsClick = onReviewTransactionsClick,
                    onNavigateBack = {},
                )
            }
        }
    }

    @Test
    fun `form shows the card title intro and calculate button`() {
        var calculateClicked = false
        setScreen(formState(), onCalculateClick = { calculateClicked = true })

        composeRule.onNodeWithText("RappiCard").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha de corte").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha límite de pago").assertIsDisplayed()
        composeRule.onNodeWithText("Saldo total del extracto").assertIsDisplayed()
        composeRule.onNodeWithText("Calcular diferencia").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Calcular diferencia").performClick()

        assertThat(calculateClicked).isTrue()
    }

    @Test
    fun `calculate is disabled without a valid balance`() {
        setScreen(formState().copy(statementBalanceText = ""))

        composeRule.onNodeWithText("Calcular diferencia").assertIsNotEnabled()
    }

    @Test
    fun `matching preview shows the match message and saves`() {
        var saved = false
        setScreen(previewState(Money.ZERO), onSaveConfirmed = { saved = true })

        composeRule.onNodeWithText("¡Cuadra con el banco!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Deuda según la app al corte").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Guardar conciliación").performScrollTo().performClick()

        assertThat(saved).isTrue()
    }

    @Test
    fun `positive preview explains the interest adjustment`() {
        var saved = false
        setScreen(previewState(Money(100_000L)), onSaveConfirmed = { saved = true })

        composeRule.onNodeWithText("Diferencia").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("intereses y comisiones", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Guardar conciliación").performScrollTo().performClick()

        assertThat(saved).isTrue()
    }

    @Test
    fun `interest mismatch shows the warning`() {
        setScreen(
            previewState(Money(100_000L), interestMismatch = true).copy(interestText = "500"),
        )

        composeRule
            .onNodeWithText("puede faltar algún movimiento", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `negative preview offers review force adjust and plain save`() {
        var reviewed = false
        var forced = false
        var saved = false
        setScreen(
            previewState(Money(-100_000L)),
            onSaveConfirmed = { saved = true },
            onForceAdjustConfirmed = { forced = true },
            onReviewTransactionsClick = { reviewed = true },
        )

        composeRule
            .onNodeWithText("falta anotar un pago", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Revisar movimientos del ciclo")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertThat(reviewed).isTrue()

        composeRule.onNodeWithText("Ajustar de todos modos").performScrollTo().performClick()
        assertThat(forced).isTrue()

        composeRule.onNodeWithText("Guardar conciliación").performScrollTo().performClick()
        assertThat(saved).isTrue()
    }
}
