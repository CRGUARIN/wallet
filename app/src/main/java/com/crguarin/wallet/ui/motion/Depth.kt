package com.crguarin.wallet.ui.motion

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.walletColors

/**
 * Elevated surface: background plus a soft two-layer shadow, following the theme. Light uses
 * `surfaceContainerLowest` with onSurface shadows (y 1 dp radius 2 dp at 6 %, y 8 dp radius
 * 24 dp at 7 %); dark uses `surfaceContainer` with black shadows (40 % and 35 %). The [Shadow]s
 * are remembered, never rebuilt per frame.
 */
fun Modifier.raisedSurface(shape: Shape): Modifier =
    composed {
        val scheme = MaterialTheme.colorScheme
        val dark = MaterialTheme.walletColors.isDark
        val background = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLowest
        val near =
            remember(dark, scheme) {
                if (dark) {
                    Shadow(radius = 2.dp, color = Color.Black, offset = DpOffset(0.dp, 1.dp), alpha = 0.40f)
                } else {
                    Shadow(
                        radius = 2.dp,
                        color = scheme.onSurface,
                        offset = DpOffset(0.dp, 1.dp),
                        alpha = 0.06f,
                    )
                }
            }
        val far =
            remember(dark, scheme) {
                if (dark) {
                    Shadow(radius = 24.dp, color = Color.Black, offset = DpOffset(0.dp, 8.dp), alpha = 0.35f)
                } else {
                    Shadow(
                        radius = 24.dp,
                        color = scheme.onSurface,
                        offset = DpOffset(0.dp, 8.dp),
                        alpha = 0.07f,
                    )
                }
            }
        this
            .background(background, shape)
            .dropShadow(shape, near)
            .dropShadow(shape, far)
    }

/**
 * Shadow of a deck or hero card: y 14 dp radius 28 dp black 28 % in light, y 16 dp radius 32 dp
 * black 60 % in dark, plus a 1 dp light edge at the top inside (white 18 % light, 14 % dark).
 * Decorative: it adds no semantics.
 */
fun Modifier.cardShadow(shape: Shape): Modifier =
    composed {
        val dark = MaterialTheme.walletColors.isDark
        val shadow =
            remember(dark) {
                if (dark) {
                    Shadow(radius = 32.dp, color = Color.Black, offset = DpOffset(0.dp, 16.dp), alpha = 0.60f)
                } else {
                    Shadow(radius = 28.dp, color = Color.Black, offset = DpOffset(0.dp, 14.dp), alpha = 0.28f)
                }
            }
        val edgeAlpha = if (dark) 0.14f else 0.18f
        this
            .dropShadow(shape, shadow)
            .drawTopEdge(shape, edgeAlpha)
    }

/** 1 dp light line at the top, clipped inside [shape]. */
private fun Modifier.drawTopEdge(
    shape: Shape,
    alpha: Float,
): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip =
            Path().apply {
                when (outline) {
                    is Outline.Rectangle -> addRect(outline.rect)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Generic -> addPath(outline.path)
                }
            }
        val strokePx = 1.dp.toPx()
        onDrawWithContent {
            drawContent()
            clipPath(clip) {
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(0f, strokePx / 2f),
                    end = Offset(size.width, strokePx / 2f),
                    strokeWidth = strokePx,
                )
            }
        }
    }
