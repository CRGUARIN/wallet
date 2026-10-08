package com.crguarin.wallet.domain.model.transit

import com.crguarin.wallet.core.money.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransitMathTest {
    private val fare = Money(350_000L)

    @Test
    fun `trips left are the whole trips the balance pays`() {
        assertThat(TransitMath.tripsLeft(Money(1_050_000L), fare)).isEqualTo(3)
        assertThat(TransitMath.tripsLeft(Money(1_049_999L), fare)).isEqualTo(2)
        assertThat(TransitMath.tripsLeft(fare, fare)).isEqualTo(1)
    }

    @Test
    fun `trips left are zero when the balance cannot pay a trip`() {
        assertThat(TransitMath.tripsLeft(Money(349_999L), fare)).isEqualTo(0)
        assertThat(TransitMath.tripsLeft(Money.ZERO, fare)).isEqualTo(0)
        assertThat(TransitMath.tripsLeft(Money(-500_000L), fare)).isEqualTo(0)
    }

    @Test
    fun `trips left are zero without a positive fare`() {
        assertThat(TransitMath.tripsLeft(Money(1_050_000L), null)).isEqualTo(0)
        assertThat(TransitMath.tripsLeft(Money(1_050_000L), Money.ZERO)).isEqualTo(0)
        assertThat(TransitMath.tripsLeft(Money(1_050_000L), Money(-1L))).isEqualTo(0)
    }

    @Test
    fun `trips left are capped at the largest int`() {
        assertThat(TransitMath.tripsLeft(Money.MAX, Money(1L))).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `low balance is a balance below one fare`() {
        assertThat(TransitMath.isLowBalance(Money(349_999L), fare)).isTrue()
        assertThat(TransitMath.isLowBalance(Money(-1L), fare)).isTrue()
        assertThat(TransitMath.isLowBalance(fare, fare)).isFalse()
        assertThat(TransitMath.isLowBalance(Money(1_050_000L), fare)).isFalse()
    }

    @Test
    fun `low balance is false without a positive fare`() {
        assertThat(TransitMath.isLowBalance(Money.ZERO, null)).isFalse()
        assertThat(TransitMath.isLowBalance(Money.ZERO, Money.ZERO)).isFalse()
        assertThat(TransitMath.isLowBalance(Money(-100L), Money(-1L))).isFalse()
    }
}
