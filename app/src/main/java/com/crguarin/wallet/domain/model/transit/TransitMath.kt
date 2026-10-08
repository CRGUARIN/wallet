package com.crguarin.wallet.domain.model.transit

import com.crguarin.wallet.core.money.Money

/**
 * Pure helpers for transit cards: how many trips a balance pays and the single credit trip.
 *
 * Like the Cívica, a card with no balance left (zero or below the fare, but not negative) still
 * lets one trip through on credit; the balance goes negative and the next top-up pays it. While
 * the balance is negative no other trip is allowed.
 */
object TransitMath {
    /** Whole trips [balance] still pays at [fare]; 0 without a positive fare or a positive balance. */
    fun tripsLeft(
        balance: Money,
        fare: Money?,
    ): Int =
        if (fare == null || fare <= Money.ZERO || balance <= Money.ZERO) {
            0
        } else {
            (balance.minorUnits / fare.minorUnits).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }

    /** True while another trip is allowed: the balance is not negative (the credit trip is unused). */
    fun canTravel(balance: Money): Boolean = balance >= Money.ZERO

    /** True when [fare] is set and the next trip can only go through as the credit trip (0 <= [balance] < [fare]). */
    fun isCreditTrip(
        balance: Money,
        fare: Money?,
    ): Boolean = fare != null && fare > Money.ZERO && balance >= Money.ZERO && balance < fare

    /** True when the credit trip is used: the balance is negative until the next top-up. */
    fun isCreditUsed(balance: Money): Boolean = balance < Money.ZERO
}
