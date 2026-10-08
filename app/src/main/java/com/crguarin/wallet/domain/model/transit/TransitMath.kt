package com.crguarin.wallet.domain.model.transit

import com.crguarin.wallet.core.money.Money

/**
 * Pure helpers for transit cards: how many trips a balance pays and whether it is too low for one.
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

    /** True when [fare] is set and [balance] cannot pay one more trip ([balance] < [fare]). */
    fun isLowBalance(
        balance: Money,
        fare: Money?,
    ): Boolean = fare != null && fare > Money.ZERO && balance < fare
}
