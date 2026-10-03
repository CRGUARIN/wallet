package com.crguarin.wallet.ui.reports.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors

/**
 * Geometry of a [CumulativeLineChart], cached with `drawWithCache` so the paths are not rebuilt on
 * every frame.
 */
private class LineChartGeometry(
    val plotLeft: Float,
    val plotTop: Float,
    val plotRight: Float,
    val plotBottom: Float,
    val daysInMonth: Int,
    val currentPoints: List<Offset>,
    val previousPoints: List<Offset>,
    val currentPath: Path?,
    val fillPath: Path?,
    val previousPath: Path?,
) {
    val plotWidth: Float = plotRight - plotLeft
    val plotHeight: Float = plotBottom - plotTop

    /** Horizontal position of a 1-based day of the month. */
    fun xFor(day: Int): Float {
        val span = (daysInMonth - 1).coerceAtLeast(1)
        return plotLeft + (day - 1).coerceIn(0, span).toFloat() / span * plotWidth
    }
}

/**
 * Stateless cumulative line chart: the current month is a solid line with a soft gradient fill and
 * the previous month is a dashed line without fill. Both series must already share the same
 * normalization (fractions `0..1` of the maximum of both, computed by the UI mapper), so the gap
 * between them is comparable.
 *
 * The x axis uses [xLabels] (day number plus its already localized text, for example the 1st, the
 * 15th and the last day) and the y axis uses [yLabels] with compact money labels. Labels are drawn
 * with [drawText] and the `labelSmall` style, so they scale with the system font size.
 *
 * @param daysInMonth number of days of the represented month; the x axis spans `1..daysInMonth`.
 * @param contentDescription accessibility summary resolved by the caller with `stringResource`.
 */
@Composable
fun CumulativeLineChart(
    current: List<Float>,
    previous: List<Float>,
    daysInMonth: Int,
    currentColor: Color,
    xLabels: List<Pair<Int, String>>,
    yLabels: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val yLayouts =
        remember(yLabels, labelStyle, textMeasurer) {
            yLabels.map { textMeasurer.measure(it, labelStyle) }
        }
    val xLayouts =
        remember(xLabels, labelStyle, textMeasurer) {
            xLabels.map { textMeasurer.measure(it.second, labelStyle) }
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

    Canvas(
        modifier =
            modifier
                .semantics { this.contentDescription = description }
                .drawWithCache {
                    val chart =
                        buildLineChartGeometry(
                            size = size,
                            daysInMonth = daysInMonth,
                            current = current,
                            previous = previous,
                            yAxisWidth = yAxisWidth,
                            xAxisHeight = xAxisHeight,
                        )
                    onDrawBehind {
                        for (index in yLayouts.indices) {
                            val layout = yLayouts[index]
                            val fraction = if (yLayouts.size <= 1) 0f else index / (yLayouts.size - 1f)
                            val y = chart.plotBottom - fraction * chart.plotHeight
                            drawLine(
                                color = guideColor,
                                start = Offset(chart.plotLeft, y),
                                end = Offset(chart.plotRight, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawText(
                                textLayoutResult = layout,
                                color = labelColor,
                                topLeft =
                                    Offset(
                                        x = chart.plotLeft - 6.dp.toPx() - layout.size.width,
                                        y = (y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height),
                                    ),
                            )
                        }

                        chart.fillPath?.let { path ->
                            drawPath(
                                path = path,
                                brush =
                                    Brush.verticalGradient(
                                        colors = listOf(currentColor.copy(alpha = 0.15f), Color.Transparent),
                                        startY = chart.plotTop,
                                        endY = chart.plotBottom,
                                    ),
                            )
                        }
                        chart.previousPath?.let { path ->
                            drawPath(
                                path = path,
                                color = labelColor,
                                style =
                                    Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                                    ),
                            )
                        }
                        chart.currentPath?.let { path ->
                            drawPath(
                                path = path,
                                color = currentColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                        chart.currentPoints.lastOrNull()?.let { last ->
                            drawCircle(color = currentColor, radius = 3.5.dp.toPx(), center = last)
                        }

                        for (index in xLayouts.indices) {
                            val layout = xLayouts[index]
                            val day = xLabels[index].first.coerceIn(1, daysInMonth.coerceAtLeast(1))
                            val x = chart.xFor(day)
                            val left = (x - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                            drawText(
                                textLayoutResult = layout,
                                color = labelColor,
                                topLeft = Offset(left, chart.plotBottom + 4.dp.toPx()),
                            )
                        }
                    }
                },
    ) {}
}

private fun Density.buildLineChartGeometry(
    size: Size,
    daysInMonth: Int,
    current: List<Float>,
    previous: List<Float>,
    yAxisWidth: Float,
    xAxisHeight: Float,
): LineChartGeometry {
    val plotLeft = yAxisWidth
    val plotRight = size.width
    val plotTop = 4.dp.toPx()
    val plotBottom = (size.height - xAxisHeight).coerceAtLeast(plotTop + 1.dp.toPx())
    val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
    val plotHeight = plotBottom - plotTop
    val span = (daysInMonth - 1).coerceAtLeast(1)

    fun xFor(day: Int): Float = plotLeft + (day - 1).coerceIn(0, span).toFloat() / span * plotWidth

    fun yFor(fraction: Float): Float = plotBottom - fraction.coerceIn(0f, 1f) * plotHeight

    val currentPoints = current.mapIndexed { index, fraction -> Offset(xFor(index + 1), yFor(fraction)) }
    val previousPoints =
        previous.mapIndexedNotNull { index, fraction ->
            if (index < daysInMonth) Offset(xFor(index + 1), yFor(fraction)) else null
        }
    val currentPath = currentPoints.toPolyline()
    val fillPath =
        if (currentPoints.isEmpty()) {
            null
        } else {
            Path().apply {
                moveTo(currentPoints.first().x, currentPoints.first().y)
                for (index in 1 until currentPoints.size) {
                    lineTo(currentPoints[index].x, currentPoints[index].y)
                }
                lineTo(currentPoints.last().x, plotBottom)
                lineTo(currentPoints.first().x, plotBottom)
                close()
            }
        }
    val previousPath = previousPoints.toPolyline()
    return LineChartGeometry(
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = plotRight,
        plotBottom = plotBottom,
        daysInMonth = daysInMonth,
        currentPoints = currentPoints,
        previousPoints = previousPoints,
        currentPath = currentPath,
        fillPath = fillPath,
        previousPath = previousPath,
    )
}

private fun List<Offset>.toPolyline(): Path? {
    if (isEmpty()) return null
    val path = Path()
    path.moveTo(first().x, first().y)
    for (index in 1 until size) {
        path.lineTo(this[index].x, this[index].y)
    }
    return path
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CumulativeLineChartLightPreview() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        CumulativeLineChartPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun CumulativeLineChartDarkPreview() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        CumulativeLineChartPreviewContent()
    }
}

@Composable
private fun CumulativeLineChartPreviewContent() {
    val expenseColor = MaterialTheme.walletSemanticColors.expense
    CumulativeLineChart(
        current = List(31) { index -> (index + 1) * 0.022f },
        previous = List(30) { index -> (index + 1) * 0.025f },
        daysInMonth = 31,
        currentColor = expenseColor,
        xLabels = listOf(1 to "1", 15 to "15", 31 to "31"),
        yLabels = listOf("$ 0", "$ 500 mil", "$ 1 M", "$ 1,5 M"),
        contentDescription = "Gráfico de líneas",
        modifier = Modifier.fillMaxWidth().height(180.dp),
    )
}
