package com.crguarin.wallet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Soft "pebble" tiles: large radii on cards, sheets and dialogs.
val WalletShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

object WalletShapeTokens {
    /**
     * "Pebble": a square with corners at 36% of its side, for avatars. It gives about 22 dp on the
     * 60 dp account avatar and 15 dp on the 42 dp row avatar.
     */
    val pebble = RoundedCornerShape(percent = 36)
}
