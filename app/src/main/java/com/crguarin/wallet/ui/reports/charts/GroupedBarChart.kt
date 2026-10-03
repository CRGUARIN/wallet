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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.ceil

/**
 * One column of a [GroupedBarChart]: a label plus one bar per series (for example income and
 * expense), with colors resolved by the caller.
 *
 * @param values bar positions as fractions of the axis range, `0..1`: each bar goes from the
 *   chart baseline to its value.
 * @param dimmed true for groups that are not meaningful yet (for example future months).
 */
@Immutable
data class BarGroup(
    val label: String,
    val values: List<Float>,
    val colors: List<Color>,
    val dimmed: Boolean = false,
    /** Short label (e.g. a single letter) the chart uses when [label] does not fit in the column. */
    val narrowLabel: String = label,
)

/**
 * Draws one out of every [labelStride] x-axis labels, starting with the first, so crowded labels
 * (for example month names in year mode or very large fonts) never overlap.
 *
 * @param maxLabelWidthPx width of the widest label to draw, in pixels.
 * @param columnWidthPx width of one group column, in pixels.
 */
internal fun labelStride(
    maxLabelWidthPx: Float,
    columnWidthPx: Float,
): Int {
    if (maxLabelWidthPx <= 0f) return 1
    if (columnWidthPx <= 0f) return Int.MAX_VALUE
    return maxOf(1, ceil(maxLabelWidthPx / columnWidthPx).toInt())
}

/**
 * Start and end (ordered, `0..1`) of a bar from [baseline] to [value]: when the value is above
 * the baseline the bar spans `baseline..value`, otherwise `value..baseline`. Both ends are
 * clamped to `0..1`.
 */
internal fun barSpan(
    value: Float,
    baseline: Float,
): Pair<Float, Float> {
    val coercedValue = value.coerceIn(0f, 1f)
    val coercedBaseline = baseline.coerceIn(0f, 1f)
    return minOf(coercedValue, coercedBaseline) to maxOf(coercedValue, coercedBaseline)
}

