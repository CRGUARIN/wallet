package com.crguarin.wallet.data.seed

import com.crguarin.wallet.domain.model.CategoryType

/**
 * Canonical defaults of the credit-card system categories. The seeder uses them for fresh
 * installs and [com.crguarin.wallet.domain.repository.CategoryRepository.ensureSystemCategory]
 * callers use them to recreate the category on demand.
 */
object SystemCategoryDefaults {
    const val CARD_INTEREST_FEES_KEY = "card_interest_fees"
    val CARD_INTEREST_FEES_TYPE = CategoryType.EXPENSE
    const val CARD_INTEREST_FEES_COLOR = 0xFFEF5350.toInt()
    const val CARD_INTEREST_FEES_ICON = "credit_card"

    const val CARD_ADJUSTMENT_KEY = "card_adjustment"
    val CARD_ADJUSTMENT_TYPE = CategoryType.INCOME
    const val CARD_ADJUSTMENT_COLOR = 0xFF66BB6A.toInt()
    const val CARD_ADJUSTMENT_ICON = "receipt_long"
}
