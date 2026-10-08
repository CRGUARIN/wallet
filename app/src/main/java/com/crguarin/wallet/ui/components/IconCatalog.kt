package com.crguarin.wallet.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import com.crguarin.wallet.R

/** A titled group of icon keys shown together in the picker. */
data class IconGroup(
    @StringRes val titleRes: Int,
    val keys: List<String>,
)

/**
 * Catalog of icons available for categories and accounts.
 *
 * Persisted in the database as string keys. Must safely fallback to a default icon
 * if an unknown key is provided.
 */
object IconCatalog {
    val fallbackIcon: ImageVector = Icons.Default.Category
    const val DEFAULT_ICON_KEY = "more_horiz"

    // Keys are persisted in the database: never rename or remove one.
    private val moneyIcons: List<Pair<String, ImageVector>> =
        listOf(
            "wallet" to Icons.Default.AccountBalanceWallet,
            "savings" to Icons.Default.Savings,
            "credit_card" to Icons.Default.CreditCard,
            "account_balance" to Icons.Default.AccountBalance,
            "payments" to Icons.Default.Payments,
            "monetization_on" to Icons.Default.MonetizationOn,
            "attach_money" to Icons.Default.AttachMoney,
            "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
            "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
            "paid" to Icons.Default.Paid,
            "atm" to Icons.Default.Atm,
            "euro" to Icons.Default.Euro,
            "money" to Icons.Default.Money,
            "currency_exchange" to Icons.Default.CurrencyExchange,
            "calculate" to Icons.Default.Calculate,
            "receipt" to Icons.Default.Receipt,
            "request_quote" to Icons.Default.RequestQuote,
            "price_check" to Icons.Default.PriceCheck,
            "toll" to Icons.Default.Toll,
            "insights" to Icons.Default.Insights,
            "handshake" to Icons.Default.Handshake,
            "volunteer_activism" to Icons.Default.VolunteerActivism,
            "redeem" to Icons.Default.Redeem,
        )

    private val foodIcons: List<Pair<String, ImageVector>> =
        listOf(
            "restaurant" to Icons.Default.Restaurant,
            "local_grocery_store" to Icons.Default.LocalGroceryStore,
            "local_cafe" to Icons.Default.LocalCafe,
            "local_bar" to Icons.Default.LocalBar,
            "fastfood" to Icons.Default.Fastfood,
            "local_pizza" to Icons.Default.LocalPizza,
            "icecream" to Icons.Default.Icecream,
            "bakery_dining" to Icons.Default.BakeryDining,
            "lunch_dining" to Icons.Default.LunchDining,
            "dinner_dining" to Icons.Default.DinnerDining,
            "ramen_dining" to Icons.Default.RamenDining,
            "breakfast_dining" to Icons.Default.BreakfastDining,
            "local_drink" to Icons.Default.LocalDrink,
            "wine_bar" to Icons.Default.WineBar,
            "set_meal" to Icons.Default.SetMeal,
            "kitchen" to Icons.Default.Kitchen,
        )

    private val transportIcons: List<Pair<String, ImageVector>> =
        listOf(
            "directions_bus" to Icons.Default.DirectionsBus,
            "directions_car" to Icons.Default.DirectionsCar,
            "local_taxi" to Icons.Default.LocalTaxi,
            "local_gas_station" to Icons.Default.LocalGasStation,
            "flight" to Icons.Default.Flight,
            "two_wheeler" to Icons.Default.TwoWheeler,
            "motorcycle" to Icons.Default.Motorcycle,
            "pedal_bike" to Icons.Default.PedalBike,
            "train" to Icons.Default.Train,
            "subway" to Icons.Default.Subway,
            "tram" to Icons.Default.Tram,
            "directions_boat" to Icons.Default.DirectionsBoat,
            "local_parking" to Icons.Default.LocalParking,
            "electric_car" to Icons.Default.ElectricCar,
            "ev_station" to Icons.Default.EvStation,
            "commute" to Icons.Default.Commute,
            "car_repair" to Icons.Default.CarRepair,
            "airport_shuttle" to Icons.Default.AirportShuttle,
            "luggage" to Icons.Default.Luggage,
        )

