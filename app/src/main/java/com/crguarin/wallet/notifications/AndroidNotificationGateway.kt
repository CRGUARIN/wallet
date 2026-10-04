package com.crguarin.wallet.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.crguarin.wallet.MainActivity
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.repository.NotificationGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [NotificationGateway].
 *
 * All visible texts come from string resources resolved in the APP locale (the one
 * chosen in Settings, or the system locale when the app follows the system).
 * Silent no-op when notifications are disabled or the API 33+ permission is missing;
 * posting still goes through [NotificationManagerCompat], which needs no
 * MissingPermission suppression.
 */
@Singleton
class AndroidNotificationGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val localeSource: AppLocaleSource,
    ) : NotificationGateway {
        override fun show(events: List<NotificationEvent>) {
            if (events.isEmpty() || !areNotificationsAllowed()) return
            val localized = localizedContext()
            NotificationChannels.createAll(localized.context)
            val manager = NotificationManagerCompat.from(context)
            events.forEach { event ->
                val content = contentFor(localized, event) ?: return@forEach
                try {
                    manager.notify(event.key.hashCode(), buildNotification(localized, event.key, content).build())
                } catch (_: SecurityException) {
                    // Revoked between the pre-check and the post: stay silent.
                }
            }
        }

        override fun showTest() {
            if (!areNotificationsAllowed()) return
            val localized = localizedContext()
            NotificationChannels.createAll(localized.context)
            val content =
                NotificationContent(
                    channel = NotificationChannels.RECURRING,
                    title = localized.context.getString(R.string.notif_test_title),
                    text = localized.context.getString(R.string.notif_test_text),
                    deepLink = DEEP_LINK_RECURRING.toUri(),
                )
            try {
                NotificationManagerCompat
                    .from(context)
                    .notify(TEST_KEY.hashCode(), buildNotification(localized, TEST_KEY, content).build())
            } catch (_: SecurityException) {
                // Revoked between the pre-check and the post: stay silent.
            }
        }

        private data class NotificationContent(
            val channel: String,
            val title: String,
            val text: String,
            val deepLink: Uri,
            val inboxLines: List<String>? = null,
        )

        private fun contentFor(
            res: Context,
            locale: Locale,
            event: NotificationEvent,
        ): NotificationContent? =
            when (event) {
                is NotificationEvent.RecurringUpcoming -> {
                    val amount = MoneyFormatter.format(event.amount, locale)
                    val date = event.date.formatMedium(locale)
                    val text =
                        if (event.type == TransactionType.EXPENSE) {
                            res.getString(R.string.notif_recurring_upcoming_expense, date, event.accountName)
                        } else {
                            res.getString(R.string.notif_recurring_upcoming_income, date, event.accountName)
                        }
                    NotificationContent(
                        channel = NotificationChannels.RECURRING,
                        title = res.getString(R.string.notif_recurring_upcoming_title, event.name, amount),
                        text = text,
                        deepLink = deepLinkUri(event),
                    )
                }
                is NotificationEvent.RecurringPending -> {
                    val amount = MoneyFormatter.format(event.amount, locale)
                    val title =
                        if (event.type == TransactionType.EXPENSE) {
                            res.getString(R.string.notif_recurring_pending_expense, event.name)
                        } else {
                            res.getString(R.string.notif_recurring_pending_income, event.name)
                        }
                    NotificationContent(
                        channel = NotificationChannels.RECURRING,
                        title = title,
                        text = res.getString(R.string.notif_recurring_pending_text, amount),
                        deepLink = deepLinkUri(event),
                    )
                }
                is NotificationEvent.RecurringPosted -> {
                    if (event.occurrences.isEmpty()) return null
                    val lines =
                        event.occurrences.map { occurrence ->
                            val amount = MoneyFormatter.format(occurrence.amount, locale)
                            val title = res.getString(R.string.notif_recurring_upcoming_title, occurrence.name, amount)
                            res.getString(R.string.notif_recurring_posted_text, title, occurrence.accountName)
                        }
                    if (event.occurrences.size == 1) {
                        val occurrence = event.occurrences.first()
                        NotificationContent(
                            channel = NotificationChannels.RECURRING,
                            title = res.getString(R.string.notif_recurring_posted_title, occurrence.name),
                            text = lines.first(),
                            deepLink = deepLinkUri(event),
                        )
                    } else {
                        NotificationContent(
                            channel = NotificationChannels.RECURRING,
                            title =
                                res.resources.getQuantityString(
                                    R.plurals.notif_recurring_posted_many,
                                    event.occurrences.size,
                                    event.occurrences.size,
                                ),
                            text = lines.first(),
                            deepLink = deepLinkUri(event),
                            inboxLines = lines,
                        )
                    }
                }
                is NotificationEvent.CardClosing -> {
                    val title =
                        if (event.daysUntil <= 0) {
                            res.getString(R.string.notif_card_closing_today, event.cardName)
                        } else {
                            res.resources.getQuantityString(
                                R.plurals.notif_card_closing_days,
                                event.daysUntil,
                                event.daysUntil,
                                event.cardName,
                            )
                        }
                    NotificationContent(
                        channel = NotificationChannels.CARDS,
                        title = title,
                        text = res.getString(R.string.notif_card_closing_text),
                        deepLink = deepLinkUri(event),
                    )
                }
                is NotificationEvent.CardPaymentDue -> {
                    val title =
                        if (event.daysUntil <= 0) {
                            res.getString(R.string.notif_card_payment_today, event.cardName)
                        } else {
                            res.resources.getQuantityString(
                                R.plurals.notif_card_payment_days,
                                event.daysUntil,
                                event.daysUntil,
                                event.cardName,
                            )
                        }
                    val amount = event.amount
                    val text =
                        if (amount != null) {
                            res.getString(R.string.notif_card_payment_amount, MoneyFormatter.format(amount, locale))
                        } else {
                            res.getString(R.string.notif_card_payment_unknown)
                        }
                    NotificationContent(
                        channel = NotificationChannels.CARDS,
                        title = title,
                        text = text,
                        deepLink = deepLinkUri(event),
                    )
                }
                is NotificationEvent.CardOverdue -> {
                    NotificationContent(
                        channel = NotificationChannels.CARDS,
                        title = res.getString(R.string.notif_card_overdue_title, event.cardName),
                        text =
                            res.getString(
                                R.string.notif_card_overdue_text,
                                MoneyFormatter.format(event.amount, locale),
                            ),
                        deepLink = deepLinkUri(event),
                    )
                }
            }

        private fun contentFor(
            localized: LocalizedStrings,
            event: NotificationEvent,
        ): NotificationContent? = contentFor(localized.context, localized.locale, event)

        private fun buildNotification(
            localized: LocalizedStrings,
            key: String,
            content: NotificationContent,
        ): NotificationCompat.Builder {
            val id = key.hashCode()
            val builder =
                NotificationCompat
                    .Builder(localized.context, content.channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(content.title)
                    .setContentText(content.text)
                    .setContentIntent(contentIntent(id, content.deepLink))
                    .setAutoCancel(true)
            val lines = content.inboxLines
            if (lines != null) {
                val style = NotificationCompat.InboxStyle().setBigContentTitle(content.title)
                lines.forEach { line -> style.addLine(line) }
                builder.setStyle(style)
            }
            return builder
        }

        private fun contentIntent(
            id: Int,
            deepLink: Uri,
        ): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = deepLink
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            return PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun areNotificationsAllowed(): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
            return true
        }

        private data class LocalizedStrings(
            val context: Context,
            val locale: Locale,
        )

        private fun localizedContext(): LocalizedStrings {
            val appLocales = localeSource.locales()
            val locale =
                if (appLocales.isEmpty) {
                    Locale.getDefault()
                } else {
                    appLocales[0] ?: Locale.getDefault()
                }
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return LocalizedStrings(context.createConfigurationContext(config), locale)
        }

        companion object {
            const val DEEP_LINK_RECURRING = "wallet://recurring"
            const val DEEP_LINK_ACCOUNT_BASE = "wallet://account"

            internal const val TEST_KEY = "wallet_test_notification"

            fun accountDeepLink(accountId: Long): Uri = "$DEEP_LINK_ACCOUNT_BASE/$accountId".toUri()

            internal fun deepLinkUri(event: NotificationEvent): Uri =
                when (event) {
                    is NotificationEvent.CardClosing -> accountDeepLink(event.accountId)
                    is NotificationEvent.CardPaymentDue -> accountDeepLink(event.accountId)
                    is NotificationEvent.CardOverdue -> accountDeepLink(event.accountId)
                    else -> DEEP_LINK_RECURRING.toUri()
                }

            private fun LocalDate.formatMedium(locale: Locale): String =
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(this)
        }
    }
