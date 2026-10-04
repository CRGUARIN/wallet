package com.crguarin.wallet.ui.recurring

import android.app.Application
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es")
class RecurringScheduleTextTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val anchor = LocalDate.of(2026, 9, 12)

    private fun textFor(
        locale: Locale,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
    ): String {
        val configuration =
            Configuration(app.resources.configuration).apply { setLocale(locale) }
        val resources = app.createConfigurationContext(configuration).resources
        return scheduleText(resources, frequency, interval, anchor, locale)
    }

    @Test
    fun `monthly in spanish names the anchor day`() {
        assertThat(textFor(Locale.forLanguageTag("es"), RecurrenceFrequency.MONTHLY))
            .isEqualTo("Cada mes · día 12")
    }

    @Test
    fun `yearly in spanish takes the connector from the resources`() {
        assertThat(textFor(Locale.forLanguageTag("es"), RecurrenceFrequency.YEARLY))
            .isEqualTo("Cada año · el 12 de septiembre")
    }

    @Test
    fun `monthly in english names the anchor day`() {
        assertThat(textFor(Locale.ENGLISH, RecurrenceFrequency.MONTHLY))
            .isEqualTo("Every month · day 12")
    }

    @Test
    fun `yearly in english puts the month before the day`() {
        assertThat(textFor(Locale.ENGLISH, RecurrenceFrequency.YEARLY))
            .isEqualTo("Every year · on September 12")
    }

    @Test
    fun `interval uses the unit plural`() {
        assertThat(textFor(Locale.forLanguageTag("es"), RecurrenceFrequency.MONTHLY, interval = 2))
            .isEqualTo("Cada 2 meses · día 12")
    }
}
