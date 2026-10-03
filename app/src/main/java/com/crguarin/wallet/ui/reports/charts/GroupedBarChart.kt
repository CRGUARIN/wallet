package com.crguarin.wallet.ui.reports.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors

/**
 * One column of a [GroupedBarChart]: a label plus one bar per series (for example income and
 * expense), with colors resolved by the caller.
 *
 * @param values bar heights as fractions of the axis maximum, `0..1`.
 * @param dimmed true for groups that are not meaningful yet (for example future months).
 */
@Immutable
data class BarGroup(
    val label: String,
    val values: List<Float>,
    val colors: List<Color>,
    val dimmed: Boolean = false,
)

/**
 * Stateless grouped bar chart: bars grow from the base with rounded top corners, horizontal guide
 * lines come from [yLabels] and the whole column of each group is tappable.
 *
 * The axis labels are drawn with [drawText] and the `labelSmall` style, so they scale with the
 * system font size. Colors are never hardcoded: bars use the colors resolved by the caller and the
 * guides/average line use [androidx.compose.material3.ColorScheme].
 *
 * @param selectedIndex group drawn at full opacity when set; the rest are dimmed to 0.5.
 * @param averageFraction optional dashed line (0..1 of the axis maximum), for example the average
 *   expense of the drawn months.
 * @param contentDescription accessibility summary resolved by the caller with `stringResource`.
 */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    selectedIndex: Int?,
    averageFraction: Float?,
    yLabels: List<String>,
    onGroupClick: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val fallbackColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()
    val yLayouts =
        remember(yLabels, labelStyle, textMeasurer) {
            yLabels.map { textMeasurer.measure(it, labelStyle) }
        }
    val xLayouts =
        remember(groups, labelStyle, textMeasurer) {
            groups.map { textMeasurer.measure(it.label, labelStyle) }
        }
    val density = LocalDensity.current
    val yAxisWidth =
        with(density) {
            ((yLayouts.maxOfOrNull { it.size.width } ?: 0) + 6.dp.roundToPx()).toFloat()
        }
    val xAxisHeight =
        with(density) {
            ((xLayouts.maxOfOrNull { it.size.height } ?: 0) + 4.dp.roundToPx()).toFloat()
        }
    val description = contentDescription
    val currentOnGroupClick by rememberUpdatedState(onGroupClick)

    val growth = remember { Animatable(0f) }
    LaunchedEffect(groups) {
        growth.snapTo(0f)
        growth.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }
    val growthValue = growth.value

    Canvas(
        modifier =
            modifier
                .semantics { this.contentDescription = description }
                .pointerInput(groups.size, yAxisWidth) {
                    detectTapGestures { offset ->
                        if (groups.isEmpty() || yAxisWidth >= size.width || offset.x < yAxisWidth) {
                            return@detectTapGestures
                        }
                        val columnWidth = (size.width - yAxisWidth) / groups.size
                        val index = ((offset.x - yAxisWidth) / columnWidth).toInt()
                        if (index in groups.indices) currentOnGroupClick(index)
                    }
                },
    ) {
        if (groups.isEmpty()) return@Canvas

        val plotLeft = yAxisWidth
        val plotRight = size.width
        val plotTop = 4.dp.toPx()
        val plotBottom = size.height - xAxisHeight
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

        yLayouts.forEachIndexed { index, layout ->
            val fraction = if (yLayouts.size <= 1) 0f else index / (yLayouts.size - 1f)
            val y = plotBottom - fraction * plotHeight
            drawLine(
                color = guideColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawText(
                textLayoutResult = layout,
                color = labelColor,
                topLeft =
                    Offset(
                        x = plotLeft - 6.dp.toPx() - layout.size.width,
                        y = (y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height),
                    ),
            )
        }

        val columnWidth = plotWidth / groups.size
        val innerPadding = 4.dp.toPx()
        val barGap = 2.dp.toPx()
        val radius = 4.dp.toPx()
        for (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            if (group.values.isEmpty()) continue
            val dimmed = group.dimmed || (selectedIndex != null && selectedIndex != groupIndex)
            val alpha = if (dimmed) 0.5f else 1f
            val usable = (columnWidth - innerPadding * 2f).coerceAtLeast(1f)
            val barWidth = ((usable - barGap * (group.values.size - 1)) / group.values.size).coerceAtLeast(1f)
            for (valueIndex in group.values.indices) {
                val fraction = group.values[valueIndex].coerceIn(0f, 1f)
                val barHeight = fraction * plotHeight * growthValue
                if (barHeight <= 0f) continue
                val left = plotLeft + groupIndex * columnWidth + innerPadding + valueIndex * (barWidth + barGap)
                val top = plotBottom - barHeight
                val color = group.colors.getOrElse(valueIndex) { fallbackColor }
                val corner = radius.coerceAtMost(barWidth / 2f).coerceAtMost(barHeight)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(corner, corner),
                    alpha = alpha,
                )
                if (barHeight > corner) {
                    // Cover the bottom corners so only the top ones stay rounded.
                    drawRect(
                        color = color,
                        topLeft = Offset(left, top + barHeight - corner),
                        size = Size(barWidth, corner),
                        alpha = alpha,
                    )
                }
            }
        }

        if (averageFraction != null) {
            val y = plotBottom - averageFraction.coerceIn(0f, 1f) * plotHeight
            drawLine(
                color = labelColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }

        xLayouts.forEachIndexed { index, layout ->
            val columnCenter = plotLeft + index * columnWidth + columnWidth / 2f
            val left = (columnCenter - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
            drawText(
                textLayoutResult = layout,
                color = labelColor,
                topLeft = Offset(left, plotBottom + 4.dp.toPx()),
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun GroupedBarChartLightPreview() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        GroupedBarChartPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun GroupedBarChartDarkPreview() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        GroupedBarChartPreviewContent()
    }
}

@Composable
private fun GroupedBarChartPreviewContent() {
    val income = MaterialTheme.walletSemanticColors.income
    val expense = MaterialTheme.walletSemanticColors.expense
    GroupedBarChart(
        groups =
            listOf(
                BarGroup("may", listOf(0.62f, 0.78f), listOf(income, expense)),
                BarGroup("jun", listOf(0.55f, 0.42f), listOf(income, expense)),
                BarGroup("jul", listOf(0.70f, 0.58f), listOf(income, expense)),
                BarGroup("ago", listOf(0.48f, 0.85f), listOf(income, expense)),
                BarGroup("sep", listOf(0.66f, 0.50f), listOf(income, expense)),
                BarGroup("oct", listOf(0.40f, 0.22f), listOf(income, expense), dimmed = true),
            ),
        selectedIndex = 3,
        averageFraction = 0.55f,
        yLabels = listOf("$ 0", "$ 1,5 M", "$ 3 M", "$ 4,5 M", "$ 6 M"),
        onGroupClick = {},
        contentDescription = "Gráfico de barras",
        modifier = Modifier.fillMaxWidth().height(220.dp),
    )
}
