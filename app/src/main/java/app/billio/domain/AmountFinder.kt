package app.billio.domain

import java.time.LocalDate

data class AmountHit(val cents: Long, val raw: String, val context: String, val score: Int)

data class DateHit(val date: LocalDate, val raw: String, val context: String, val score: Int)

/**
 * Looks for money amounts and due dates in the text of a provider page.
 * It only suggests: the person picks which amount is the bill.
 */
object AmountFinder {

    private val money = Regex(
        """(?<![\w.,/-])(US\$|AR\$|ARS|USD|\$)?[  ]{0,2}(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+[.,]\d{2}|\d+)(?![\w/-]|[.,]\d)""",
    )

    /** Units that follow a number which is not money: consumption, speed, percentages, days. */
    private val units = listOf("%", "kwh", "kw", "m3", "m³", "gb", "mb", "mbps", "megas", "dias", "días", "days", "hs")

    private val amountWords = listOf(
        "total a pagar" to 9, "importe a pagar" to 9, "saldo a pagar" to 9, "monto a pagar" to 9, "total factura" to 8,
        "deuda total" to 8, "saldo actual" to 7, "saldo vencido" to 7, "a pagar" to 6, "saldo" to 5, "total" to 5,
        "importe" to 5, "deuda" to 5, "monto" to 4, "cuota" to 3,
        "amount due" to 9, "total due" to 9, "balance due" to 9, "payment due" to 8, "current balance" to 8,
        "total amount" to 7, "balance" to 5, "amount" to 4, "due" to 3,
    )

    private val dateWords = listOf(
        "2° vencimiento" to 4, "2º vencimiento" to 4, "segundo vencimiento" to 4, "2do vencimiento" to 4,
        "vencimiento" to 9, "vence" to 9, "vto" to 9, "fecha límite" to 8, "fecha limite" to 8, "pagar antes" to 8,
        "hasta el" to 5,
        "due date" to 9, "due by" to 9, "due on" to 9, "pay by" to 8, "due" to 6,
    )

