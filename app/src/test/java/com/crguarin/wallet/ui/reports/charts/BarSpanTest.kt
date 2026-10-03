package com.crguarin.wallet.ui.reports.charts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BarSpanTest {
    @Test
    fun `value above the baseline spans from the baseline up`() {
        assertThat(barSpan(value = 0.8f, baseline = 0.25f)).isEqualTo(0.25f to 0.8f)
    }

    @Test
    fun `value below the baseline spans from the value up`() {
        assertThat(barSpan(value = 0.1f, baseline = 0.25f)).isEqualTo(0.1f to 0.25f)
    }

    @Test
    fun `value on the baseline spans nothing`() {
        assertThat(barSpan(value = 0.25f, baseline = 0.25f)).isEqualTo(0.25f to 0.25f)
    }

    @Test
    fun `zero baseline keeps the legacy base to value span`() {
        assertThat(barSpan(value = 0.6f, baseline = 0f)).isEqualTo(0f to 0.6f)
    }

    @Test
    fun `ends outside zero to one are clamped`() {
        assertThat(barSpan(value = 1.5f, baseline = -0.5f)).isEqualTo(0f to 1f)
    }
}
