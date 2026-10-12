package com.crguarin.wallet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.motion.LocalIntroFinished
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Inner padding of the hero card. */
private val HERO_PADDING = 22.dp

/** Width of the contour lines. */
private val CONTOUR_STROKE_WIDTH = 1.2.dp

/**
 * Headline card with the signature gradient and the topographic contour lines, for the main figure
 * of a screen. The content is drawn in `walletColors.onHero`.
 *
 * @param shape corner shape of the card; the large shape of the theme by default.
 * @param animateContours when true, the contour lines are traced in `walletMotion.contour`
 *   milliseconds the first time a card asks for it in the process (Home, when it opens); later
 *   cards, and any card with this flag false, show them already drawn. With the system "remove
 *   animations" setting the tracing ends at once.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = WalletBrushes.hero(),
    contentColor: Color = MaterialTheme.walletColors.onHero,
    animateContours: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contourColor = MaterialTheme.walletColors.heroContour
    val traceMillis = MaterialTheme.walletMotion.contour
    // The trace waits for the brand intro: it used to draw under BrandIntro and was never seen.
    val introFinished = LocalIntroFinished.current
    // Decided once per card: whether this composition is the one that traces the lines.
    val traces = remember(introFinished) { introFinished && animateContours && HeroContourTrace.claim() }
    // The trace has not started yet because the intro still covers the screen: the lines stay hidden
    // instead of showing drawn and then restarting.
    val pendingTrace = animateContours && !introFinished && !HeroContourTrace.isClaimed()
    val progress = remember(traces, pendingTrace) { Animatable(if (traces || pendingTrace) 0f else 1f) }
    if (traces) {
        LaunchedEffect(traces) {
            progress.animateTo(1f, tween(durationMillis = traceMillis, easing = LinearOutSlowInEasing))
        }
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(brush)
                    .contourLines(color = contourColor, progress = { progress.value }),
        ) {
            Column(modifier = Modifier.padding(HERO_PADDING), content = content)
        }
    }
}

/**
 * Process-wide memory of whether the contour lines were already traced, so the animation plays the
 * first time Home opens and not on every visit.
 */
internal object HeroContourTrace {
    private var claimed = false

    /** True for the first caller in the process, false for everyone after. */
    fun claim(): Boolean {
        if (claimed) return false
        claimed = true
        return true
    }

    /** True once the trace was claimed in this process. */
    fun isClaimed(): Boolean = claimed

    /** Lets the animation play again. Only for tests. */
    fun reset() {
        claimed = false
    }
}

/**
 * Eight deformed concentric rings centered above the top-right corner, like the contour lines of a
 * map. The reference geometry lives in a box of 328 x 150 dp aligned to the top-right corner and is
 * scaled with the width of the card, so the rings keep their shape at any size.
 */
internal object HeroContours {
    const val COUNT = 8
    const val SEGMENTS = 72

    /** Width (dp) of the reference box. */
    const val REFERENCE_WIDTH = 328f
    private const val CENTER_X = 300f
    private const val CENTER_Y = 18f
    private const val FIRST_RADIUS = 26f
    private const val RADIUS_STEP = 17f
    private const val FLATTEN = 0.78f

    /** Points of ring [index] in reference units: x from the left edge, y from the top edge. */
    fun ring(index: Int): List<Offset> {
        val radius = FIRST_RADIUS + index * RADIUS_STEP
        return List(SEGMENTS + 1) { step ->
            val angle = step.toDouble() / SEGMENTS * 2.0 * PI
            val deformed =
                radius *
                    (1.0 + 0.10 * sin(3.0 * angle + 0.7 * index) + 0.05 * sin(5.0 * angle + 1.3 * index))
            Offset(
                x = (CENTER_X + deformed * cos(angle)).toFloat(),
                y = (CENTER_Y + deformed * FLATTEN * sin(angle)).toFloat(),
            )
        }
    }
}

/** Draws the contour rings behind the content. [progress] (0 to 1) is how much of each ring is traced. */
private fun Modifier.contourLines(
    color: Color,
    progress: () -> Float,
): Modifier =
    drawWithCache {
        val unit = size.width / HeroContours.REFERENCE_WIDTH
        val originX = size.width - HeroContours.REFERENCE_WIDTH * unit
        val paths =
            List(HeroContours.COUNT) { index ->
                smoothClosedPath(HeroContours.ring(index).map { Offset(originX + it.x * unit, it.y * unit) })
            }
        val measures = paths.map { path -> PathMeasure().also { it.setPath(path, false) } }
        val stroke = Stroke(width = CONTOUR_STROKE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val partial = Path()
        onDrawBehind {
            val fraction = progress()
            if (fraction >= 1f) {
                paths.forEach { drawPath(it, color, style = stroke) }
            } else if (fraction > 0f) {
                measures.forEach { measure ->
                    partial.reset()
                    measure.getSegment(0f, measure.length * fraction, partial, startWithMoveTo = true)
                    drawPath(partial, color, style = stroke)
                }
            }
        }
    }

/** Closed curve through [points] (the last equals the first), smoothed with quadratic segments. */
private fun smoothClosedPath(points: List<Offset>): Path {
    val ring = points.dropLast(1)
    val path = Path()
    if (ring.size < 3) return path

    fun mid(
        a: Offset,
        b: Offset,
    ) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val start = mid(ring.last(), ring.first())
    path.moveTo(start.x, start.y)
    ring.forEachIndexed { index, point ->
        val next = ring[(index + 1) % ring.size]
        val end = mid(point, next)
        path.quadraticTo(point.x, point.y, end.x, end.y)
    }
    path.close()
    return path
}

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
private fun HeroCardPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        HeroCardPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true, widthDp = 360)
@Composable
private fun HeroCardPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        HeroCardPreviewContent()
    }
}

@Composable
private fun HeroCardPreviewContent() {
    HeroCard(modifier = Modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.accounts_total_balance), style = MaterialTheme.typography.titleSmall)
        HeroAmountText(text = "$ 1.250.000", color = MaterialTheme.walletColors.onHero)
    }
}
