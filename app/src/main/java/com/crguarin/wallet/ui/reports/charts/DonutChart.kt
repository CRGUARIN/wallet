package com.crguarin.wallet.ui.reports.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * One slice of a [DonutChart], with the key the caller uses for selection (it can be any type, so
 * the chart stays free of domain models) and an already normalized fraction.
 *
 * @param fraction share of the whole, `0..1`.
 */
@Immutable
data class DonutSlice<T>(
    val key: T,
    val fraction: Float,
    val color: Color,
)

/**
 * Index of the slice covering [angleDegrees] (measured from the top, clockwise), or null when the
 * angle falls past the last slice.
 *
 * When [Float] rounding makes the fractions add up to less than 1, an angle at the very end falls
 * past every sweep; as a fallback it resolves to the last slice with a positive fraction. When no
 * slice has a positive fraction the result is null.
 */
internal fun sliceIndexAt(
    fractions: List<Float>,
    angleDegrees: Float,
): Int? {
    var cursor = 0f
    for (index in fractions.indices) {
        val sweep = fractions[index] * 360f
        if (angleDegrees >= cursor && angleDegrees < cursor + sweep) return index
        cursor += sweep
    }
    return fractions.indices.lastOrNull { fractions[it] > 0f }
}

/**
 * Stateless donut chart drawn with Compose primitives.
 *
 * Why no charting library (spec 5.3):
 * - Vico has no donut chart.
 * - Charting libraries add APK weight, extra R8 rules and their own theming that fights Material 3
 *   and dynamic color.
 * - The three report charts are simple to draw with [androidx.compose.ui.graphics.drawscope.DrawScope.drawArc],
 *   rounded rects and paths, keeping the look consistent with the rest of the app.
 *
 * The ring starts at -90 degrees (top) and grows clockwise. Slices are separated by 2 degrees so
 * two adjacent categories with the same color stay distinguishable; the selected slice is drawn
 * thicker and the rest dimmed. Taps outside the ring are ignored.
 *
 * @param centerContent content drawn on top of the hole (usually the total or the selected slice).
 * @param contentDescription accessibility summary resolved by the caller with `stringResource`.
 */
@Composable
fun <T> DonutChart(
    slices: List<DonutSlice<T>>,
    selectedKey: T?,
    onSliceClick: (T) -> Unit,
    centerContent: @Composable () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    val description = contentDescription
    val currentOnSliceClick by rememberUpdatedState(onSliceClick)
    val ringWidth = 28.dp
    val selectedRingWidth = 34.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .semantics { this.contentDescription = description }
                    .pointerInput(slices, selectedKey) {
                        detectTapGestures { offset ->
                            if (slices.isEmpty()) return@detectTapGestures
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val outerRadius = min(size.width, size.height) / 2f
                            val innerRadius = outerRadius - selectedRingWidth.toPx()
                            val distance = hypot(dx, dy)
                            if (distance < innerRadius || distance > outerRadius) return@detectTapGestures

                            // Angle from the top, clockwise, matching the drawn arcs.
                            val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f
                            val index = sliceIndexAt(slices.map { it.fraction }, angle)
                            if (index != null) currentOnSliceClick(slices[index].key)
                        }
                    },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f
            val gapDegrees = if (slices.size > 1) 2f else 0f
            val alphaProgress = progress.value
            var cursor = -90f
            for (slice in slices) {
                val isSelected = slice.key == selectedKey
                val strokeWidth = (if (isSelected) selectedRingWidth else ringWidth).toPx()
                val ringRadius = outerRadius - strokeWidth / 2f
                val fullSweep = slice.fraction.coerceIn(0f, 1f) * 360f
                val gap = if (gapDegrees > 0f) min(gapDegrees, fullSweep * 0.5f) else 0f
                val sweep = (fullSweep - gap) * alphaProgress
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color.copy(alpha = if (selectedKey == null || isSelected) 1f else 0.6f),
                        startAngle = cursor + gap / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2f, ringRadius * 2f),
                        style = Stroke(width = strokeWidth),
                    )
                }
                cursor += fullSweep
            }
        }
        centerContent()
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun DonutChartLightPreview() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        DonutChartPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun DonutChartDarkPreview() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        DonutChartPreviewContent()
    }
}

@Composable
private fun DonutChartPreviewContent() {
    val colorScheme = MaterialTheme.colorScheme
    DonutChart(
        slices =
            listOf(
                DonutSlice(key = "Alimentación", fraction = 0.45f, color = colorScheme.primary),
                DonutSlice(key = "Transporte", fraction = 0.20f, color = colorScheme.tertiary),
                DonutSlice(key = "Hogar", fraction = 0.15f, color = colorScheme.secondary),
                DonutSlice(key = "Otras", fraction = 0.20f, color = colorScheme.outline),
            ),
        selectedKey = "Alimentación",
        onSliceClick = {},
        centerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$ 562.500", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        contentDescription = "Gráfico de dona",
        modifier = Modifier.size(200.dp),
    )
}
