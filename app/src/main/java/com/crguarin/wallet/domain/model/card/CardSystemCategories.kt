package com.crguarin.wallet.domain.model.card

import com.crguarin.wallet.domain.model.CategoryType

/**
 * Canonical defaults of the credit-card system categories. The seeder uses them for fresh
 * installs, and [com.crguarin.wallet.domain.repository.CategoryRepository.ensureSystemCategory]
 * callers use them to recreate the category on demand. Colors come from the fixed `WalletPalette`.
 */
object CardSystemCategories {
    const val INTEREST_FEES_KEY = "card_interest_fees"
    val INTEREST_FEES_TYPE = CategoryType.EXPENSE
    const val INTEREST_FEES_COLOR = 0xFFEF5350.toInt()
    const val INTEREST_FEES_ICON = "credit_card"

    const val ADJUSTMENT_KEY = "card_adjustment"
    val ADJUSTMENT_TYPE = CategoryType.INCOME
    const val ADJUSTMENT_COLOR = 0xFF66BB6A.toInt()
    const val ADJUSTMENT_ICON = "receipt_long"

    /** Reconciliation relies on these keeping their type, so it cannot be changed. */
    val KEYS: Set<String> = setOf(INTEREST_FEES_KEY, ADJUSTMENT_KEY)
}
