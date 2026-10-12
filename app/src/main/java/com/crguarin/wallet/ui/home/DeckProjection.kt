package com.crguarin.wallet.ui.home

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Where one deck card sits: its layout top and height (container px), its depth towards the
 * viewer, and the optional local transform of the deal (a rotation about its own center, an extra
 * translation and a scale). Rest pose: no local transform, scale 1.
 */
data class CardPose(
    val top: Float,
    val height: Float,
    val depth: Float,
    val rotateXDeg: Float = 0f,
    val dy: Float = 0f,
    val dz: Float = 0f,
    val scale: Float = 1f,
)

/**
 * The CSS 3D math of the account deck, in pixels, with no Android types. The deck is one group
 * rotated [tiltDeg] about its bottom edge (`rotateX`), seen through a perspective of [perspective]
 * from the point (W/2, -0.2 H). CSS conventions: y goes down, z goes towards the viewer.
 *
 * Only the group tilt, the depth and the local transforms bend the cards: a point's vertical
 * position depends on its layout y and depth, not on x. So [projectY] and [stripSpan] need no
 * width, and the hit targets can be sized before the deck is measured.
 */
class DeckProjection(
    /** Height of the deck container (px). The group pivots on its bottom edge. */
    val height: Float,
    /** Distance of the viewer from the deck plane (px). */
    val perspective: Float,
    /** Backward tilt of the group, in degrees. */
    val tiltDeg: Float,
) {
    private val pivotY = height
    private val originY = -0.2f * height
    private val cosTilt = cos(Math.toRadians(tiltDeg.toDouble())).toFloat()
    private val sinTilt = sin(Math.toRadians(tiltDeg.toDouble())).toFloat()

    /**
     * Projected Y (container px) of a horizontal line at layout [y] and [depth] (px towards the
     * viewer), with the group tilted by [tilt]. It is the same for every x of that line.
     */
    fun projectY(
        y: Float,
        depth: Float,
        tilt: Float = tiltDeg,
    ): Float {
        val c = cos(Math.toRadians(tilt.toDouble())).toFloat()
        val s = sin(Math.toRadians(tilt.toDouble())).toFloat()
        val rel = y - pivotY
        val yGroup = pivotY + rel * c - depth * s
        val zGroup = rel * s + depth * c
        return originY + (yGroup - originY) * perspective / (perspective - zGroup)
    }

    /**
     * Vertical span (min, max) on screen of the horizontal band from layout [top] at [topDepth] to
     * layout [bottom] at [bottomDepth]: the visible strip of a card runs from its own top edge to
     * the top edge of the card in front. Returned as `[min, max]` in container px.
     */
    fun stripSpan(
        top: Float,
        topDepth: Float,
        bottom: Float,
        bottomDepth: Float,
        tilt: Float = tiltDeg,
    ): FloatArray {
        val a = projectY(top, topDepth, tilt)
        val b = projectY(bottom, bottomDepth, tilt)
        return floatArrayOf(min(a, b), max(a, b))
    }

    /**
     * The 4 projected corners of a card of container [width], in the order TL, TR, BR, BL, as
     * `[x0, y0, x1, y1, x2, y2, x3, y3]` in container px. The card's local transform is applied
     * first (scale, then rotation about its center, then translation), then the group tilt, then
     * the perspective. With [tilt] 0 and no depth the corners are the layout rectangle.
     */
    fun cardCorners(
        width: Float,
        pose: CardPose,
        tilt: Float = tiltDeg,
    ): FloatArray {
        val out = FloatArray(8)
        val c = cos(Math.toRadians(tilt.toDouble())).toFloat()
        val s = sin(Math.toRadians(tilt.toDouble())).toFloat()
        val alpha = Math.toRadians(pose.rotateXDeg.toDouble())
        val ca = cos(alpha).toFloat()
        val sa = sin(alpha).toFloat()
        val ox = width / 2f
        val centerY = pose.top + pose.height / 2f
        val corners =
            floatArrayOf(
                0f,
                0f,
                width,
                0f,
                width,
                pose.height,
                0f,
                pose.height,
            )
        for (k in 0 until 4) {
            // Local, relative to the card center, scaled.
            val lx = (corners[2 * k] - width / 2f) * pose.scale
            val ly = (corners[2 * k + 1] - pose.height / 2f) * pose.scale
            // Local rotateX about the center (CSS convention), then the extra translation.
            val x = ox + lx
            val y = centerY + ca * ly + pose.dy
            val z = pose.depth + sa * ly + pose.dz
            // Group tilt about the pivot.
            val rel = y - pivotY
            val yGroup = pivotY + rel * c - z * s
            val zGroup = rel * s + z * c
            val f = perspective / (perspective - zGroup)
            out[2 * k] = ox + (x - ox) * f
            out[2 * k + 1] = originY + (yGroup - originY) * f
        }
        return out
    }
}
