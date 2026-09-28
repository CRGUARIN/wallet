package com.crguarin.wallet.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats a [LocalDate] with medium localized date style according to [locale].
 */
fun LocalDate.formatMedium(locale: Locale = Locale.getDefault()): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return format(formatter)
}

/**
 * Composable helper that formats [LocalDate] using the current Compose configuration locale.
 */
@Composable
fun LocalDate.formatMedium(): String {
    val locale = LocalConfiguration.current.locales[0]
    return formatMedium(locale)
}
