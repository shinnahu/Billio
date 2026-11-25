package app.billio.domain

import app.billio.data.AppData
import app.billio.data.Category
import java.time.LocalDate
import java.time.YearMonth

data class MonthTotal(val month: YearMonth, val cents: Long)

data class CategoryTotal(val category: Category, val cents: Long)

/** Totals of what was actually paid, by the month the payment was made. */
object Stats {

    fun monthly(data: AppData, currency: String, today: LocalDate, months: Int = 6): List<MonthTotal> {
        val ids = data.bills.filter { it.currency == currency }.map { it.id }.toSet()
        val last = YearMonth.from(today)
        return (months - 1 downTo 0).map { back ->
            val month = last.minusMonths(back.toLong())
            MonthTotal(month, data.payments.filter { it.billId in ids && it.paidOn.startsWith(month.toString()) }.sumOf { it.amountCents })
        }
    }

    fun byCategory(data: AppData, currency: String, month: YearMonth): List<CategoryTotal> {
        val bills = data.bills.filter { it.currency == currency }.associateBy { it.id }
        return data.payments
            .filter { it.billId in bills && it.paidOn.startsWith(month.toString()) }
            .groupBy { bills.getValue(it.billId).category }
            .map { (category, list) -> CategoryTotal(category, list.sumOf { it.amountCents }) }
            .sortedByDescending { it.cents }
    }

    /** Average of the months that have payments, leaving out the current one because it is not finished. */
    fun average(data: AppData, currency: String, today: LocalDate): Long? {
        val past = monthly(data, currency, today, 7).dropLast(1).filter { it.cents > 0 }
        return if (past.isEmpty()) null else past.sumOf { it.cents } / past.size
    }
}
