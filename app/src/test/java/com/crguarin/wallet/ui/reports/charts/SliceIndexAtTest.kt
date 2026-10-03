package com.crguarin.wallet.ui.reports.charts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SliceIndexAtTest {
    @Test
    fun `angle past float-rounded total falls back to the last positive slice`() {
        val fractions = listOf(0.45f, 0.35f, 0.1999f)
        assertThat(sliceIndexAt(fractions, 359.99f)).isEqualTo(2)
    }

    @Test
    fun `angle exactly on a border belongs to the next slice`() {
        val fractions = listOf(0.25f, 0.25f, 0.5f)
        assertThat(sliceIndexAt(fractions, 90f)).isEqualTo(1)
    }

    @Test
    fun `angle inside the first slice returns zero`() {
        val fractions = listOf(0.5f, 0.5f)
        assertThat(sliceIndexAt(fractions, 10f)).isEqualTo(0)
    }

    @Test
    fun `all zero fractions return null`() {
        assertThat(sliceIndexAt(listOf(0f, 0f, 0f), 100f)).isNull()
    }

    @Test
    fun `fallback skips trailing zero slices`() {
        val fractions = listOf(0.5f, 0.4999f, 0f)
        assertThat(sliceIndexAt(fractions, 359.99f)).isEqualTo(1)
    }
}