/**
 * Stateless grouped bar chart: bars grow from the base with rounded top corners, horizontal guide
 * lines come from [yLabels] and the whole column of each group is tappable.
 *
 * The axis labels are drawn with [drawText] and the `labelSmall` style, so they scale with the
 * system font size. Colors are never hardcoded: bars use the colors resolved by the caller and the
 * guides/average line use [androidx.compose.material3.ColorScheme].
 *
 * [BarGroup.values] are POSITIONS 0..1 inside the axis range: each bar goes from
 * [baselineFraction] to its value. With `baselineFraction = 0` this is identical to the previous
 * behavior (bars grow from the base).
 *
 * Bars above the baseline keep the rounded top corners; bars below it are rounded at the bottom
 * and flat at the base. When the baseline is above the base a stronger zero line is drawn over
 * the guides. The optional overlay line joins the group centers point to point, breaking on null
 * values, and shares the growth animation with the bars.
 *
 * @param selectedIndex group drawn at full opacity when set; the rest are dimmed to 0.5.
 * @param averageFraction optional dashed line (0..1 of the axis range), for example the average
 *   expense of the drawn months.
 * @param baselineFraction y position (0..1) of the zero line; bars grow from it.
 * @param lineValues optional overlay line, one y position (0..1) per group; null = no point.
 * @param lineColor color of the overlay line.
 * @param linePointColors color of each point (e.g. by sign); defaults to [lineColor].
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
    baselineFraction: Float = 0f,
    lineValues: List<Float?>? = null,
    lineColor: Color = Color.Unspecified,
    linePointColors: List<Color>? = null,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val fallbackColor = MaterialTheme.colorScheme.outline
    val zeroLineColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()
    val yLayouts =
        remember(yLabels, labelStyle, textMeasurer) {
            yLabels.map { textMeasurer.measure(it, labelStyle) }
        }
    val xLayouts =
        remember(groups, labelStyle, textMeasurer) {
            groups.map { textMeasurer.measure(it.label, labelStyle) }
        }
    val narrowLayouts =
        remember(groups, labelStyle, textMeasurer) {
            groups.map { textMeasurer.measure(it.narrowLabel, labelStyle) }
        }
    val density = LocalDensity.current
    val averageDash = remember(density) { with(density) { PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) } }
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
    // Overlay line paths are reused across frames (rewound on every draw) instead of allocated.
    val netLinePath = remember { Path() }
    val netDimPath = remember { Path() }
    LaunchedEffect(groups, lineValues, baselineFraction) {
        growth.snapTo(0f)
        growth.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

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
        val baseline = baselineFraction.coerceIn(0f, 1f)
        val baselineY = plotBottom - baseline * plotHeight
        val growthValue = growth.value
        for (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            if (group.values.isEmpty()) continue
            val dimmed = group.dimmed || (selectedIndex != null && selectedIndex != groupIndex)
            val alpha = if (dimmed) 0.5f else 1f
            val usable = (columnWidth - innerPadding * 2f).coerceAtLeast(1f)
            val barWidth = ((usable - barGap * (group.values.size - 1)) / group.values.size).coerceAtLeast(1f)
            for (valueIndex in group.values.indices) {
                val target = group.values[valueIndex].coerceIn(0f, 1f)
                val animated = baseline + (target - baseline) * growthValue
                val (spanStart, spanEnd) = barSpan(animated, baseline)
                val top = plotBottom - spanEnd * plotHeight
                val bottom = plotBottom - spanStart * plotHeight
                val barHeight = bottom - top
                if (barHeight <= 0f) continue
                val left = plotLeft + groupIndex * columnWidth + innerPadding + valueIndex * (barWidth + barGap)
                val color = group.colors.getOrElse(valueIndex) { fallbackColor }
                val corner = radius.coerceAtMost(barWidth / 2f).coerceAtMost(barHeight)
                // Only the far end from the base is rounded; the base side stays flat.
                val roundTop = target >= baseline
                val farCorner = CornerRadius(corner)
                val barPath =
                    Path().apply {
                        addRoundRect(
                            RoundRect(
                                Rect(Offset(left, top), Size(barWidth, barHeight)),
                                topLeft = if (roundTop) farCorner else CornerRadius.Zero,
                                topRight = if (roundTop) farCorner else CornerRadius.Zero,
                                bottomRight = if (roundTop) CornerRadius.Zero else farCorner,
                                bottomLeft = if (roundTop) CornerRadius.Zero else farCorner,
                            ),
                        )
                    }
                drawPath(path = barPath, color = color, alpha = alpha)
            }
        }

        if (baseline > 0f) {
            drawLine(
                color = zeroLineColor,
                start = Offset(plotLeft, baselineY),
                end = Offset(plotRight, baselineY),
                strokeWidth = 1.5.dp.toPx(),
            )
        }

        if (averageFraction != null) {
            val y = plotBottom - averageFraction.coerceIn(0f, 1f) * plotHeight
            drawLine(
                color = labelColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = averageDash,
            )
        }

        val overlayValues = lineValues
        if (overlayValues != null && growthValue > 0f) {
            val resolvedLineColor = if (lineColor == Color.Unspecified) fallbackColor else lineColor
            netLinePath.rewind()
            netDimPath.rewind()
            val points = ArrayList<Triple<Offset, Color, Float>>(groups.size)
            var previousPoint: Offset? = null
            var previousDimmed = false
            for (index in groups.indices) {
                val value = overlayValues.getOrNull(index)
                if (value == null) {
                    previousPoint = null
                    continue
                }
                val dimmed = groups[index].dimmed || (selectedIndex != null && selectedIndex != index)
                val animated = baseline + (value.coerceIn(0f, 1f) - baseline) * growthValue
                val point =
                    Offset(
                        plotLeft + index * columnWidth + columnWidth / 2f,
                        plotBottom - animated * plotHeight,
                    )
                previousPoint?.let { start ->
                    val target = if (dimmed || previousDimmed) netDimPath else netLinePath
                    target.moveTo(start.x, start.y)
                    target.lineTo(point.x, point.y)
                }
                previousPoint = point
                previousDimmed = dimmed
                points += Triple(point, linePointColors?.getOrNull(index) ?: resolvedLineColor, if (dimmed) 0.5f else 1f)
            }
            val lineStyle = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(path = netLinePath, color = resolvedLineColor, style = lineStyle)
            drawPath(path = netDimPath, color = resolvedLineColor, alpha = 0.5f, style = lineStyle)
            for ((center, color, alpha) in points) {
                drawCircle(color = color, radius = 3.dp.toPx(), center = center, alpha = alpha)
            }
        }

        val fullMaxWidth = xLayouts.maxOfOrNull { it.size.width }?.toFloat() ?: 0f
        val useNarrow = fullMaxWidth > columnWidth - 2.dp.toPx()
        val activeLayouts = if (useNarrow) narrowLayouts else xLayouts
        val activeMaxWidth = activeLayouts.maxOfOrNull { it.size.width }?.toFloat() ?: 0f
        val stride = labelStride(activeMaxWidth + 2.dp.toPx(), columnWidth)
        activeLayouts.forEachIndexed { index, layout ->
            if (index % stride != 0) return@forEachIndexed
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
