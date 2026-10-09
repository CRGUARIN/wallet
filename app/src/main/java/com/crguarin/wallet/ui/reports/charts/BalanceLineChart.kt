package com.crguarin.wallet.ui.reports.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.reports.compactMoney
import com.crguarin.wallet.ui.reports.monthNarrowLetter
import com.crguarin.wallet.ui.theme.WalletTheme
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Index of the point nearest to [x] (pixels from the plot left edge, [width] wide), or null when
 * there is nothing to tap ([count] or [slotCount] not positive, or [width] not positive).
 *
 * Points sit on slots `0..count - 1` of [slotCount] (`index / (slotCount - 1)`); taps past the
 * last point resolve to it, and a single slot always resolves to index 0.
 */
internal fun nearestIndex(
    x: Float,
    width: Float,
    slotCount: Int,
    count: Int,
): Int? {
    if (count <= 0 || slotCount <= 0 || width <= 0f) return null
    val last = minOf(count, slotCount) - 1
    if (last <= 0) return 0
    return (x / width * (slotCount - 1)).roundToInt().coerceIn(0, last)
}

/**
 * Geometry of a [BalanceLineChart], cached with `drawWithCache` so the paths are not rebuilt on
 * every frame.
 */
private class BalanceChartGeometry(
    val plotLeft: Float,
    val plotTop: Float,
    val plotRight: Float,
    val plotBottom: Float,
    val slotCount: Int,
    val points: List<Offset>,
    val linePath: Path?,
    val fillPath: Path?,
    val fillBaseY: Float,
) {
    val plotWidth: Float = plotRight - plotLeft
    val plotHeight: Float = plotBottom - plotTop

    /** Horizontal position of a 0-based slot; a single slot is centered. */
    fun xFor(index: Int): Float {
        if (slotCount <= 1) return (plotLeft + plotRight) / 2f
        val span = slotCount - 1
        return plotLeft + index.coerceIn(0, span).toFloat() / span * plotWidth
    }
}

/**
 * Stateless balance line chart: one point per day (month mode) or per month end (year mode), with
 * a soft gradient fill down (or up) to the zero line and a stronger zero line when the range
 * crosses zero. Tapping the plot selects the nearest point through [onPointSelected].
 *
 * It shares the look and margins of [CumulativeLineChart] (y labels on the left, guides and x
 * labels drawn with [drawText] and the `labelSmall` style). An empty [values] draws nothing.
 *
 * @param values y positions 0..1, one per point, in order.
 * @param slotCount total x slots (days in month or 12); values.size <= slotCount.
 * @param zeroFraction y of the zero line when the range crosses zero, else null.
 * @param xLabels (slot index, label) pairs for the x axis.
 * @param yLabels y axis labels, bottom to top, evenly spaced.
 * @param selectedIndex selected point, drawn as a circle with a `surfaceContainerLow` ring; null = none.
 * @param contentDescription accessibility summary resolved by the caller with `stringResource`.
 */