    private val homeIcons: List<Pair<String, ImageVector>> =
        listOf(
            "home" to Icons.Default.Home,
            "apartment" to Icons.Default.Apartment,
            "power" to Icons.Default.Power,
            "bolt" to Icons.Default.Bolt,
            "wifi" to Icons.Default.Wifi,
            "build" to Icons.Default.Build,
            "house" to Icons.Default.House,
            "cottage" to Icons.Default.Cottage,
            "chair" to Icons.Default.Chair,
            "bed" to Icons.Default.Bed,
            "weekend" to Icons.Default.Weekend,
            "bathtub" to Icons.Default.Bathtub,
            "yard" to Icons.Default.Yard,
            "roofing" to Icons.Default.Roofing,
            "local_laundry_service" to Icons.Default.LocalLaundryService,
            "cleaning_services" to Icons.Default.CleaningServices,
            "plumbing" to Icons.Default.Plumbing,
            "handyman" to Icons.Default.Handyman,
            "lightbulb" to Icons.Default.Lightbulb,
            "water_drop" to Icons.Default.WaterDrop,
            "local_fire_department" to Icons.Default.LocalFireDepartment,
            "router" to Icons.Default.Router,
            "tv" to Icons.Default.Tv,
        )

    private val healthIcons: List<Pair<String, ImageVector>> =
        listOf(
            "medical_services" to Icons.Default.MedicalServices,
            "fitness_center" to Icons.Default.FitnessCenter,
            "local_hospital" to Icons.Default.LocalHospital,
            "local_pharmacy" to Icons.Default.LocalPharmacy,
            "medication" to Icons.Default.Medication,
            "healing" to Icons.Default.Healing,
            "monitor_heart" to Icons.Default.MonitorHeart,
            "vaccines" to Icons.Default.Vaccines,
            "psychology" to Icons.Default.Psychology,
            "spa" to Icons.Default.Spa,
            "self_improvement" to Icons.Default.SelfImprovement,
            "content_cut" to Icons.Default.ContentCut,
            "face" to Icons.Default.Face,
        )

    private val shoppingIcons: List<Pair<String, ImageVector>> =
        listOf(
            "shopping_bag" to Icons.Default.ShoppingBag,
            "shopping_cart" to Icons.Default.ShoppingCart,
            "checkroom" to Icons.Default.Checkroom,
            "store" to Icons.Default.Store,
            "storefront" to Icons.Default.Storefront,
            "local_mall" to Icons.Default.LocalMall,
            "shopping_basket" to Icons.Default.ShoppingBasket,
            "sell" to Icons.Default.Sell,
            "local_offer" to Icons.Default.LocalOffer,
            "diamond" to Icons.Default.Diamond,
            "watch" to Icons.Default.Watch,
            "local_florist" to Icons.Default.LocalFlorist,
        )

    private val leisureIcons: List<Pair<String, ImageVector>> =
        listOf(
            "movie" to Icons.Default.Movie,
            "sports_esports" to Icons.Default.SportsEsports,
            "beach_access" to Icons.Default.BeachAccess,
            "celebration" to Icons.Default.Celebration,
            "palette" to Icons.Default.Palette,
            "sports_soccer" to Icons.Default.SportsSoccer,
            "sports_basketball" to Icons.Default.SportsBasketball,
            "music_note" to Icons.Default.MusicNote,
            "headphones" to Icons.Default.Headphones,
            "piano" to Icons.Default.Piano,
            "theater_comedy" to Icons.Default.TheaterComedy,
            "local_activity" to Icons.Default.LocalActivity,
            "nightlife" to Icons.Default.Nightlife,
            "casino" to Icons.Default.Casino,
            "park" to Icons.Default.Park,
            "hiking" to Icons.Default.Hiking,
            "pool" to Icons.Default.Pool,
            "hotel" to Icons.Default.Hotel,
            "photo_camera" to Icons.Default.PhotoCamera,
            "auto_stories" to Icons.Default.AutoStories,
            "brush" to Icons.Default.Brush,
        )

