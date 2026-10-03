package com.crguarin.wallet.ui.reports.charts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LabelStrideTest {
    @Test
    fun `label that fits returns one`() {
        assertThat(labelStride(maxLabelWidthPx = 50f, columnWidthPx = 100f)).isEqualTo(1)
    }

    @Test
    fun `label exactly as wide as the column returns one`() {
        assertThat(labelStride(maxLabelWidthPx = 100f, columnWidthPx = 100f)).isEqualTo(1)
    }

    @Test
    fun `label twice as wide returns two`() {
        assertThat(labelStride(maxLabelWidthPx = 200f, columnWidthPx = 100f)).isEqualTo(2)
    }

    @Test
    fun `fractional overflow rounds up`() {
        assertThat(labelStride(maxLabelWidthPx = 101f, columnWidthPx = 100f)).isEqualTo(2)
    }

    @Test
    fun `empty label returns one`() {
        assertThat(labelStride(maxLabelWidthPx = 0f, columnWidthPx = 100f)).isEqualTo(1)
    }

    @Test
    fun `zero column width does not crash and draws only the first label`() {
        assertThat(labelStride(maxLabelWidthPx = 50f, columnWidthPx = 0f)).isAtLeast(1)
    }
}
