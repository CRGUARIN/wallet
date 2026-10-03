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
import com.crguarin.wallet.ui.theme.walletSemanticColors
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GroupedBarChartTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setChart(
        onGroupClick: (Int) -> Unit = {},
        lineValues: List<Float?>? = null,
    ) {
        composeRule.setContent {
            WalletTheme {
                val semanticColors = MaterialTheme.walletSemanticColors
                GroupedBarChart(
                    groups =
                        listOf(
                            BarGroup("E", listOf(0.7f, 0.2f), listOf(semanticColors.income, semanticColors.expense)),
                            BarGroup("F", listOf(0.4f, 0.6f), listOf(semanticColors.income, semanticColors.expense)),
                            BarGroup("M", listOf(0.5f, 0.5f), listOf(semanticColors.income, semanticColors.expense)),
                            BarGroup("A", listOf(0.3f, 0.1f), listOf(semanticColors.income, semanticColors.expense)),
                        ),
                    selectedIndex = null,
                    averageFraction = 0.4f,
                    yLabels = listOf("0", "1", "2"),
                    onGroupClick = onGroupClick,
                    contentDescription = "bars",
                    baselineFraction = 0.25f,
                    lineValues = lineValues,
                    lineColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(400.dp).height(220.dp),
                )
            }
        }
    }

    @Test
    fun `renders with baseline and net line`() {
        setChart(lineValues = listOf(0.5f, 0.3f, 0.5f, null))

        composeRule.onNodeWithContentDescription("bars").assertExists()
    }

    @Test
    fun `tap selects the tapped group`() {
        var clicked: Int? = null
        setChart(onGroupClick = { clicked = it }, lineValues = listOf(0.5f, 0.3f, 0.5f, null))

        val node = composeRule.onNodeWithContentDescription("bars")
        val size = node.fetchSemanticsNode().size
        node.performTouchInput { click(Offset(size.width * 0.4f, size.height * 0.5f)) }

        assertThat(clicked).isEqualTo(1)
    }

    @Test
    fun `renders without overlay line`() {
        setChart(lineValues = null)

        composeRule.onNodeWithContentDescription("bars").assertExists()
    }
}
