package com.crguarin.wallet.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NameNormalizationTest {
    @Test
    fun `trims and lowercases`() {
        assertThat("  Efectivo  ".normalizedName()).isEqualTo("efectivo")
    }

    @Test
    fun `is locale independent`() {
        assertThat("TITLE".normalizedName()).isEqualTo("title")
    }

    @Test
    fun `blank becomes empty`() {
        assertThat("   ".normalizedName()).isEmpty()
    }
}
