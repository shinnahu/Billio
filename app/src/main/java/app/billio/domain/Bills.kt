package app.billio.domain

import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Due
import app.billio.data.Payment
import java.time.LocalDate
import java.time.YearMonth

enum class Status {
    /** There is an amount to pay and its due date has passed. */
    OVERDUE,

    /** There is an amount to pay. */
    DUE,

    /** The reminder day has arrived, nothing was paid, and the amount is not known yet. */
    PENDING,

    /** Nothing to do yet this month. */
    UPCOMING,

    /** This month is paid. */
    PAID,

    /** A plan whose instalments are all paid. */
    DONE,
}

data class BillState(
    val bill: Bill,
    val status: Status,
    /** The month this state talks about: the oldest unpaid one, otherwise the current one. */
    val period: YearMonth,
    /** The oldest unpaid bill, when its amount is known. */
    val due: Due?,
    /** Everything still unpaid whose amount is known, in cents. Null when no amount is known. */
    val toPayCents: Long?,
    val dueDate: LocalDate?,
    val paidThisMonthCents: Long,
    val nextReminder: LocalDate?,
    /** Next instalment to pay, for plans with a fixed number of them. */
    val nextCuota: Int?,
) {
    val needsAction: Boolean get() = status == Status.OVERDUE || status == Status.DUE || status == Status.PENDING

    /** What to show as the amount: the real one, or the usual one while the real one is unknown. */
    val shownCents: Long? get() = toPayCents ?: bill.estimatedCents
    val isEstimate: Boolean get() = toPayCents == null && bill.estimatedCents != null
}

object Bills {

    fun key(period: YearMonth): String = period.toString()

    fun period(key: String): YearMonth = runCatching { YearMonth.parse(key) }.getOrElse { YearMonth.now() }

    fun date(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** The reminder day inside [month]; the 31st becomes the last day of shorter months. */
    fun reminderDate(bill: Bill, month: YearMonth): LocalDate =
        month.atDay(bill.reminderDay.coerceIn(1, month.lengthOfMonth()))

    fun isPaid(payments: List<Payment>, billId: String, period: String, cuota: Int?): Boolean =
        payments.any { it.billId == billId && it.period == period && (cuota == null || it.cuota == null || it.cuota == cuota) }

    fun openDues(bill: Bill, data: AppData): List<Due> =
        data.dues.filter { it.billId == bill.id && !isPaid(data.payments, bill.id, it.period, it.cuota) }
            .sortedWith(compareBy({ it.period }, { it.cuota ?: 0 }))

    fun state(bill: Bill, data: AppData, today: LocalDate): BillState {
        val current = YearMonth.from(today)
        val payments = data.payments.filter { it.billId == bill.id }
        val open = openDues(bill, data)
        val first = open.firstOrNull()
        val paidThisMonth = payments.filter { it.period == key(current) }.sumOf { it.amountCents }
        val paidCurrent = payments.any { it.period == key(current) }
        val lastCuota = payments.mapNotNull { it.cuota }.maxOrNull() ?: 0
        val finished = bill.cuotasTotal != null && lastCuota >= bill.cuotasTotal
        val nextCuota = bill.cuotasTotal?.let { total -> (lastCuota + 1).takeIf { it <= total } }

        val status = when {
            first != null -> if (date(first.dueDate)?.isBefore(today) == true) Status.OVERDUE else Status.DUE
            finished -> Status.DONE
            paidCurrent -> Status.PAID
            !today.isBefore(reminderDate(bill, current)) -> Status.PENDING
            else -> Status.UPCOMING
        }
        val nextReminder = when {
            finished || !bill.remindersOn -> null
            !paidCurrent && !today.isAfter(reminderDate(bill, current)) -> reminderDate(bill, current)
            else -> reminderDate(bill, current.plusMonths(1))
        }
        return BillState(
            bill = bill,
            status = status,
            period = first?.let { period(it.period) } ?: current,
            due = first,
            toPayCents = if (open.isEmpty()) null else open.sumOf { it.amountCents },
            dueDate = date(first?.dueDate),
            paidThisMonthCents = paidThisMonth,
            nextReminder = nextReminder,
            nextCuota = nextCuota,
        )
    }

    /** Every bill, the ones that need attention first. */
    fun states(data: AppData, today: LocalDate): List<BillState> =
        data.bills.map { state(it, data, today) }.sortedWith(
            compareBy<BillState>({ it.status.ordinal })
                .thenBy { it.dueDate ?: it.nextReminder ?: LocalDate.MAX }
                .thenBy { it.bill.name.lowercase() },
        )
}

/** What the home screen shows above the list. */
data class Summary(
    val currency: String,
    /** Unpaid amounts that are known, plus the usual amount of bills still without one. */
    val toPayCents: Long,
    /** How many bills are counted with their usual amount instead of a real one. */
    val estimated: Int,
    val paidThisMonthCents: Long,
    /** Unpaid total in the other currency, when there are bills in both. */
    val otherCurrency: String?,
    val otherToPayCents: Long,
) {
    companion object {
        fun of(states: List<BillState>, payments: List<Payment>, home: String, today: LocalDate): Summary {
            val month = Bills.key(YearMonth.from(today))
            fun unpaid(list: List<BillState>) = list.filter { it.status != Status.PAID && it.status != Status.DONE }
            val mine = states.filter { it.bill.currency == home }
            val others = states.filter { it.bill.currency != home }
            val ids = mine.map { it.bill.id }.toSet()
            return Summary(
                currency = home,
                toPayCents = unpaid(mine).sumOf { it.shownCents ?: 0L },
                estimated = unpaid(mine).count { it.isEstimate },
                paidThisMonthCents = payments.filter { it.billId in ids && it.paidOn.startsWith(month) }.sumOf { it.amountCents },
                otherCurrency = others.firstOrNull()?.bill?.currency,
                otherToPayCents = unpaid(others).sumOf { it.shownCents ?: 0L },
            )
        }
    }
}
