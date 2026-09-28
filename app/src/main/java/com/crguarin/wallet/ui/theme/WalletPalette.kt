package com.crguarin.wallet.ui.theme

/**
 * Fixed palette of colors used for accounts and categories.
 * Contains all colors used by [com.crguarin.wallet.data.seed.DefaultCategoriesSeeder].
 */
object WalletPalette {
    val colors: List<Int> =
        listOf(
            0xFFEF5350.toInt(), // Red (Food)
            0xFFEC407A.toInt(), // Pink (Entertainment)
            0xFFAB47BC.toInt(), // Purple (Housing)
            0xFF42A5F5.toInt(), // Blue (Education)
            0xFF26C6DA.toInt(), // Cyan (Freelance)
            0xFF26A69A.toInt(), // Teal (Health)
            0xFF66BB6A.toInt(), // Green (Salary)
            0xFF9CCC65.toInt(), // Light Green (Interest)
            0xFFFFCA28.toInt(), // Amber (Gifts)
            0xFFFFA726.toInt(), // Orange (Transport)
            0xFFFF7043.toInt(), // Deep Orange (Shopping)
            0xFF8D6E63.toInt(), // Brown (Other income)
            0xFF78909C.toInt(), // Blue Grey (Other expense)
        )

    val defaultColor: Int = colors[0]
}
