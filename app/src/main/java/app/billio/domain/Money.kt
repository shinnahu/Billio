package app.billio.domain

import kotlin.math.abs

/** Amounts are kept as whole cents so nothing is lost to rounding. */
object Money {

    fun currencyOf(country: String): String = if (country == "US") "USD" else "ARS"

    /** Thousands mark and decimal mark, as each country writes them. */
    private fun marks(currency: String): Pair<Char, Char> = if (currency == "USD") ',' to '.' else '.' to ','

    /** "$" for the user's own currency; the other one is spelled out so the two never look alike. */
    fun symbol(currency: String, home: String = currency): String = when {
        currency == home -> "$"
        currency == "USD" -> "US$"
        currency == "ARS" -> "AR$"
        else -> "$currency "
    }

    /** "$1,450" and ".05", split so the screen can dim the cents. */
    fun parts(cents: Long, currency: String, home: String = currency): Pair<String, String> {
        val (group, decimal) = marks(currency)
        val value = abs(cents)
        val whole = (value / 100).toString().reversed().chunked(3).joinToString(group.toString()).reversed()
        val sign = if (cents < 0) "-" else ""
        return "$sign${symbol(currency, home)}$whole" to "$decimal${(value % 100).toString().padStart(2, '0')}"
    }

    fun format(cents: Long, currency: String, home: String = currency): String =
        parts(cents, currency, home).let { it.first + it.second }

    /** Without the cents when they are zero: "$65", "$45.230,10". */
    fun short(cents: Long, currency: String, home: String = currency): String =
        parts(cents, currency, home).let { if (abs(cents) % 100 == 0L) it.first else it.first + it.second }

    /** Text for an input box, no symbol: "45230,10". */
    fun editable(cents: Long, currency: String): String {
        val decimal = marks(currency).second
        val value = abs(cents)
        return if (value % 100 == 0L) "${value / 100}" else "${value / 100}$decimal${(value % 100).toString().padStart(2, '0')}"
    }

    /**
     * Reads what a person typed or what a page shows: "45.230,10", "$1,450.05", "45230", "45,5".
     * Returns cents, or null when the text is not an amount.
     */
    fun parse(text: String, currency: String): Long? {
        val s = text.trim().filter { it.isDigit() || it == '.' || it == ',' }.trimEnd('.', ',')
        if (s.none(Char::isDigit) || s.first() == '.' || s.first() == ',') return null
        val dot = s.lastIndexOf('.')
        val comma = s.lastIndexOf(',')
        val decimalAt: Int = when {
            dot < 0 && comma < 0 -> -1
            dot >= 0 && comma >= 0 -> maxOf(dot, comma) // both present: the last one marks the decimals
            else -> {
                val at = maxOf(dot, comma)
                val mark = s[at]
                val tail = s.length - at - 1
                when {
                    s.count { it == mark } > 1 -> -1 // 1.234.567: thousands
                    tail == 3 -> -1 // 45.230 or 1,450: thousands, money never has three decimals
                    tail in 1..2 -> at
                    else -> return null
                }
            }
        }
        val wholeText = (if (decimalAt < 0) s else s.substring(0, decimalAt)).filter(Char::isDigit)
        val fracText = if (decimalAt < 0) "" else s.substring(decimalAt + 1).filter(Char::isDigit)
        if (wholeText.isEmpty() || wholeText.length > 13 || fracText.length > 2) return null
        return wholeText.toLong() * 100 + fracText.padEnd(2, '0').toLong()
    }
}
