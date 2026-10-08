package com.crguarin.wallet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 sizes with bolder, tighter display and headline styles and semibold titles
private fun walletStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight,
    letterSpacing: Double,
): TextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )

val Typography =
    Typography(
        displayLarge = walletStyle(57, 64, FontWeight.Bold, -0.5),
        displayMedium = walletStyle(45, 52, FontWeight.Bold, -0.5),
        displaySmall = walletStyle(36, 44, FontWeight.Bold, -0.5),
        headlineLarge = walletStyle(32, 40, FontWeight.Bold, -0.5),
        headlineMedium = walletStyle(28, 36, FontWeight.Bold, -0.25),
        headlineSmall = walletStyle(24, 32, FontWeight.SemiBold, 0.0),
        titleLarge = walletStyle(22, 28, FontWeight.SemiBold, 0.0),
        titleMedium = walletStyle(16, 24, FontWeight.SemiBold, 0.1),
        titleSmall = walletStyle(14, 20, FontWeight.SemiBold, 0.1),
        bodyLarge = walletStyle(16, 24, FontWeight.Normal, 0.5),
        bodyMedium = walletStyle(14, 20, FontWeight.Normal, 0.25),
        bodySmall = walletStyle(12, 16, FontWeight.Normal, 0.4),
        labelLarge = walletStyle(14, 20, FontWeight.SemiBold, 0.1),
        labelMedium = walletStyle(12, 16, FontWeight.Medium, 0.5),
        labelSmall = walletStyle(11, 16, FontWeight.Medium, 0.5),
    )
