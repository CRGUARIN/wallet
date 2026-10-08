package com.crguarin.wallet.domain.model.transit

/** Categories used for trips: the seeded "public_transport" subcategory, else the "transport" root. */
object TransitSystemCategories {
    const val TRIP_KEY = "public_transport"
    const val FALLBACK_KEY = "transport"
    const val FALLBACK_COLOR = 0xFFFFA726.toInt()
    const val FALLBACK_ICON = "directions_bus"
}
