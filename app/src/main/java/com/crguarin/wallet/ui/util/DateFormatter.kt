package com.crguarin.wallet.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
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

/**
 * Formats a [YearMonth] as a capitalized month name plus year, e.g. "Septiembre 2026" / "September 2026".
 */
fun YearMonth.formatMonthYear(locale: Locale = Locale.getDefault()): String {
    val monthName = month.getDisplayName(TextStyle.FULL, locale)
    val capitalized = monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    return "$capitalized $year"
}

/**
 * Returns the string resource for a relative day label ("Today"/"Yesterday") or null when the date
 * is not one of those and must be formatted as a full date.
 */
@StringRes
fun relativeDayLabelRes(
    date: LocalDate,
    today: LocalDate,
): Int? =
    when (date) {
        today -> R.string.transactions_day_today
        today.minusDays(1) -> R.string.transactions_day_yesterday
        else -> null
    }

/**
 * Composable label for a day header: "Hoy"/"Ayer" when applicable, otherwise the localized date.
 */
@Composable
fun relativeDayLabel(
    date: LocalDate,
    today: LocalDate,
): String {
    val res = relativeDayLabelRes(date, today)
    return if (res != null) stringResource(res) else date.formatMedium()
}
