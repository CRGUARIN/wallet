package com.crguarin.wallet.ui.reports.charts

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BalanceLineChartTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setChart(
        values: List<Float>,
        slotCount: Int = 11,
        selectedIndex: Int? = null,
        onPointSelected: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            WalletTheme {
                BalanceLineChart(
                    values = values,
                    slotCount = slotCount,
                    zeroFraction = 0.25f,
                    xLabels = listOf(0 to "a", 10 to "b"),
                    yLabels = listOf("0", "1"),
                    lineColor = MaterialTheme.colorScheme.primary,
                    selectedIndex = selectedIndex,
                    onPointSelected = onPointSelected,
                    contentDescription = "balance",
                    modifier = Modifier.width(400.dp).height(180.dp),
                )
            }
        }
    }

    @Test
    fun `empty values render without crashing`() {
        setChart(values = emptyList())

        composeRule.onNodeWithContentDescription("balance").assertExists()
    }

    @Test
    fun `tap on the far right selects the last point`() {
        var selected: Int? = null
        setChart(values = List(11) { 0.1f + it * 0.05f }, onPointSelected = { selected = it })

        val node = composeRule.onNodeWithContentDescription("balance")
        val size = node.fetchSemanticsNode().size
        node.performTouchInput { click(Offset(size.width * 0.98f, size.height * 0.5f)) }

        assertThat(selected).isEqualTo(10)
    }

    @Test
    fun `tap on the labels selects nothing`() {
        var selected: Int? = null
        setChart(values = List(11) { 0.5f }, onPointSelected = { selected = it })

        composeRule
            .onNodeWithContentDescription("balance")
            .performTouchInput { click(Offset(2f, 90f)) }

        assertThat(selected).isNull()
    }

    @Test
    fun `single point in a single slot is selectable`() {
        var selected: Int? = null
        setChart(values = listOf(0.5f), slotCount = 1, onPointSelected = { selected = it })

        val node = composeRule.onNodeWithContentDescription("balance")
        val size = node.fetchSemanticsNode().size
        node.performTouchInput { click(Offset(size.width * 0.5f, size.height * 0.5f)) }

        assertThat(selected).isEqualTo(0)
    }

    @Test
    fun `flat line renders with selection`() {
        setChart(values = List(11) { 0.5f }, selectedIndex = 10)

        composeRule.onNodeWithContentDescription("balance").assertExists()
    }

    @Test
    fun `line on zero renders with selection`() {
        setChart(values = List(11) { 0.25f }, selectedIndex = 0)

        composeRule.onNodeWithContentDescription("balance").assertExists()
    }
}
