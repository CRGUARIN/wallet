package com.crguarin.wallet.ui.reports.charts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NearestIndexTest {
    @Test
    fun `tap on the edges resolves to the first and last point`() {
        assertThat(nearestIndex(x = 0f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(0)
        assertThat(nearestIndex(x = 1000f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(10)
    }

    @Test
    fun `tap between slots resolves to the nearest one`() {
        // Slots every 100 px: 449 px is closer to slot 4, 451 px to slot 5.
        assertThat(nearestIndex(x = 449f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(4)
        assertThat(nearestIndex(x = 451f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(5)
    }

    @Test
    fun `tap past the last point resolves to the last point`() {
        assertThat(nearestIndex(x = 1200f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(10)
        assertThat(nearestIndex(x = -50f, width = 1000f, slotCount = 11, count = 11)).isEqualTo(0)
    }

    @Test
    fun `tap past the values resolves to the last value, not to an empty slot`() {
        assertThat(nearestIndex(x = 1000f, width = 1000f, slotCount = 12, count = 3)).isEqualTo(2)
        assertThat(nearestIndex(x = 500f, width = 1000f, slotCount = 12, count = 3)).isEqualTo(2)
    }

    @Test
    fun `single slot always resolves to zero`() {
        assertThat(nearestIndex(x = 0f, width = 1000f, slotCount = 1, count = 1)).isEqualTo(0)
        assertThat(nearestIndex(x = 999f, width = 1000f, slotCount = 1, count = 1)).isEqualTo(0)
    }

    @Test
    fun `single value resolves to zero on any tap`() {
        assertThat(nearestIndex(x = 900f, width = 1000f, slotCount = 12, count = 1)).isEqualTo(0)
    }

    @Test
    fun `nothing to tap resolves to null`() {
        assertThat(nearestIndex(x = 500f, width = 1000f, slotCount = 12, count = 0)).isNull()
        assertThat(nearestIndex(x = 500f, width = 1000f, slotCount = 0, count = 0)).isNull()
        assertThat(nearestIndex(x = 500f, width = 0f, slotCount = 12, count = 5)).isNull()
    }
}
