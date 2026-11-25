package app.billio.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.billio.BillioApp
import app.billio.domain.Money
import app.billio.domain.Reminders
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * One alarm a day, at the hour chosen in Settings. When it rings, the day's reminders are shown and the
 * alarm is set again for the next day. It is not an exact alarm, so Android may deliver it a few minutes late.
 */
object ReminderScheduler {
    private const val PREFS = "reminders"
    private const val LAST_RUN = "lastRun"

    fun schedule(context: Context, hour: Int, now: ZonedDateTime = ZonedDateTime.now()) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        var at = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending(context))
    }

    /** Shows what is due today, once a day however many times it is called. */
    fun run(context: Context, today: LocalDate = LocalDate.now()) {
        val app = context.applicationContext as? BillioApp ?: return
        val data = app.store.data.value
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getString(LAST_RUN, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val home = Money.currencyOf(data.settings.country.ifEmpty { "AR" })
        Notifier.post(context, Reminders.since(last, today, data), home)
        prefs.edit().putString(LAST_RUN, today.toString()).apply()
    }

    /** After a restart: if today's hour already passed while the phone was off, show today's reminders now. */
    fun catchUp(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val app = context.applicationContext as? BillioApp ?: return
        if (now.hour >= app.store.data.value.settings.reminderHour) run(context, now.toLocalDate())
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.run(context)
        val app = context.applicationContext as? BillioApp ?: return
        ReminderScheduler.schedule(context, app.store.data.value.settings.reminderHour)
    }
}

/** Alarms are lost when the phone restarts or its clock changes, so they are set again here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? BillioApp ?: return
        ReminderScheduler.schedule(context, app.store.data.value.settings.reminderHour)
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.catchUp(context)
    }
}
