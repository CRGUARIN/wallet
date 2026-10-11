package com.crguarin.wallet.ui.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OdometerMathTest {
    @Test
    fun `same length pairs digits one to one`() {
        val slots = OdometerMath.plan("$ 12.500", "$ 28.000")

        assertThat(slots.map { it.to }).containsExactly('2', '8', '0', '0', '0').inOrder()
        assertThat(slots.map { it.from }).containsExactly('1', '2', '5', '0', '0').inOrder()
    }

    @Test
    fun `longer text rolls the new digits from zero`() {
        val slots = OdometerMath.plan("$ 80.000", "$ 1.280.000")

        assertThat(slots.map { it.to }).containsExactly('1', '2', '8', '0', '0', '0', '0').inOrder()
        assertThat(slots.map { it.from }).containsExactly('0', '0', '8', '0', '0', '0', '0').inOrder()
    }

    @Test
    fun `shorter text drops the extra leading digits with no animation`() {
        val slots = OdometerMath.plan("$ 1.280.000", "$ 80.000")

        assertThat(slots.map { it.to }).containsExactly('8', '0', '0', '0', '0').inOrder()
        assertThat(slots.map { it.from }).containsExactly('8', '0', '0', '0', '0').inOrder()
    }

    @Test
    fun `negative amounts keep the sign out of the slots`() {
        val slots = OdometerMath.plan("-$ 1.120.000", "-$ 999.999.999.999")

        assertThat(slots).hasSize(12)
        assertThat(slots.first().to).isEqualTo('9')
        assertThat(slots.first().from).isEqualTo('0')
    }

    @Test
    fun `identical texts plan still columns`() {
        val slots = OdometerMath.plan("$ 12.500", "$ 12.500")

        assertThat(slots).hasSize(5)
        slots.forEach { assertThat(it.from).isEqualTo(it.to) }
    }

    @Test
    fun `zeroed replaces only digits keeping the length`() {
        assertThat(OdometerMath.zeroed("$ 1.280.000")).isEqualTo("$ 0.000.000")
        assertThat(OdometerMath.zeroed("-$ 999.999.999.999")).isEqualTo("-$ 000.000.000.000")
        assertThat(OdometerMath.zeroed("$ 1.280.000")).hasLength("$ 1.280.000".length)
    }
}