    private val familyIcons: List<Pair<String, ImageVector>> =
        listOf(
            "school" to Icons.Default.School,
            "child_care" to Icons.Default.ChildCare,
            "pets" to Icons.Default.Pets,
            "card_giftcard" to Icons.Default.CardGiftcard,
            "family_restroom" to Icons.Default.FamilyRestroom,
            "favorite" to Icons.Default.Favorite,
            "cake" to Icons.Default.Cake,
            "toys" to Icons.Default.Toys,
            "pregnant_woman" to Icons.Default.PregnantWoman,
            "elderly" to Icons.Default.Elderly,
            "backpack" to Icons.Default.Backpack,
            "science" to Icons.Default.Science,
            "language" to Icons.Default.Language,
            "draw" to Icons.Default.Draw,
        )

    private val techIcons: List<Pair<String, ImageVector>> =
        listOf(
            "phone_android" to Icons.Default.PhoneAndroid,
            "computer" to Icons.Default.Computer,
            "smartphone" to Icons.Default.Smartphone,
            "laptop" to Icons.Default.Laptop,
            "tablet" to Icons.Default.Tablet,
            "phone" to Icons.Default.Phone,
            "email" to Icons.Default.Email,
            "sim_card" to Icons.Default.SimCard,
            "cloud" to Icons.Default.Cloud,
            "subscriptions" to Icons.Default.Subscriptions,
            "videocam" to Icons.Default.Videocam,
            "podcasts" to Icons.Default.Podcasts,
            "gamepad" to Icons.Default.Gamepad,
            "print" to Icons.Default.Print,
        )

    private val otherIcons: List<Pair<String, ImageVector>> =
        listOf(
            "work" to Icons.Default.Work,
            "security" to Icons.Default.Security,
            "star" to Icons.Default.Star,
            "more_horiz" to Icons.Default.MoreHoriz,
            "business_center" to Icons.Default.BusinessCenter,
            "badge" to Icons.Default.Badge,
            "gavel" to Icons.Default.Gavel,
            "church" to Icons.Default.Church,
            "local_shipping" to Icons.Default.LocalShipping,
            "inventory2" to Icons.Default.Inventory2,
            "construction" to Icons.Default.Construction,
            "agriculture" to Icons.Default.Agriculture,
            "eco" to Icons.Default.Eco,
            "recycling" to Icons.Default.Recycling,
            "public" to Icons.Default.Public,
            "key" to Icons.Default.Key,
            "lock" to Icons.Default.Lock,
            "description" to Icons.Default.Description,
            "event" to Icons.Default.Event,
            "schedule" to Icons.Default.Schedule,
            "flag" to Icons.Default.Flag,
        )

    private val iconSources: List<Pair<Int, List<Pair<String, ImageVector>>>> =
        listOf(
            R.string.icon_group_money to moneyIcons,
            R.string.icon_group_food to foodIcons,
            R.string.icon_group_transport to transportIcons,
            R.string.icon_group_home to homeIcons,
            R.string.icon_group_health to healthIcons,
            R.string.icon_group_shopping to shoppingIcons,
            R.string.icon_group_leisure to leisureIcons,
            R.string.icon_group_family to familyIcons,
            R.string.icon_group_tech to techIcons,
            R.string.icon_group_other to otherIcons,
        )

    private val iconsByKey: Map<String, ImageVector> = iconSources.flatMap { it.second }.toMap()

    val groups: List<IconGroup> = iconSources.map { (titleRes, icons) -> IconGroup(titleRes, icons.map { it.first }) }

    val keys: List<String> = groups.flatMap { it.keys }

    fun get(key: String): ImageVector = iconsByKey[key] ?: fallbackIcon
}
