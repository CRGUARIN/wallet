package com.crguarin.wallet.ui.recurring

import android.content.res.Resources
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Builds the schedule line shown under each active payment
 * ("Cada mes · día 12"), combining the `recurring_every_*` and
 * `recurring_schedule_*` strings with the weekday phrases from
 * resources and java.time month names in the current locale.
 */
fun scheduleText(
    resources: Resources,
    frequency: RecurrenceFrequency,
    interval: Int,
    startDate: LocalDate,
    locale: Locale = Locale.getDefault(),
): String {
    val every =
        when (frequency) {
            RecurrenceFrequency.WEEKLY ->
                resources.getQuantityString(R.plurals.recurring_every_weeks, interval, interval)
            RecurrenceFrequency.MONTHLY ->
                resources.getQuantityString(R.plurals.recurring_every_months, interval, interval)
            RecurrenceFrequency.YEARLY ->
                resources.getQuantityString(R.plurals.recurring_every_years, interval, interval)
        }
    return when (frequency) {
        RecurrenceFrequency.WEEKLY ->
            resources.getString(
                R.string.recurring_schedule_weekly,
                every,
                resources.getStringArray(R.array.recurring_schedule_weekdays)[startDate.dayOfWeek.value - 1],
            )
        RecurrenceFrequency.MONTHLY ->
            resources.getString(
                R.string.recurring_schedule_monthly,
                every,
                startDate.dayOfMonth,
            )
        RecurrenceFrequency.YEARLY ->
            resources.getString(
                R.string.recurring_schedule_yearly,
                every,
                startDate.dayOfMonth,
                startDate.month.getDisplayName(TextStyle.FULL, locale),
            )
    }
}
