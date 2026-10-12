package com.crguarin.wallet.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeckProjectionTest {
    private val width = 360f
    private val height = 420f
    private val perspective = 1100f

    private fun flat() = DeckProjection(height = height, perspective = perspective, tiltDeg = 0f)

    private fun tilted() = DeckProjection(height = height, perspective = perspective, tiltDeg = 24f)

    @Test
    fun `no tilt and no depth gives the layout rectangle`() {
        val corners = flat().cardCorners(width, CardPose(top = 54f, height = 190f, depth = 0f))

        assertThat(corners.toList())
            .containsExactly(0f, 54f, width, 54f, width, 244f, 0f, 244f)
            .inOrder()
    }

    @Test
    fun `a point at the pivot does not move`() {
        val projection = tilted()
        // The pivot is the bottom center of the container, at depth 0.
        val y = projection.projectY(height, depth = 0f)

        assertThat(y).isWithin(1e-3f).of(height)
    }

    @Test
    fun `the top edge of the back card is narrower than the top edge of the front card`() {
        val projection = tilted()
        val back = projection.cardCorners(width, CardPose(top = 0f, height = 190f, depth = 0f))
        val front = projection.cardCorners(width, CardPose(top = 200f, height = 190f, depth = 54f))

        val backTopWidth = back[2] - back[0]
        val frontTopWidth = front[2] - front[0]
        assertThat(backTopWidth).isLessThan(frontTopWidth)
    }

    @Test
    fun `a tilted deck projects its top card above its layout top`() {
        val projection = tilted()

        // The top card recedes and its top edge rises less than its layout offset suggests, but it
        // stays inside the container.
        val top = projection.projectY(0f, depth = 0f)
        assertThat(top).isGreaterThan(0f)
        assertThat(top).isLessThan(height / 4f)
    }

    @Test
    fun `strip span returns min and max of the two edges`() {
        val projection = tilted()

        val span = projection.stripSpan(top = 0f, topDepth = 0f, bottom = 66f, bottomDepth = 18f)

        assertThat(span[0]).isLessThan(span[1])
        assertThat(span[0]).isEqualTo(projection.projectY(0f, 0f))
        assertThat(span[1]).isEqualTo(projection.projectY(66f, 18f))
    }

    @Test
    fun `every visible strip of a three account deck is at least 48 dp tall`() {
        val strip = 66f
        val step = 18f
        val lift = 28f
        val total = 196f
        val count = 3
        val deckHeight = strip * count + total + 8f
        val projection = DeckProjection(height = deckHeight, perspective = 1100f, tiltDeg = 24f)

        fun top(i: Int) = strip * i

        fun depth(i: Int) = if (i < count) step * i else step * (count - 1) + lift

        for (i in 0 until count) {
            val span = projection.stripSpan(top(i), depth(i), top(i + 1), depth(i + 1))
            assertThat(span[1] - span[0]).isAtLeast(48f)
        }
    }
}
