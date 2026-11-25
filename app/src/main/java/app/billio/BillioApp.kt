package app.billio

import android.app.Application
import app.billio.data.Store
import app.billio.notify.Notifier
import app.billio.notify.ReminderScheduler
import java.io.File

class BillioApp : Application() {

    /** The single copy of the user's data, shared by the screens and the reminder alarm. */
    val store: Store by lazy { Store(File(filesDir, "bills.json")) }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        ReminderScheduler.schedule(this, store.data.value.settings.reminderHour)
    }
}
