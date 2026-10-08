package com.crguarin.wallet.ui.debts

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import java.util.Locale

/**
 * Money as plain text for sentences such as "Faltan $ 5.000 de $ 20.000". Non-breaking spaces
 * become normal spaces, the same way [com.crguarin.wallet.ui.components.MoneyText] renders them.
 */
internal fun Money.formatDisplay(locale: Locale): String =
    MoneyFormatter
        .format(this, locale)
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')

/**
 * Share of [amount] already paid, from 0 to 1, for a progress indicator. A progress fraction
 * is not money, so a Float is fine here.
 */
internal fun paymentProgress(
    paid: Money,
    amount: Money,
): Float {
    if (amount.minorUnits <= 0L) return 0f
    return (paid.minorUnits.toFloat() / amount.minorUnits.toFloat()).coerceIn(0f, 1f)
}
