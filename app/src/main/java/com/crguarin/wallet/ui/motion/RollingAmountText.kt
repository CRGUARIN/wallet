package com.crguarin.wallet.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletTypography

/**
 * Decides where [RollingAmountText] rolls from:
 * - on its first show in this entry, from [OdometerMath.zeroed] of [text] when
 *   [fromZeroOnFirstShow], else no roll;
 * - when [text] changed since the last show in this entry, from the previous text (this is what
 *   makes the Home balance roll when coming back after saving a movement);
 * - when [text] did not change, or without animations: null (no roll).
 *
 * Survives rotation and navigation (`rememberSaveable`, keyed by [saveKey]).
 */
@Composable
fun rememberRollFrom(
    text: String,
    saveKey: String,
    fromZeroOnFirstShow: Boolean,
): String? {
    val animate = animationsOn()
    var lastText by rememberSaveable(key = "roll-last:$saveKey") { mutableStateOf<String?>(null) }
    var currentFrom by rememberSaveable(key = "roll-from:$saveKey") { mutableStateOf<String?>(null) }
    if (lastText != text) {
        currentFrom =
            when {
                !animate -> null
                lastText == null -> if (fromZeroOnFirstShow) OdometerMath.zeroed(text) else null
                else -> lastText
            }
        lastText = text
    } else if (!animate) {
        currentFrom = null
    }
    return currentFrom
}

/**
 * An amount whose digits roll upwards to their new value, like a split-flap board. Only digits
 * move ("$" small and raised, sign, dots and spaces stay fixed); digits align from the right and
 * new longer digits roll from 0 (see [OdometerMath]). Each digit takes `contour` (700 ms) with
 * the `roll` easing, delayed by `digitStagger` × its position from the left.
 *
 * It shrinks from [style].[TextStyle.fontSize] down to [minFontSize] to fit one line, measuring
 * the FINAL text, and is never truncated. It draws with a [TextMeasurer] in the draw phase (two
 * glyph rows per column, clipped): no `Text` per digit and no recomposition per frame.
 *
 * Semantics are the same as the composable it replaces: a single node with the exact text, plus
 * [contentDescription] when given, so `onNodeWithText("$ 12.500")` keeps finding one node and no
 * loose digit is a node. TalkBack reads the final amount once, never digit by digit.
 */
@Composable
fun RollingAmountText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.walletTypography.heroAmount,
    rollFrom: String? = null,
    minFontSize: TextUnit = 22.sp,
    contentDescription: String? = null,
    symbolScale: Float = 0.58f,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()

    val slots =
        remember(rollFrom, text) {
            if (rollFrom == null || rollFrom == text) emptyList() else OdometerMath.plan(rollFrom, text)
        }
    val totalMillis = slots.size * motion.digitStagger + motion.contour
    val master = remember(text, rollFrom) { Animatable(if (slots.isEmpty()) 1f else 0f) }
    LaunchedEffect(text, rollFrom, animate) {
        if (!animate || slots.isEmpty()) {
            master.snapTo(1f)
        } else {
            master.snapTo(0f)
            master.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = totalMillis, easing = LinearEasing),
            )
        }
    }

    // Same floor as HeroAmountText: at large font scales the 22 sp floor is held at 22 dp.
    val floor =
        with(density) {
            if (minFontSize.isSp) minOf(minFontSize.value, 22.dp.toSp().value).sp else 22.sp
        }
    val fitStyle =
        remember(style, color) {
            val lineHeight =
                if (style.lineHeight.isSp && style.fontSize.isSp) {
                    (style.lineHeight.value / style.fontSize.value).em
                } else {
                    style.lineHeight
                }
            style.copy(color = color, lineHeight = lineHeight)
        }
    val annotated = remember(text, symbolScale) { annotatedWithSmallSymbol(text, symbolScale) }

    BoxWithConstraints(
        modifier =
            modifier.clearAndSetSemantics {
                this.text = AnnotatedString(text)
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val drawing =
            remember(annotated, fitStyle, floor, maxWidthPx, slots) {
                planDrawing(measurer, annotated, fitStyle, floor, maxWidthPx, slots)
            }
        val heightDp = with(density) { drawing.heightPx.toDp() }
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .drawBehind {
                        drawing.staticCells.forEach { cell ->
                            drawText(cell.layout, topLeft = Offset(cell.xPx, 0f))
                        }
                        val progress = master.value
                        drawing.columns.forEachIndexed { position, column ->
                            val raw =
                                (progress * totalMillis - position * motion.digitStagger) / motion.contour
                            val local = raw.coerceIn(0f, 1f)
                            val exact = motion.roll.transform(local) * column.steps
                            val index = exact.toInt().coerceAtMost(column.steps)
                            val fraction = exact - index
                            val first = drawing.digitLayouts.getValue(digitAt(column.slot.from, index))
                            clipRect(
                                left = column.xPx,
                                top = 0f,
                                right = column.xPx + column.widthPx,
                                bottom = size.height,
                            ) {
                                drawText(first, topLeft = Offset(column.xPx, -fraction * size.height))
                                if (fraction > 0f) {
                                    val second =
                                        drawing.digitLayouts.getValue(digitAt(column.slot.from, index + 1))
                                    drawText(
                                        second,
                                        topLeft = Offset(column.xPx, (1f - fraction) * size.height),
                                    )
                                }
                            }
                        }
                    },
        )
    }
}

