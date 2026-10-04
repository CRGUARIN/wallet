package com.crguarin.wallet.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.MainActivity
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.recurring.PostedOccurrence
import com.crguarin.wallet.notifications.AndroidNotificationGateway.Companion.accountDeepLink
import com.crguarin.wallet.notifications.AndroidNotificationGateway.Companion.deepLinkUri
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidNotificationGatewayTest {
    private lateinit var context: Context
    private lateinit var gateway: AndroidNotificationGateway

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Robolectric denies runtime permissions by default; the gateway pre-checks
        // POST_NOTIFICATIONS on API 33+, so grant it except where a test denies it.
        Shadows
            .shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        gateway = withSystemLocales()
    }

    @After
    fun tearDown() {
        notificationManager().cancelAll()
        Shadows.shadowOf(notificationManager()).setNotificationsEnabled(true)
    }

    @Test
    fun `recurring upcoming expense uses spanish texts and recurring channel`() {
        useAppLocale("es")
        val date = LocalDate.of(2026, 10, 12)
        val event = upcoming(date = date, type = TransactionType.EXPENSE)

        gateway.show(listOf(event))

        val posted = singlePosted(event)
        val es = locale("es")
        assertThat(posted.channelId).isEqualTo(NotificationChannels.RECURRING)
        assertThat(titleOf(posted)).isEqualTo("Netflix · ${MoneyFormatter.format(Money(1_590_000), es)}")
        assertThat(textOf(posted)).isEqualTo(
            "Próximo cobro el ${date.formatMedium(es)} desde Ahorros",
        )
    }

    @Test
    fun `recurring upcoming income uses english texts when app locale is english`() {
        useAppLocale("en")
        val date = LocalDate.of(2026, 10, 12)
        val event = upcoming(date = date, type = TransactionType.INCOME)

        gateway.show(listOf(event))

        val posted = singlePosted(event)
        val en = locale("en")
        assertThat(posted.channelId).isEqualTo(NotificationChannels.RECURRING)
        assertThat(textOf(posted)).isEqualTo("Next income on ${date.formatMedium(en)} to Ahorros")
    }

    @Test
    fun `recurring pending asks the question with the amount`() {
        useAppLocale("es")
        val event =
            NotificationEvent.RecurringPending(
                key = "rec-pend-1-2026-10-04",
                paymentId = 1,
                name = "Netflix",
                type = TransactionType.EXPENSE,
                amount = Money(1_590_000),
                date = LocalDate.of(2026, 10, 4),
            )

        gateway.show(listOf(event))

        val posted = singlePosted(event)
        assertThat(titleOf(posted)).isEqualTo("¿Ya pagaste Netflix?")
        assertThat(textOf(posted)).contains("Toca para confirmar el valor")
    }

    @Test
    fun `recurring pending income asks about receiving`() {
        useAppLocale("es")
        val event =
            NotificationEvent.RecurringPending(
                key = "rec-pend-2-2026-10-04",
                paymentId = 2,
                name = "Salario",
                type = TransactionType.INCOME,
                amount = Money(50_000_000),
                date = LocalDate.of(2026, 10, 4),
            )

        gateway.show(listOf(event))

        assertThat(titleOf(singlePosted(event))).isEqualTo("¿Recibiste Salario?")
    }

    @Test
    fun `single posted occurrence notifies title and account`() {
        useAppLocale("es")
        val event =
            NotificationEvent.RecurringPosted(
                key = "rec-posted-1-2026-10-04",
                occurrences =
                    listOf(
                        postedOccurrence(name = "Netflix", amount = Money(1_590_000), account = "Ahorros"),
                    ),
            )

        gateway.show(listOf(event))

        val posted = singlePosted(event)
        assertThat(titleOf(posted)).isEqualTo("Se registró Netflix")
        assertThat(textOf(posted)).contains("Ahorros")
    }

    @Test
    fun `several posted occurrences group with one inbox line each`() {
        useAppLocale("es")
        val event =
            NotificationEvent.RecurringPosted(
                key = "rec-posted-many-2026-10-04",
                occurrences =
                    listOf(
                        postedOccurrence(name = "Netflix", amount = Money(1_590_000), account = "Ahorros"),
                        postedOccurrence(name = "Spotify", amount = Money(999_000), account = "Nequi"),
                        postedOccurrence(name = "Gimnasio", amount = Money(8_990_000), account = "Ahorros"),
                    ),
            )

        gateway.show(listOf(event))

        val posted = singlePosted(event)
        assertThat(titleOf(posted)).isEqualTo("Se registraron 3 pagos recurrentes")
        val lines = posted.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        assertThat(lines).hasLength(3)
        val texts = lines!!.map { it.toString() }
        assertThat(texts[0]).contains("Netflix")
        assertThat(texts[1]).contains("Spotify")
        assertThat(texts[2]).contains("Gimnasio")
    }

    @Test
    fun `card closing today and in days`() {
        useAppLocale("es")
        val today =
            NotificationEvent.CardClosing(
                key = "card-close-7-2026-10-04",
                accountId = 7,
                cardName = "Visa",
                closingDate = LocalDate.of(2026, 10, 4),
                daysUntil = 0,
            )
        val ahead = today.copy(key = "card-close-7-2026-10-02", daysUntil = 2)

        gateway.show(listOf(today, ahead))

        assertThat(titleOf(posted(today))).isEqualTo("Hoy es el corte de Visa")
        assertThat(titleOf(posted(ahead))).isEqualTo("Visa corta en 2 días")
        assertThat(textOf(posted(ahead))).isEqualTo("Cuando llegue el extracto, concílialo en la app.")
        assertThat(posted(today).channelId).isEqualTo(NotificationChannels.CARDS)
    }

    @Test
    fun `card payment due today shows amount or reconcile hint`() {
        useAppLocale("es")
        val withAmount =
            NotificationEvent.CardPaymentDue(
                key = "card-due-7-2026-10-04",
                accountId = 7,
                cardName = "Visa",
                dueDate = LocalDate.of(2026, 10, 4),
                daysUntil = 0,
                amount = Money(45_900_000),
            )
        val unknown =
            NotificationEvent.CardPaymentDue(
                key = "card-due-8-2026-10-04",
                accountId = 8,
                cardName = "Master",
                dueDate = LocalDate.of(2026, 10, 4),
                daysUntil = 3,
                amount = null,
            )

        gateway.show(listOf(withAmount, unknown))

        assertThat(titleOf(posted(withAmount))).isEqualTo("Hoy vence el pago de Visa")
        assertThat(textOf(posted(withAmount))).contains("Por pagar:")
        assertThat(titleOf(posted(unknown))).isEqualTo("El pago de Master vence en 3 días")
        assertThat(textOf(posted(unknown))).isEqualTo("Concilia el extracto para saber cuánto pagar.")
    }

    @Test
    fun `card overdue shows pending amount`() {
        useAppLocale("es")
        val event =
            NotificationEvent.CardOverdue(
                key = "card-over-7-2026-10-05",
                accountId = 7,
                cardName = "Visa",
                dueDate = LocalDate.of(2026, 10, 4),
                amount = Money(45_900_000),
            )

        gateway.show(listOf(event))

        assertThat(titleOf(posted(event))).isEqualTo("El pago de Visa está vencido")
        assertThat(textOf(posted(event))).contains("Pendiente:")
    }

    @Test
    fun `test notification lands on the recurring channel`() {
        useAppLocale("es")

        gateway.showTest()

        val all = Shadows.shadowOf(notificationManager()).allNotifications
        assertThat(all).hasSize(1)
        assertThat(all.single().channelId).isEqualTo(NotificationChannels.RECURRING)
        assertThat(titleOf(all.single())).isEqualTo("Las notificaciones funcionan")
    }

    @Test
    fun `blocked notifications publish nothing`() {
        useAppLocale("es")
        Shadows.shadowOf(notificationManager()).setNotificationsEnabled(false)

        gateway.show(listOf(upcoming()))
        gateway.showTest()

        assertThat(Shadows.shadowOf(notificationManager()).allNotifications).isEmpty()
    }

    @Test
    fun `missing post-notifications permission publishes nothing`() {
        useAppLocale("es")
        Shadows
            .shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        gateway.show(listOf(upcoming()))

        assertThat(Shadows.shadowOf(notificationManager()).allNotifications).isEmpty()
    }

    @Test
    fun `repeating the same key replaces instead of duplicating`() {
        useAppLocale("es")
        val event = upcoming()

        gateway.show(listOf(event))
        gateway.show(listOf(event))

        assertThat(Shadows.shadowOf(notificationManager()).allNotifications).hasSize(1)
    }

    @Test
    fun `deep links point to recurring list and account detail`() {
        assertThat(deepLinkUri(upcoming()).toString()).isEqualTo("wallet://recurring")
        assertThat(
            deepLinkUri(
                NotificationEvent.CardClosing(
                    key = "k",
                    accountId = 42,
                    cardName = "Visa",
                    closingDate = LocalDate.of(2026, 10, 4),
                    daysUntil = 0,
                ),
            ).toString(),
        ).isEqualTo("wallet://account/42")
        assertThat(accountDeepLink(7).toString()).isEqualTo("wallet://account/7")
    }

    @Test
    fun `posted notifications carry the deep link and flags in their content intent`() {
        useAppLocale("es")
        val recurringEvent = upcoming()
        val cardEvent =
            NotificationEvent.CardClosing(
                key = "card-close-42-2026-10-04",
                accountId = 42,
                cardName = "Visa",
                closingDate = LocalDate.of(2026, 10, 4),
                daysUntil = 0,
            )

        gateway.show(listOf(recurringEvent, cardEvent))

        val recurringIntent = savedIntentOf(posted(recurringEvent))
        assertThat(recurringIntent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(recurringIntent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(recurringIntent.data.toString()).isEqualTo("wallet://recurring")
        assertThat(recurringIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
        assertThat(recurringIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val cardIntent = savedIntentOf(posted(cardEvent))
        assertThat(cardIntent.data.toString()).isEqualTo("wallet://account/42")

        // One PendingIntent per event: the request code is the notification id.
        val recurringPending = posted(recurringEvent).contentIntent
        val cardPending = posted(cardEvent).contentIntent
        assertThat(Shadows.shadowOf(recurringPending).requestCode).isEqualTo(recurringEvent.key.hashCode())
        assertThat(Shadows.shadowOf(cardPending).requestCode).isEqualTo(cardEvent.key.hashCode())
        assertThat(Shadows.shadowOf(recurringPending).requestCode)
            .isNotEqualTo(Shadows.shadowOf(cardPending).requestCode)
    }

    @Test
    fun `every event type resolves english texts when the app locale is english`() {
        useAppLocale("en")
        val date = LocalDate.of(2026, 10, 12)
        val pending =
            NotificationEvent.RecurringPending(
                key = "rec-pend-en-1-2026-10-12",
                paymentId = 1,
                name = "Netflix",
                type = TransactionType.EXPENSE,
                amount = Money(1_590_000),
                date = date,
            )
        val postedSingle =
            NotificationEvent.RecurringPosted(
                key = "rec-posted-en-1-2026-10-12",
                occurrences = listOf(postedOccurrence(name = "Netflix", amount = Money(1_590_000), account = "Ahorros")),
            )
        val postedMany =
            NotificationEvent.RecurringPosted(
                key = "rec-posted-en-many-2026-10-12",
                occurrences =
                    listOf(
                        postedOccurrence(name = "Netflix", amount = Money(1_590_000), account = "Ahorros"),
                        postedOccurrence(name = "Spotify", amount = Money(999_000), account = "Nequi"),
                        postedOccurrence(name = "Gimnasio", amount = Money(8_990_000), account = "Ahorros"),
                    ),
            )
        val closing =
            NotificationEvent.CardClosing(
                key = "card-close-en-7-2026-10-12",
                accountId = 7,
                cardName = "Visa",
                closingDate = date,
                daysUntil = 2,
            )
        val dueToday =
            NotificationEvent.CardPaymentDue(
                key = "card-due-en-7-2026-10-12",
                accountId = 7,
                cardName = "Visa",
                dueDate = date,
                daysUntil = 0,
                amount = Money(45_900_000),
            )
        val dueUnknown =
            NotificationEvent.CardPaymentDue(
                key = "card-due-en-8-2026-10-12",
                accountId = 8,
                cardName = "Master",
                dueDate = date,
                daysUntil = 3,
                amount = null,
            )
        val overdue =
            NotificationEvent.CardOverdue(
                key = "card-over-en-7-2026-10-13",
                accountId = 7,
                cardName = "Visa",
                dueDate = date,
                amount = Money(45_900_000),
            )

        gateway.show(listOf(pending, postedSingle, postedMany, closing, dueToday, dueUnknown, overdue))
        gateway.showTest()

        assertThat(titleOf(posted(pending))).isEqualTo("Did you pay Netflix?")
        assertThat(titleOf(posted(postedSingle))).isEqualTo("Netflix recorded")
        assertThat(titleOf(posted(postedMany))).isEqualTo("3 recurring payments recorded")
        assertThat(posted(postedMany).extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)).hasLength(3)
        assertThat(titleOf(posted(closing))).isEqualTo("Visa statement closes in 2 days")
        assertThat(titleOf(posted(dueToday))).isEqualTo("Visa payment is due today")
        assertThat(textOf(posted(dueToday))).contains("Amount due:")
        assertThat(textOf(posted(dueUnknown))).isEqualTo("Reconcile the statement to know how much to pay.")
        assertThat(titleOf(posted(overdue))).isEqualTo("Visa payment is overdue")
        val testNotification =
            Shadows
                .shadowOf(notificationManager())
                .getNotification(AndroidNotificationGateway.TEST_KEY.hashCode())
        assertThat(testNotification).isNotNull()
        assertThat(titleOf(testNotification!!)).isEqualTo("Notifications work")
    }

    @Test
    fun `channels exist with localized names after notifying`() {
        useAppLocale("es")

        gateway.show(listOf(upcoming()))

        val recurring = notificationManager().getNotificationChannel(NotificationChannels.RECURRING)
        val cards = notificationManager().getNotificationChannel(NotificationChannels.CARDS)
        assertThat(recurring).isNotNull()
        assertThat(cards).isNotNull()
        assertThat(recurring.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(cards.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    }

    // Helpers.

    private fun notificationManager(): NotificationManager = context.getSystemService(NotificationManager::class.java)!!

    private fun withSystemLocales(): AndroidNotificationGateway =
        AndroidNotificationGateway(context) { LocaleListCompat.getEmptyLocaleList() }

    private fun useAppLocale(tag: String) {
        gateway = AndroidNotificationGateway(context) { LocaleListCompat.forLanguageTags(tag) }
    }

    private fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    private fun upcoming(
        date: LocalDate = LocalDate.of(2026, 10, 12),
        type: TransactionType = TransactionType.EXPENSE,
    ): NotificationEvent.RecurringUpcoming =
        NotificationEvent.RecurringUpcoming(
            key = "rec-up-1-2026-10-12",
            paymentId = 1,
            name = "Netflix",
            type = type,
            amount = Money(1_590_000),
            date = date,
            daysUntil = 8,
            accountName = "Ahorros",
        )

    private fun postedOccurrence(
        name: String,
        amount: Money,
        account: String,
    ): PostedOccurrence =
        PostedOccurrence(
            paymentId = 1,
            name = name,
            type = TransactionType.EXPENSE,
            transactionId = 10,
            date = LocalDate.of(2026, 10, 4),
            amount = amount,
            accountId = 3,
            accountName = account,
        )

    private fun posted(event: NotificationEvent): Notification {
        val found = Shadows.shadowOf(notificationManager()).getNotification(event.key.hashCode())
        return checkNotNull(found) { "no notification posted for ${event.key}" }
    }

    private fun singlePosted(event: NotificationEvent): Notification {
        val all = Shadows.shadowOf(notificationManager()).allNotifications
        assertThat(all).hasSize(1)
        return posted(event)
    }

    private fun titleOf(notification: Notification): String = notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()

    private fun textOf(notification: Notification): String = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()

    private fun savedIntentOf(notification: Notification): Intent = Shadows.shadowOf(notification.contentIntent).savedIntent

    private fun LocalDate.formatMedium(locale: Locale): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(this)
}
