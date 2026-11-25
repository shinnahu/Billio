package app.billio.domain

import app.billio.data.AppData
import app.billio.data.Bill
import java.time.LocalDate
import java.time.YearMonth

enum class ReminderKind { OVERDUE, DUE_TODAY, DUE_TOMORROW, MONTHLY }

data class Reminder(val bill: Bill, val kind: ReminderKind, val amountCents: Long?, val dueDate: LocalDate?)

data class Upcoming(val date: LocalDate, val bill: Bill, val isDueDate: Boolean, val amountCents: Long?)

/** Decides which notifications a given day deserves. No Android here, so it can be tested as plain code. */
object Reminders {

    /** Days after the due date on which an unpaid bill is brought up again. */
    private val overdueDays = setOf(1L, 4L, 8L)

    fun on(day: LocalDate, data: AppData, today: LocalDate = day): List<Reminder> =
        data.bills.filter { it.remindersOn }.mapNotNull { bill ->
            val state = Bills.state(bill, data, today)
            if (state.status == Status.PAID || state.status == Status.DONE) return@mapNotNull null
            val dueKinds = Bills.openDues(bill, data).mapNotNull { due ->
                val date = Bills.date(due.dueDate) ?: return@mapNotNull null
                when {
                    day == date -> ReminderKind.DUE_TODAY
                    day == date.minusDays(1) -> ReminderKind.DUE_TOMORROW
                    date.isBefore(day) && (day.toEpochDay() - date.toEpochDay()) in overdueDays -> ReminderKind.OVERDUE
                    else -> null
                }
            }
            val monthly = ReminderKind.MONTHLY.takeIf {
                day == Bills.reminderDate(bill, YearMonth.from(day)) &&
                    !Bills.isPaid(data.payments, bill.id, Bills.key(YearMonth.from(day)), null)
            }
            val kind = (dueKinds + listOfNotNull(monthly)).minByOrNull { it.ordinal } ?: return@mapNotNull null
            Reminder(bill, kind, state.toPayCents, state.dueDate)
        }

    /**
     * Reminders for [today], plus the ones missed since [lastRun] (phone off, alarm delayed).
     * Each bill appears once, with its most urgent reason.
     */
    fun since(lastRun: LocalDate?, today: LocalDate, data: AppData): List<Reminder> {
        if (lastRun != null && !lastRun.isBefore(today)) return emptyList()
        val from = if (lastRun == null || lastRun.isBefore(today.minusDays(3))) today else lastRun.plusDays(1)
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }
        return days.flatMap { on(it, data, today) }
            .groupBy { it.bill.id }
            .map { (_, list) -> list.minByOrNull { it.kind.ordinal }!! }
            .sortedBy { it.kind.ordinal }
            .toList()
    }

    /** What is coming in the next [days] days: reminder days and known due dates of unpaid bills. */
    fun upcoming(today: LocalDate, data: AppData, days: Long = 45): List<Upcoming> {
        val until = today.plusDays(days)
        val out = mutableListOf<Upcoming>()
        for (bill in data.bills) {
            val state = Bills.state(bill, data, today)
            if (state.status == Status.DONE) continue
            for (due in Bills.openDues(bill, data)) {
                val date = Bills.date(due.dueDate) ?: continue
                if (!date.isBefore(today) && !date.isAfter(until)) out += Upcoming(date, bill, true, due.amountCents)
            }
            if (!bill.remindersOn) continue
            var month = YearMonth.from(today)
            repeat(3) {
                val date = Bills.reminderDate(bill, month)
                val paid = Bills.isPaid(data.payments, bill.id, Bills.key(month), null)
                if (!paid && !date.isBefore(today) && !date.isAfter(until)) {
                    out += Upcoming(date, bill, false, state.toPayCents.takeIf { month == state.period })
                }
                month = month.plusMonths(1)
            }
        }
        return out.sortedWith(compareBy({ it.date }, { it.bill.name.lowercase() }))
    }
}