/** How much the "$" is raised, as a [BaselineShift] multiplier: same as `HeroAmountText`. */
private const val SYMBOL_RAISE = 0.25f

private fun annotatedWithSmallSymbol(
    text: String,
    symbolScale: Float,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val symbol = text.indexOf('$')
        if (symbol >= 0) {
            val spaceFollows = symbol + 1 < text.length && text[symbol + 1].isWhitespace()
            addStyle(
                SpanStyle(
                    fontSize = symbolScale.em,
                    baselineShift = BaselineShift(SYMBOL_RAISE),
                ),
                start = symbol,
                end = if (spaceFollows) symbol + 2 else symbol + 1,
            )
        }
    }

private data class StaticCell(
    val layout: TextLayoutResult,
    val xPx: Float,
)

private data class DigitColumn(
    val slot: OdometerSlot,
    val xPx: Float,
    val widthPx: Float,
    val steps: Int,
)

private data class AmountDrawing(
    val heightPx: Int,
    val staticCells: List<StaticCell>,
    val columns: List<DigitColumn>,
    val digitLayouts: Map<Char, TextLayoutResult>,
)

private fun digitAt(
    from: Char,
    step: Int,
): Char = '0' + ((from - '0' + step) % 10)

private fun planDrawing(
    measurer: TextMeasurer,
    annotated: AnnotatedString,
    style: TextStyle,
    floor: TextUnit,
    maxWidthPx: Float,
    slots: List<OdometerSlot>,
): AmountDrawing {
    val top = if (style.fontSize.isSp) style.fontSize.value else floor.value
    var sizeSp = top
    while (sizeSp > floor.value) {
        val width =
            measurer.measure(annotated, style = style.copy(fontSize = sizeSp.sp), maxLines = 1).size.width
        if (width <= maxWidthPx) break
        sizeSp -= 1f
    }
    val sized = style.copy(fontSize = sizeSp.sp)
    val full =
        measurer.measure(
            annotated,
            style = sized,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidthPx.toInt().coerceAtLeast(0)),
        )
    val digitLayouts = ('0'..'9').associateWith { measurer.measure(it.toString(), style = sized, maxLines = 1) }

    var x = 0f
    val staticCells = mutableListOf<StaticCell>()
    val digitX = mutableListOf<Pair<Float, Float>>()
    annotated.forEachIndexed { index, _ ->
        val cell = annotated.subSequence(index, index + 1)
        val cellLayout = measurer.measure(cell, style = sized, maxLines = 1)
        val width = cellLayout.size.width.toFloat()
        if (annotated[index].isDigit() && slots.isNotEmpty()) {
            digitX += x to width
        } else {
            staticCells += StaticCell(cellLayout, x)
        }
        x += width
    }
    val columns =
        digitX.mapIndexed { position, (digitXpx, digitWidth) ->
            val slot = slots[position]
            DigitColumn(
                slot = slot,
                xPx = digitXpx,
                widthPx = digitWidth,
                steps = (slot.to - slot.from + 10) % 10,
            )
        }
    return AmountDrawing(
        heightPx = full.size.height,
        staticCells = staticCells,
        columns = columns,
        digitLayouts = digitLayouts,
    )
}

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
private fun RollingAmountTextPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        RollingAmountPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true, widthDp = 360)
@Composable
private fun RollingAmountTextPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        RollingAmountPreviewContent()
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun RollingAmountTextPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        RollingAmountPreviewContent()
    }
}

@Composable
private fun RollingAmountPreviewContent() {
    Column(
        modifier = Modifier.fillMaxWidth().background(brush = WalletBrushes.hero()),
    ) {
        RollingAmountText(text = "$ 2.340.500", color = Color.White)
        RollingAmountText(text = "$ 999.999.999.999", color = Color.White)
        RollingAmountText(text = "-$ 1.120.000", color = Color.White)
    }
}
