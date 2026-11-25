package app.billio

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Catalog
import app.billio.data.Category
import app.billio.data.Due
import app.billio.data.Settings
import app.billio.notify.Notifier
import app.billio.notify.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** The parts that talk to Android: the daily alarm, the notifications, the bundled list of companies. */
@RunWith(RobolectricTestRunner::class)
class AndroidGlueTest {
    private val app: BillioApp get() = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    private fun bill(id: String, name: String, day: Int) = Bill(
        id = id, name = name, category = Category.ELECTRICITY, country = "AR", currency = "ARS", reminderDay = day,
    )

    @Test
    fun alarmIsSetForTheNextTimeTheChosenHourComes() {
        val zone = ZoneId.systemDefault()
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))

        ReminderScheduler.schedule(app, 9, ZonedDateTime.of(2026, 11, 25, 7, 30, 0, 0, zone))
        assertEquals(ZonedDateTime.of(2026, 11, 25, 9, 0, 0, 0, zone).toInstant().toEpochMilli(), alarms.peekNextScheduledAlarm()!!.triggerAtTime)

        ReminderScheduler.schedule(app, 9, ZonedDateTime.of(2026, 11, 25, 9, 0, 5, 0, zone))
        assertEquals(ZonedDateTime.of(2026, 11, 26, 9, 0, 0, 0, zone).toInstant().toEpochMilli(), alarms.peekNextScheduledAlarm()!!.triggerAtTime)
        assertEquals(1, alarms.scheduledAlarms.size) // the new alarm replaces the old one
    }

    @Test
    fun theDailyRunShowsOneNotificationPerBillAndOnlyOncePerDay() {
        app.store.update {
            AppData(
                settings = Settings(country = "AR"),
                bills = listOf(bill("edenor", "Edenor", 1), bill("aysa", "AySA", 25), bill("gas", "Naturgy", 12)),
                dues = listOf(Due("edenor", "2026-11", 4523010, "2026-11-25")),
            )
        }
        ReminderScheduler.run(app, LocalDate.of(2026, 11, 25))

        val shown = notifications.allNotifications.map {
            it.extras.getString("android.title") to it.extras.getString("android.text")
        }.toSet()
        assertEquals(
            setOf("Edenor is due today" to "To pay: $45.230,10", "Time to pay AySA" to "Check this month's amount and log the payment."),
            shown,
        )
        val edenor = notifications.allNotifications.first { it.extras.getString("android.title") == "Edenor is due today" }
        assertNotNull(edenor.contentIntent)
        assertEquals("Log payment", edenor.actions.single().title)

        app.getSystemService(NotificationManager::class.java).cancelAll()
        ReminderScheduler.run(app, LocalDate.of(2026, 11, 25))
        assertTrue(notifications.allNotifications.isEmpty())
    }

    @Test
    fun remindersAreWrittenInTheLanguagePickedInSettings() {
        // The simulated phone is in English; Spanish comes only from the choice saved in Settings.
        app.store.update {
            AppData(settings = Settings(country = "AR", language = "es"), bills = listOf(bill("edenor", "Edenor", 25)))
        }
        ReminderScheduler.run(app, LocalDate.of(2026, 11, 25))

        val reminder = notifications.allNotifications.single()
        assertEquals("Es momento de pagar Edenor", reminder.extras.getString("android.title"))
        assertEquals("Consultá el importe de este mes y registrá el pago.", reminder.extras.getString("android.text"))
        assertEquals("Registrar pago", reminder.actions.single().title)
        val channel = app.getSystemService(NotificationManager::class.java).getNotificationChannel(reminder.channelId)
        assertEquals("Recordatorios de facturas", channel.name.toString())
    }

    @Test
    fun aPickedLanguageWinsOverThePhoneLanguage() {
        assertEquals("Settings", Language.wrap(app, "").getString(R.string.settings_title))
        assertEquals("Ajustes", Language.wrap(app, "es").getString(R.string.settings_title))
        assertEquals("Settings", Language.wrap(app, "en").getString(R.string.settings_title))
        assertEquals("es", Language.shown(Language.wrap(app, "es")))
        assertEquals("en", Language.shown(app))
    }

    @Test
    @Config(qualifiers = "es-rAR")
    fun onASpanishPhoneEnglishCanStillBePicked() {
        assertEquals("Ajustes", Language.wrap(app, "").getString(R.string.settings_title))
        assertEquals("Settings", Language.wrap(app, "en").getString(R.string.settings_title))
        assertEquals("Ajustes", Language.wrap(app, "es").getString(R.string.settings_title))
    }

    @Test
    fun aPhoneWithSeveralLanguagesStillGetsTheOnePicked() {
        val phone = Configuration(app.resources.configuration)
        for (languages in listOf("fr-FR,es-AR", "en-US,es-AR", "es-AR,en-US")) {
            phone.setLocales(LocaleList.forLanguageTags(languages))
            val context = app.createConfigurationContext(phone)
            assertEquals(languages, "Settings", Language.wrap(context, "en").getString(R.string.settings_title))
            assertEquals(languages, "Ajustes", Language.wrap(context, "es").getString(R.string.settings_title))
        }
    }

    @Test
    fun tappingAReminderOpensThatBillAndTheLogPaymentButtonOpensTheForm() {
        app.store.update { AppData(bills = listOf(bill("edenor", "Edenor", 1))) }
        val state = AppState(app.store, Catalog.load(app))
        state.openFromNotification("edenor", pay = true)
        assertEquals(listOf<Screen>(Screen.Detail("edenor")), state.stack.toList())
        assertEquals(Sheet.Pay("edenor"), state.sheet)

        state.openFromNotification("deleted-bill", pay = false)
        assertEquals(listOf<Screen>(Screen.Detail("edenor")), state.stack.toList())
        assertEquals(Notifier.EXTRA_BILL, "bill")
    }

    @Test
    fun catalogHasTheCompaniesAskedForAndEveryEntryIsUsable() {
        val catalog = Catalog.load(app as Context)
        val ar = catalog.inCountry("AR")
        val us = catalog.inCountry("US")
        assertTrue(ar.size >= 40 && us.size >= 40)

        fun find(country: String, name: String) = catalog.inCountry(country).first { it.name.startsWith(name) }
        assertEquals(Category.INTERNET, find("AR", "Telecentro").category)
        assertEquals(Category.ELECTRICITY, find("AR", "Edenor").category)
        assertEquals("Naturgy", catalog.byId("ar-naturgy-ban")!!.name)
        assertEquals(Category.GAS, catalog.byId("ar-naturgy-ban")!!.category)
        assertEquals("N° de cuenta", find("AR", "Edenor").idLabel)
        assertEquals("Código de pago online", catalog.byId("ar-naturgy-ban")!!.idLabel)
        assertEquals(Category.INTERNET, find("US", "Xfinity").category)
        assertEquals(Category.ELECTRICITY, find("US", "PG&E").category)
        assertEquals(Category.INSURANCE, find("US", "GEICO").category)
        assertFalse(us.any { it.name == "Edenor" })
        assertTrue(ar.any { it.name == "Netflix" } && us.any { it.name == "Netflix" })

        assertEquals(catalog.providers.size, catalog.providers.map { it.id }.toSet().size)
        catalog.providers.forEach { p ->
            assertTrue(p.id, p.name.isNotBlank() && p.country in setOf("AR", "US", "*"))
            assertTrue(p.id, p.url.isEmpty() || p.url.startsWith("https://"))
            assertTrue(p.id, p.access in setOf("", "login", "open"))
        }
        assertEquals(listOf("EPEC (Córdoba)"), catalog.search("AR", "cordoba").map { it.name }.filter { it.startsWith("EPEC") })
        assertTrue(catalog.search("AR", "fibertel").any { it.id == "ar-personal-flow" })
    }
}
