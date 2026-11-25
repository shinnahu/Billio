package app.billio

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The app language picked in Settings: "en" or "es". Empty means nobody picked one, and the phone's
 * language decides, the way Android does it for every app.
 */
object Language {
    const val ENGLISH = "en"
    const val SPANISH = "es"

    /** What is saved in Settings. */
    fun chosen(context: Context): String =
        (context.applicationContext as? BillioApp)?.store?.data?.value?.settings?.language.orEmpty()

    /** The language the texts of [context] really come out in: "en" or "es". */
    fun shown(context: Context): String = context.getString(R.string.language_code)

    /** A context whose texts are in [language]. The same context when no language was picked. */
    fun wrap(context: Context, language: String): Context {
        if (language != ENGLISH && language != SPANISH) return context
        val config = Configuration()
        // Only the picked language, without the phone's other languages behind it.
        config.setLocale(locale(context, language))
        return context.createConfigurationContext(config)
    }

    /** The phone's own variant of the language when it has one (es-AR), otherwise the language with the phone's region. */
    private fun locale(context: Context, language: String): Locale {
        val phone = context.resources.configuration.locales
        for (i in 0 until phone.size()) if (phone[i].language == language) return phone[i]
        val region = if (phone.isEmpty) "" else phone[0].country
        // The region decides details such as the first day of the week in the calendar.
        return runCatching { Locale.Builder().setLanguage(language).setRegion(region).build() }
            .getOrElse { Locale.forLanguageTag(language) }
    }
}
