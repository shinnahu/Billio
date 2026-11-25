package app.billio.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.billio.Language
import app.billio.MainActivity
import app.billio.R
import app.billio.domain.Money
import app.billio.domain.Reminder
import app.billio.domain.ReminderKind

object Notifier {
    const val EXTRA_BILL = "bill"
    const val EXTRA_PAY = "pay"
    private const val CHANNEL = "reminders"
    private const val TEST_ID = 1

    /** Reminders are written in the language picked in Settings, like the screens. */
    private fun words(context: Context): Context = Language.wrap(context, Language.chosen(context))

    /** Creates the channel, or renames it when the app language changed. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, words(context).getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun allowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** One notification per bill; a newer one for the same bill replaces the older one. */
    @SuppressLint("MissingPermission")
    fun post(context: Context, reminders: List<Reminder>, home: String) {
        if (reminders.isEmpty() || !allowed(context)) return
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        val words = words(context)
        for (r in reminders) {
            val title = words.getString(
                when (r.kind) {
                    ReminderKind.MONTHLY -> R.string.notif_monthly
                    ReminderKind.DUE_TOMORROW -> R.string.notif_due_tomorrow
                    ReminderKind.DUE_TODAY -> R.string.notif_due_today
                    ReminderKind.OVERDUE -> R.string.notif_overdue
                },
                r.bill.name,
            )
            val text = r.amountCents
                ?.let { words.getString(R.string.notif_amount, Money.format(it, r.bill.currency, home)) }
                ?: words.getString(R.string.notif_no_amount)
            val id = r.bill.id.hashCode()
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_bill)
                .setColor(context.getColor(R.color.brand))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(open(context, r.bill.id, pay = false, requestCode = id))
                .addAction(0, words.getString(R.string.action_log_payment), open(context, r.bill.id, pay = true, requestCode = id + 1))
                .build()
            manager.notify(id, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun test(context: Context) {
        if (!allowed(context)) return
        ensureChannel(context)
        val words = words(context)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bill)
            .setColor(context.getColor(R.color.brand))
            .setContentTitle(words.getString(R.string.notif_test_title))
            .setContentText(words.getString(R.string.notif_test_text))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, TEST_ID, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        NotificationManagerCompat.from(context).notify(TEST_ID, notification)
    }

    private fun open(context: Context, billId: String, pay: Boolean, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_BILL, billId)
            .putExtra(EXTRA_PAY, pay)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
