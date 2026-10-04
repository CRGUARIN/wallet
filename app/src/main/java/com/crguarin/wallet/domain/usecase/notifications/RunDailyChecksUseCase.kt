package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.model.recurring.PostedOccurrence
import com.crguarin.wallet.domain.repository.NotificationGateway
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Daily background check, in order:
 * 1. process due recurring payments;
 * 2. read settings and the last run date;
 * 3. compute events;
 * 4. append [NotificationEvent.RecurringPosted] when charges were recorded;
 * 5. show when notifications are enabled;
 * 6. always store today as the last run date.
 *
 * Every step is isolated with `runCatching`: a failure never throws and the
 * following steps still run.
 */
class RunDailyChecksUseCase
    @Inject
    constructor(
        private val processDueRecurringUseCase: ProcessDueRecurringUseCase,
        private val preferencesRepository: PreferencesRepository,
        private val computeNotificationEventsUseCase: ComputeNotificationEventsUseCase,
        private val notificationGateway: NotificationGateway,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke() {
            val today = timeProvider.today()
            val posted = runCatching { processDueRecurringUseCase() }.getOrDefault(emptyList())
            val settings =
                runCatching { preferencesRepository.getNotificationSettings() }
                    .getOrDefault(NotificationSettings())
            val lastRun = runCatching { preferencesRepository.getLastNotificationRunDate() }.getOrNull()
            val computed =
                runCatching { computeNotificationEventsUseCase(lastRun, today, settings) }
                    .getOrDefault(emptyList())
            val events =
                computed + runCatching { postedEvents(posted, today, settings) }.getOrDefault(emptyList())
            if (settings.enabled) {
                runCatching { notificationGateway.show(events) }
            }
            runCatching { preferencesRepository.setLastNotificationRunDate(today) }
        }

        private fun postedEvents(
            posted: List<PostedOccurrence>,
            today: LocalDate,
            settings: NotificationSettings,
        ): List<NotificationEvent> {
            if (posted.isEmpty() || !settings.recurringPosted) return emptyList()
            if (posted.size <= POSTED_INDIVIDUAL_LIMIT) {
                return posted.map { occurrence ->
                    NotificationEvent.RecurringPosted(
                        key = "rec-posted-${occurrence.transactionId}",
                        occurrences = listOf(occurrence),
                    )
                }
            }
            return listOf(
                NotificationEvent.RecurringPosted(
                    key = "rec-posted-grouped-$today",
                    occurrences = posted,
                ),
            )
        }

        companion object {
            const val POSTED_INDIVIDUAL_LIMIT = 3
        }
    }
