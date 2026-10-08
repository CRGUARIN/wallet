package com.crguarin.wallet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Rounder corners than the Material defaults: cards and sheets read as soft tiles.
val WalletShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