    private val monthsEs = listOf(
        "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
    )
    private val monthsEn = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    )

    private val numericDate = Regex("""(?<!\d)(\d{1,2})[/.-](\d{1,2})[/.-](\d{4}|\d{2})(?!\d)""")
    private val isoDate = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")
    private val dayMonthWords = Regex(
        """(?<!\d)(\d{1,2})(?:\s+de)?[\s/-]+(ene|feb|mar|abr|may|jun|jul|ago|sep|set|oct|nov|dic|jan|apr|aug|dec)[a-zé]*\.?(?:,?(?:\s+de|\s+del)?[\s/-]+(\d{4}))?""",
        RegexOption.IGNORE_CASE,
    )
    private val monthDayWords = Regex(
        """(?<![a-z])(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s+(\d{1,2})(?:st|nd|rd|th)?(?!\d)(?:,?\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE,
    )

    fun amounts(text: String, currency: String, limit: Int = 8): List<AmountHit> {
        val found = LinkedHashMap<Long, AmountHit>()
        val seen = HashMap<Long, Int>()
        var previousEnd = 0
        for (m in money.findAll(text)) {
            val symbol = m.groupValues[1]
            val number = m.groupValues[2]
            val hasDecimals = Regex("""[.,]\d{2}$""").containsMatchIn(number)
            if (symbol.isEmpty() && !hasDecimals) continue
            val after = text.substring(m.range.last + 1, minOf(text.length, m.range.last + 8)).trim().lowercase()
            if (units.any { after.startsWith(it) }) continue
            val cents = Money.parse(number, currency) ?: continue
            if (cents <= 0) continue
            // Words count only back to the previous amount: "Total a pagar" belongs to the number right after it.
            val from = maxOf(previousEnd, m.range.first - 90)
            previousEnd = m.range.last + 1
            val before = clean(text.substring(from, m.range.first)).lowercase()
            var score = wordScore(before, amountWords)
            if (symbol.isNotEmpty()) score += 2
            if (hasDecimals) score += 1
            val context = (label(text, from, m.range.first) + " " + m.value.trim()).trim()
            seen[cents] = (seen[cents] ?: 0) + 1
            val old = found[cents]
            if (old == null || score > old.score) found[cents] = AmountHit(cents, m.value.trim(), context, score)
        }
        return found.values
            .map { if ((seen[it.cents] ?: 0) > 1) it.copy(score = it.score + 1) else it }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun dates(text: String, currency: String, today: LocalDate, limit: Int = 5): List<DateHit> {
        val found = LinkedHashMap<LocalDate, DateHit>()
        fun add(date: LocalDate?, range: IntRange, raw: String) {
            if (date == null || date.isBefore(today.minusDays(90)) || date.isAfter(today.plusDays(200))) return
            val from = maxOf(0, range.first - 70)
            val before = clean(text.substring(from, range.first)).lowercase()
            var score = wordScore(before, dateWords)
            if (!date.isBefore(today)) score += 1
            val context = (label(text, from, range.first) + " " + raw).trim()
            val old = found[date]
            if (old == null || score > old.score) found[date] = DateHit(date, raw, context, score)
        }
        val monthFirst = currency == "USD"
        for (m in numericDate.findAll(text)) {
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            val y = m.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            val (day, month) = when {
                monthFirst && a <= 12 -> b to a
                !monthFirst && b <= 12 -> a to b
                else -> if (a > 12) a to b else b to a
            }
            add(make(y, month, day), m.range, m.value)
        }
        for (m in isoDate.findAll(text)) {
            add(make(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()), m.range, m.value)
        }
        for (m in dayMonthWords.findAll(text)) {
            val month = monthNumber(m.groupValues[2]) ?: continue
            add(withYear(m.groupValues[3], month, m.groupValues[1].toInt(), today), m.range, clean(m.value))
        }
        for (m in monthDayWords.findAll(text)) {
            val month = monthNumber(m.groupValues[1]) ?: continue
            add(withYear(m.groupValues[3], month, m.groupValues[2].toInt(), today), m.range, clean(m.value))
        }
        return found.values.sortedWith(compareByDescending<DateHit> { it.score }.thenBy { it.date }).take(limit)
    }

    private fun monthNumber(word: String): Int? {
        val w = word.lowercase().take(3).let { if (it == "set") "sep" else it }
        val i = monthsEs.indexOf(w).takeIf { it >= 0 } ?: monthsEn.indexOf(w).takeIf { it >= 0 } ?: return null
        return i + 1
    }

    private fun make(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()

    /** A date written without a year means the nearest one: "10 de diciembre" read in November is this year. */
    private fun withYear(year: String, month: Int, day: Int, today: LocalDate): LocalDate? {
        if (year.isNotEmpty()) return make(year.toInt(), month, day)
        return listOf(today.year - 1, today.year, today.year + 1).mapNotNull { make(it, month, day) }
            .minByOrNull { kotlin.math.abs(it.toEpochDay() - today.toEpochDay()) }
    }

    /** The strongest word found; a word right next to the number counts in full, one further away counts half. */
    private fun wordScore(before: String, words: List<Pair<String, Int>>): Int {
        var best = 0
        var covered = -1 to -1
        for ((word, weight) in words) {
            val at = before.lastIndexOf(word)
            if (at < 0) continue
            // "2° vencimiento" must not also count as the stronger plain "vencimiento".
            if (at >= covered.first && at + word.length <= covered.second) continue
            val distance = before.length - (at + word.length)
            val value = if (distance <= 30) weight else weight / 2
            if (covered.first < 0) covered = at to at + word.length
            if (value > best) best = value
        }
        return best
    }

    /** The words shown next to a hit so the person can tell it apart: what is written on its own line before it. */
    private fun label(text: String, from: Int, to: Int): String {
        val before = text.substring(from, to)
        val sameLine = clean(before.substringAfterLast('\n'))
        return (if (sameLine.length >= 3) sameLine else clean(before)).takeLast(36).trim()
    }

    private fun clean(s: String): String = s.replace(Regex("\\s+"), " ").trim()
}
