package com.crguarin.wallet.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catalog of icons available for categories and accounts.
 *
 * Persisted in the database as string keys. Must safely fallback to a default icon
 * if an unknown key is provided.
 */
object IconCatalog {
    val fallbackIcon: ImageVector = Icons.Default.Category
    const val DEFAULT_ICON_KEY = "more_horiz"

    private val iconsByKey: Map<String, ImageVector> =
        mapOf(
            // Seeded categories (must match DefaultCategoriesSeeder)
            "account_balance" to Icons.Default.AccountBalance,
            "apartment" to Icons.Default.Apartment,
            "card_giftcard" to Icons.Default.CardGiftcard,
            "directions_bus" to Icons.Default.DirectionsBus,
            "home" to Icons.Default.Home,
            "local_gas_station" to Icons.Default.LocalGasStation,
            "local_grocery_store" to Icons.Default.LocalGroceryStore,
            "local_taxi" to Icons.Default.LocalTaxi,
            "medical_services" to Icons.Default.MedicalServices,
            "more_horiz" to Icons.Default.MoreHoriz,
            "movie" to Icons.Default.Movie,
            "payments" to Icons.Default.Payments,
            "power" to Icons.Default.Power,
            "restaurant" to Icons.Default.Restaurant,
            "school" to Icons.Default.School,
            "shopping_bag" to Icons.Default.ShoppingBag,
            "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
            "work" to Icons.Default.Work,
            // Account icons
            "wallet" to Icons.Default.AccountBalanceWallet,
            "savings" to Icons.Default.Savings,
            "credit_card" to Icons.Default.CreditCard,
            "monetization_on" to Icons.Default.MonetizationOn,
            "attach_money" to Icons.Default.AttachMoney,
            // Additional categories & lifestyle icons (~40 total)
            "directions_car" to Icons.Default.DirectionsCar,
            "flight" to Icons.Default.Flight,
            "fitness_center" to Icons.Default.FitnessCenter,
            "sports_esports" to Icons.Default.SportsEsports,
            "pets" to Icons.Default.Pets,
            "local_cafe" to Icons.Default.LocalCafe,
            "local_bar" to Icons.Default.LocalBar,
            "fastfood" to Icons.Default.Fastfood,
            "build" to Icons.Default.Build,
            "checkroom" to Icons.Default.Checkroom,
            "phone_android" to Icons.Default.PhoneAndroid,
            "wifi" to Icons.Default.Wifi,
            "child_care" to Icons.Default.ChildCare,
            "beach_access" to Icons.Default.BeachAccess,
            "celebration" to Icons.Default.Celebration,
            "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
            "security" to Icons.Default.Security,
            "star" to Icons.Default.Star,
            "shopping_cart" to Icons.Default.ShoppingCart,
            "computer" to Icons.Default.Computer,
            "bolt" to Icons.Default.Bolt,
            "palette" to Icons.Default.Palette,
        )

    val keys: List<String> = iconsByKey.keys.toList()

    fun get(key: String): ImageVector = iconsByKey[key] ?: fallbackIcon
}
