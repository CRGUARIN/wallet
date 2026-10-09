package com.crguarin.wallet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R

private fun manrope(weight: Int): Font =
    Font(
        resId = R.font.manrope,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

/**
 * Manrope (SIL OFL 1.1), one variable file (wght axis 200 to 800) bundled in `res/font/manrope.ttf`.
 * Variable fonts work from API 26, the app minSdk.
 */
val ManropeFontFamily =
    FontFamily(
        manrope(400),
        manrope(500),
        manrope(600),
        manrope(700),
        manrope(800),
    )

private fun walletStyle(
    fontSize: Int,
    lineHeight: Int,
    weight: Int,
    tracking: Double,
): TextStyle =
    TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight(weight),
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking.em,
    )

// Very black geometric headings (800) with tight tracking, medium body (500), bold titles and labels (700).
val Typography =
    Typography(
        displayLarge = walletStyle(57, 64, 800, -0.02),
        displayMedium = walletStyle(45, 52, 800, -0.02),
        displaySmall = walletStyle(36, 44, 800, -0.02),
        headlineLarge = walletStyle(32, 40, 800, -0.02),
        headlineMedium = walletStyle(28, 36, 800, -0.02),
        headlineSmall = walletStyle(24, 32, 800, -0.02),
        titleLarge = walletStyle(22, 28, 800, -0.01),
        titleMedium = walletStyle(16, 24, 700, 0.0),
        titleSmall = walletStyle(14, 20, 700, 0.0),
        bodyLarge = walletStyle(16, 24, 500, 0.0),
        bodyMedium = walletStyle(14, 20, 500, 0.0),
        bodySmall = walletStyle(12, 16, 500, 0.0),
        labelLarge = walletStyle(14, 20, 700, 0.01),
        labelMedium = walletStyle(12, 16, 700, 0.01),
        labelSmall = walletStyle(11, 16, 700, 0.01),
    )
