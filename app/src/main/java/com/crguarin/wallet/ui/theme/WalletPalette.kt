package com.crguarin.wallet.ui.theme

/**
 * Fixed palette of colors used for accounts and categories, grouped by hue.
 * Contains all colors used by [com.crguarin.wallet.data.seed.DefaultCategoriesSeeder].
 */
object WalletPalette {
    val colors: List<Int> =
        listOf(
            0xFFEF5350.toInt(), // Red 400 (Food)
            0xFFC62828.toInt(), // Red 800 (Card interest and fees)
            0xFFEC407A.toInt(), // Pink 400 (Entertainment)
            0xFFAD1457.toInt(), // Pink 800
            0xFFAB47BC.toInt(), // Purple 400 (Housing)
            0xFF6A1B9A.toInt(), // Purple 800
            0xFF7E57C2.toInt(), // Deep Purple 400
            0xFF5C6BC0.toInt(), // Indigo 400
            0xFF283593.toInt(), // Indigo 800
            0xFF42A5F5.toInt(), // Blue 400 (Education)
            0xFF1565C0.toInt(), // Blue 800
            0xFF0288D1.toInt(), // Light Blue 700
            0xFF26C6DA.toInt(), // Cyan 400 (Freelance)
            0xFF00838F.toInt(), // Cyan 800
            0xFF26A69A.toInt(), // Teal 400 (Health)
            0xFF00695C.toInt(), // Teal 800
            0xFF66BB6A.toInt(), // Green 400 (Salary)
            0xFF2E7D32.toInt(), // Green 800 (Card adjustment)
            0xFF9CCC65.toInt(), // Light Green 400 (Interest)
            0xFFC0CA33.toInt(), // Lime 600
            0xFFFFCA28.toInt(), // Amber 400 (Gifts)
            0xFFFFA726.toInt(), // Orange 400 (Transport)
            0xFFEF6C00.toInt(), // Orange 800
            0xFFFF7043.toInt(), // Deep Orange 400 (Shopping)
            0xFFD84315.toInt(), // Deep Orange 800
            0xFF8D6E63.toInt(), // Brown 400 (Other income)
            0xFF4E342E.toInt(), // Brown 800
            0xFF616161.toInt(), // Grey 700
            0xFF78909C.toInt(), // Blue Grey 400 (Other expense)
            0xFF37474F.toInt(), // Blue Grey 800
        )

    val defaultColor: Int = colors[0]
}