@Composable
fun BalanceLineChart(
    values: List<Float>,
    slotCount: Int,
    zeroFraction: Float?,
    xLabels: List<Pair<Int, String>>,
    yLabels: List<String>,
    lineColor: Color,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val zeroLineColor = MaterialTheme.colorScheme.outline
    // The chart sits on a section card, so the ring around the selected point matches that layer.
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
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
    val currentOnPointSelected by rememberUpdatedState(onPointSelected)
    val count = values.size

    Canvas(
        modifier =
            modifier
                .semantics { this.contentDescription = description }
                .pointerInput(slotCount, count, yAxisWidth) {
                    detectTapGestures { offset ->
                        if (count == 0 || yAxisWidth >= size.width || offset.x < yAxisWidth) {
                            return@detectTapGestures
                        }
                        nearestIndex(offset.x - yAxisWidth, size.width - yAxisWidth, slotCount, count)?.let {
                            currentOnPointSelected(it)
                        }
                    }
                }.drawWithCache {
                    val chart =
                        buildBalanceChartGeometry(
                            size = size,
                            slotCount = slotCount,
                            values = values,
                            zeroFraction = zeroFraction,
                            yAxisWidth = yAxisWidth,
                            xAxisHeight = xAxisHeight,
                        )
                    onDrawBehind {
                        if (chart.points.isEmpty()) return@onDrawBehind
                        // With large font scales neighbouring y labels can overlap: a label is
                        // skipped (its guide is still drawn) when it would touch the last one drawn.
                        var lastLabelTop = Float.POSITIVE_INFINITY
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
                            val labelTop = (y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height)
                            if (labelTop + layout.size.height <= lastLabelTop) {
                                drawText(
                                    textLayoutResult = layout,
                                    color = labelColor,
                                    topLeft = Offset(x = chart.plotLeft - 6.dp.toPx() - layout.size.width, y = labelTop),
                                )
                                lastLabelTop = labelTop
                            }
                        }

                        chart.fillPath?.let { path ->
                            val lineTop = chart.points.minOf { it.y }
                            val lineBottom = chart.points.maxOf { it.y }
                            val baseBelowLine = chart.fillBaseY >= (lineTop + lineBottom) / 2f
                            val fill = lineColor.copy(alpha = 0.15f)
                            drawPath(
                                path = path,
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            if (baseBelowLine) {
                                                listOf(fill, Color.Transparent)
                                            } else {
                                                listOf(Color.Transparent, fill)
                                            },
                                        startY = minOf(chart.fillBaseY, lineTop),
                                        endY = maxOf(chart.fillBaseY, lineBottom),
                                    ),
                            )
                        }
                        if (zeroFraction != null) {
                            val y = chart.plotBottom - zeroFraction.coerceIn(0f, 1f) * chart.plotHeight
                            drawLine(
                                color = zeroLineColor,
                                start = Offset(chart.plotLeft, y),
                                end = Offset(chart.plotRight, y),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                        }
                        chart.linePath?.let { path ->
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                        if (chart.points.size == 1) {
                            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = chart.points.single())
                        }
                        selectedIndex?.let { selected ->
                            chart.points.getOrNull(selected)?.let { center ->
                                drawCircle(color = surfaceColor, radius = 5.5.dp.toPx(), center = center)
                                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = center)
                            }
                        }

                        for (index in xLayouts.indices) {
                            val layout = xLayouts[index]
                            val slot = xLabels[index].first.coerceIn(0, (slotCount - 1).coerceAtLeast(0))
                            val x = chart.xFor(slot)
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

private fun Density.buildBalanceChartGeometry(
    size: Size,
    slotCount: Int,
    values: List<Float>,
    zeroFraction: Float?,
    yAxisWidth: Float,
    xAxisHeight: Float,
): BalanceChartGeometry {
    val plotLeft = yAxisWidth
    val plotRight = size.width
    val plotTop = 4.dp.toPx()
    val plotBottom = (size.height - xAxisHeight).coerceAtLeast(plotTop + 1.dp.toPx())
    val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
    val plotHeight = plotBottom - plotTop

    fun xFor(index: Int): Float {
        if (slotCount <= 1) return (plotLeft + plotRight) / 2f
        val span = slotCount - 1
        return plotLeft + index.coerceIn(0, span).toFloat() / span * plotWidth
    }

    fun yFor(fraction: Float): Float = plotBottom - fraction.coerceIn(0f, 1f) * plotHeight

    val points = values.mapIndexed { index, fraction -> Offset(xFor(index), yFor(fraction)) }
    val linePath =
        if (points.isEmpty()) {
            null
        } else {
            Path().apply {
                moveTo(points.first().x, points.first().y)
                for (index in 1 until points.size) {
                    lineTo(points[index].x, points[index].y)
                }
            }
        }
    val baseY = if (zeroFraction != null) yFor(zeroFraction) else plotBottom
    val fillPath =
        if (points.isEmpty()) {
            null
        } else {
            Path().apply {
                moveTo(points.first().x, points.first().y)
                for (index in 1 until points.size) {
                    lineTo(points[index].x, points[index].y)
                }
                lineTo(points.last().x, baseY)
                lineTo(points.first().x, baseY)
                close()
            }
        }
    return BalanceChartGeometry(
        plotLeft = plotLeft,
        plotTop = plotTop,
        plotRight = plotRight,
        plotBottom = plotBottom,
        slotCount = slotCount,
        points = points,
        linePath = linePath,
        fillPath = fillPath,
        fillBaseY = baseY,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun BalanceLineChartLightPreview() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        BalanceLineChartPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun BalanceLineChartDarkPreview() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        BalanceLineChartPreviewContent()
    }
}

@Composable
private fun BalanceLineChartPreviewContent() {
    val locale = LocalConfiguration.current.locales[0]
    BalanceLineChart(
        values = listOf(0.1f, 0.3f, 0.2f, 0.45f, 0.6f, 0.52f, 0.7f, 0.66f),
        slotCount = 12,
        zeroFraction = 0.25f,
        xLabels = listOf(0, 5, 11).map { slot -> slot to monthNarrowLetter(YearMonth.of(2026, slot + 1), locale) },
        yLabels =
            listOf(
                compactMoney(-100_000_000L),
                compactMoney(0L),
                compactMoney(100_000_000L),
                compactMoney(200_000_000L),
                compactMoney(300_000_000L),
            ),
        lineColor = MaterialTheme.colorScheme.primary,
        selectedIndex = 7,
        onPointSelected = {},
        contentDescription = stringResource(R.string.reports_balance_evolution),
        modifier = Modifier.fillMaxWidth().height(180.dp),
    )
}
