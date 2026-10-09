package com.crguarin.wallet.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GroupedRowsTest {
    private val density = Density(1f)
    private val box = Size(300f, 64f)

    private fun CornerSize.px() = toPx(box, density)

    private fun corners(shape: RoundedCornerShape) =
        listOf(shape.topStart.px(), shape.topEnd.px(), shape.bottomEnd.px(), shape.bottomStart.px())

    @Test
    fun `a group of one is a full rounded card`() {
        assertThat(corners(groupedRowShape(index = 0, count = 1))).containsExactly(24f, 24f, 24f, 24f)
    }

    @Test
    fun `first row has rounded top corners and square bottom corners`() {
        assertThat(corners(groupedRowShape(index = 0, count = 3))).containsExactly(24f, 24f, 0f, 0f).inOrder()
    }

    @Test
    fun `rows in the middle are square`() {
        assertThat(corners(groupedRowShape(index = 1, count = 3))).containsExactly(0f, 0f, 0f, 0f)
    }

    @Test
    fun `last row has rounded bottom corners and square top corners`() {
        assertThat(corners(groupedRowShape(index = 2, count = 3))).containsExactly(0f, 0f, 24f, 24f).inOrder()
    }

    @Test
    fun `the radius can be changed`() {
        assertThat(corners(groupedRowShape(index = 0, count = 2, radius = 12.dp))).containsExactly(12f, 12f, 0f, 0f).inOrder()
    }
}
