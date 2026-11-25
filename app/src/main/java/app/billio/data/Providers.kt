package app.billio.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** A real company from the bundled catalog (assets/providers.json). */
@Serializable
data class Provider(
    val id: String,
    val name: String,
    val category: Category,
    /** "AR", "US", or "*" for services billed the same way everywhere. */
    val country: String,
    /** What the bill calls the number that identifies the account. Empty when not known. */
    val idLabel: String = "",
    val idHint: String = "",
    /** Official page where the bill can be seen or paid. Empty when not checked. */
    val url: String = "",
    /**
     * What was seen on that page when the catalog was put together: "login" when it asks to sign in,
     * "open" when it offers a way in with only the account number. Empty when it was not checked.
     */
    val access: String = "",
    val aliases: List<String> = emptyList(),
)

@Serializable
data class Catalog(val updated: String = "", val providers: List<Provider> = emptyList()) {

    fun inCountry(country: String): List<Provider> = providers.filter { it.country == country || it.country == "*" }

    fun categories(country: String): List<Category> =
        Category.entries.filter { c -> c != Category.OTHER && inCountry(country).any { it.category == c } }

    fun of(country: String, category: Category): List<Provider> =
        inCountry(country).filter { it.category == category }.sortedBy { it.name.lowercase() }

    fun search(country: String, query: String): List<Provider> {
        val q = fold(query)
        if (q.isBlank()) return emptyList()
        return inCountry(country)
            .filter { p -> fold(p.name).contains(q) || p.aliases.any { fold(it).contains(q) } }
            .sortedBy { it.name.lowercase() }
    }

    fun byId(id: String?): Provider? = providers.firstOrNull { it.id == id }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Catalog = json.decodeFromString(text)

        @Volatile
        private var cached: Catalog? = null

        fun load(context: Context): Catalog = cached ?: synchronized(this) {
            cached ?: runCatching {
                parse(context.assets.open("providers.json").bufferedReader().use { it.readText() })
            }.getOrDefault(Catalog()).also { cached = it }
        }

        /** Lower case without accents, so "cordoba" finds "Córdoba". */
        fun fold(s: String): String =
            Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    }
}
