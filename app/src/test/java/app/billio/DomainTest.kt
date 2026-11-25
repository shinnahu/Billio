package app.billio

import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Category
import app.billio.data.Due
import app.billio.data.Payment
import app.billio.data.Store
import app.billio.domain.AmountFinder
import app.billio.domain.Bills
import app.billio.domain.Money
import app.billio.domain.ReminderKind
import app.billio.domain.Reminders
import app.billio.domain.Stats
import app.billio.domain.Status
import app.billio.domain.Summary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.YearMonth

class DomainTest {

    private val today = LocalDate.of(2026, 11, 25)

    private fun bill(id: String = "b1", day: Int = 10, currency: String = "ARS", estimate: Long? = null, cuotas: Int? = null) = Bill(
        id = id, name = "Edenor", category = Category.ELECTRICITY, country = if (currency == "USD") "US" else "AR",
        currency = currency, reminderDay = day, estimatedCents = estimate, cuotasTotal = cuotas,
    )

    // ------------------------------------------------------------------ money

    @Test
    fun formatsEachCurrencyTheWayItsCountryWritesIt() {
        assertEquals("$45.230,10", Money.format(4523010, "ARS"))
        assertEquals("$1,450.05", Money.format(145005, "USD"))
        assertEquals("$0,05", Money.format(5, "ARS"))
        assertEquals("$1,450" to ".05", Money.parts(145005, "USD"))
        assertEquals("$65", Money.short(6500, "USD"))
        assertEquals("US$65", Money.short(6500, "USD", home = "ARS"))
        assertEquals("AR$1.000", Money.short(100000, "ARS", home = "USD"))
        assertEquals("45230,10", Money.editable(4523010, "ARS"))
        assertEquals("65", Money.editable(6500, "USD"))
    }

    @Test
    fun readsAmountsAsPeopleTypeThem() {
        assertEquals(4523010L, Money.parse("45.230,10", "ARS"))
        assertEquals(4523010L, Money.parse("$ 45.230,10", "ARS"))
        assertEquals(4523000L, Money.parse("45.230", "ARS"))
        assertEquals(4523000L, Money.parse("45230", "ARS"))
        assertEquals(4550L, Money.parse("45,5", "ARS"))
        assertEquals(4550L, Money.parse("45.50", "ARS"))
        assertEquals(145005L, Money.parse("$1,450.05", "USD"))
        assertEquals(145000L, Money.parse("1,450", "USD"))
        assertEquals(123456789L, Money.parse("1.234.567,89", "ARS"))
        assertEquals(123456789L, Money.parse("1,234,567.89", "USD"))
        assertNull(Money.parse("", "ARS"))
        assertNull(Money.parse("abc", "ARS"))
        assertNull(Money.parse("1.2345", "ARS"))
    }

    // ------------------------------------------------------------------ bill status

    @Test
    fun billGoesFromUpcomingToPendingToDueToPaid() {
        val b = bill(day = 10)
        var data = AppData(bills = listOf(b))
        assertEquals(Status.UPCOMING, Bills.state(b, data, LocalDate.of(2026, 11, 5)).status)
        assertEquals(LocalDate.of(2026, 11, 10), Bills.state(b, data, LocalDate.of(2026, 11, 5)).nextReminder)
        assertEquals(Status.PENDING, Bills.state(b, data, LocalDate.of(2026, 11, 10)).status)

        data = data.copy(dues = listOf(Due("b1", "2026-11", 4523010, "2026-11-28")))
        val due = Bills.state(b, data, today)
        assertEquals(Status.DUE, due.status)
        assertEquals(4523010L, due.toPayCents)
        assertEquals(LocalDate.of(2026, 11, 28), due.dueDate)
        assertTrue(due.needsAction)
        assertEquals(Status.OVERDUE, Bills.state(b, data, LocalDate.of(2026, 11, 29)).status)

        data = data.copy(payments = listOf(Payment("p1", "b1", "2026-11", 4523010, "2026-11-26")))
        val paid = Bills.state(b, data, LocalDate.of(2026, 11, 26))
        assertEquals(Status.PAID, paid.status)
        assertEquals(4523010L, paid.paidThisMonthCents)
        assertFalse(paid.needsAction)
        assertEquals(LocalDate.of(2026, 12, 10), paid.nextReminder)
    }

    @Test
    fun lastMonthsUnpaidBillStaysOnTopUntilItIsPaid() {
        val b = bill()
        val data = AppData(bills = listOf(b), dues = listOf(Due("b1", "2026-10", 300000, "2026-11-05")))
        val state = Bills.state(b, data, today)
        assertEquals(Status.OVERDUE, state.status)
        assertEquals(YearMonth.of(2026, 10), state.period)
    }

    @Test
    fun reminderDay31FallsOnTheLastDayOfShortMonths() {
        assertEquals(LocalDate.of(2027, 2, 28), Bills.reminderDate(bill(day = 31), YearMonth.of(2027, 2)))
        assertEquals(LocalDate.of(2026, 11, 30), Bills.reminderDate(bill(day = 31), YearMonth.of(2026, 11)))
    }

    @Test
    fun planWithCuotasCountsUpAndFinishes() {
        val b = bill(cuotas = 3)
        var data = AppData(bills = listOf(b))
        assertEquals(1, Bills.state(b, data, today).nextCuota)
        data = data.copy(payments = listOf(Payment("p1", "b1", "2026-10", 1000, "2026-10-10", cuota = 1, cuotaTotal = 3)))
        assertEquals(2, Bills.state(b, data, today).nextCuota)
        data = data.copy(payments = data.payments + Payment("p2", "b1", "2026-11", 1000, "2026-11-10", cuota = 2, cuotaTotal = 3) +
            Payment("p3", "b1", "2026-12", 1000, "2026-11-20", cuota = 3, cuotaTotal = 3))
        val done = Bills.state(b, data, today)
        assertEquals(Status.DONE, done.status)
        assertNull(done.nextCuota)
        assertNull(done.nextReminder)
        assertTrue(Reminders.on(LocalDate.of(2026, 12, 10), data).isEmpty())
    }

    @Test
    fun summaryAddsKnownAmountsAndUsualAmountsAndKeepsCurrenciesApart() {
        val known = bill("b1")
        val usual = bill("b2", estimate = 100000)
        val paid = bill("b3")
        val dollars = bill("b4", currency = "USD", estimate = 6500)
        val data = AppData(
            bills = listOf(known, usual, paid, dollars),
            dues = listOf(Due("b1", "2026-11", 4523010, "2026-11-28")),
            payments = listOf(Payment("p1", "b3", "2026-11", 2000000, "2026-11-12")),
        )
        val s = Summary.of(Bills.states(data, today), data.payments, "ARS", today)
        assertEquals(4523010L + 100000L, s.toPayCents)
        assertEquals(1, s.estimated)
        assertEquals(2000000L, s.paidThisMonthCents)
        assertEquals("USD", s.otherCurrency)
        assertEquals(6500L, s.otherToPayCents)
    }

    @Test
    fun billsThatNeedAttentionComeFirst() {
        val data = AppData(
            bills = listOf(bill("paid"), bill("later", day = 28), bill("overdue"), bill("due")),
            dues = listOf(Due("overdue", "2026-11", 100, "2026-11-20"), Due("due", "2026-11", 100, "2026-11-30")),
            payments = listOf(Payment("p", "paid", "2026-11", 100, "2026-11-02")),
        )
        assertEquals(listOf("overdue", "due", "later", "paid"), Bills.states(data, today).map { it.bill.id })
    }

    // ------------------------------------------------------------------ reminders

    @Test
    fun remindsOnTheChosenDayUnlessAlreadyPaid() {
        val b = bill(day = 10)
        val data = AppData(bills = listOf(b))
        assertEquals(listOf(ReminderKind.MONTHLY), Reminders.on(LocalDate.of(2026, 11, 10), data).map { it.kind })
        assertTrue(Reminders.on(LocalDate.of(2026, 11, 11), data).isEmpty())
        val paid = data.copy(payments = listOf(Payment("p", "b1", "2026-11", 100, "2026-11-03")))
        assertTrue(Reminders.on(LocalDate.of(2026, 11, 10), paid).isEmpty())
        assertTrue(Reminders.on(LocalDate.of(2026, 11, 10), data.copy(bills = listOf(b.copy(remindersOn = false)))).isEmpty())
    }

    @Test
    fun remindsAroundTheDueDate() {
        val b = bill(day = 1)
        val data = AppData(bills = listOf(b), dues = listOf(Due("b1", "2026-11", 4523010, "2026-11-28")))
        fun kinds(day: Int, month: Int = 11) = Reminders.on(LocalDate.of(2026, month, day), data).map { it.kind }
        assertEquals(listOf(ReminderKind.DUE_TOMORROW), kinds(27))
        assertEquals(listOf(ReminderKind.DUE_TODAY), kinds(28))
        assertEquals(listOf(ReminderKind.OVERDUE), kinds(29))
        assertTrue(kinds(30).isEmpty())
        assertEquals(listOf(ReminderKind.OVERDUE), kinds(2, 12))
        assertEquals(4523010L, Reminders.on(LocalDate.of(2026, 11, 28), data).single().amountCents)
    }

    @Test
    fun catchesUpOnDaysThePhoneMissedWithoutRepeating() {
        val b = bill(day = 10)
        val data = AppData(bills = listOf(b))
        // Alarm last ran on the 8th, next ran on the 11th: the reminder of the 10th is still delivered once.
        assertEquals(1, Reminders.since(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 11), data).size)
        assertTrue(Reminders.since(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 11), data).isEmpty())
        assertTrue(Reminders.since(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12), data).isEmpty())
        // After a long gap only today counts, so old reminders do not pile up.
        assertTrue(Reminders.since(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 20), data).isEmpty())
        assertEquals(1, Reminders.since(null, LocalDate.of(2026, 11, 10), data).size)
    }

    @Test
    fun upcomingListsReminderDaysAndDueDatesInOrder() {
        val a = bill("a", day = 28)
        val b = bill("b", day = 5)
        val data = AppData(bills = listOf(a, b), dues = listOf(Due("b", "2026-11", 500, "2026-12-01")))
        val list = Reminders.upcoming(today, data, days = 20)
        assertEquals(
            listOf(LocalDate.of(2026, 11, 28) to false, LocalDate.of(2026, 12, 1) to true, LocalDate.of(2026, 12, 5) to false),
            list.map { it.date to it.isDueDate },
        )
        assertEquals(500L, list[1].amountCents)
    }

    // ------------------------------------------------------------------ reading a provider page

    @Test
    fun findsTheBillAmountOnAnArgentinePage() {
        val page = """
            Edenor Digital
            N° de cuenta 1234567890
            Consumo del período 350,00 kWh
            IVA 21,00 %
            Cargo fijo ${'$'} 3.210,55
            Total a pagar ${'$'} 45.230,10
            Vencimiento 10/12/2026
            2° vencimiento 20/12/2026
        """.trimIndent()
        val amounts = AmountFinder.amounts(page, "ARS")
        assertEquals(4523010L, amounts.first().cents)
        assertTrue(amounts.none { it.cents == 35000L }) // kWh is not money
        assertTrue(amounts.none { it.cents == 2100L }) // a percentage is not money
        assertTrue(amounts.none { it.cents == 123456789000L }) // the account number is not money
        val dates = AmountFinder.dates(page, "ARS", today)
        assertEquals(LocalDate.of(2026, 12, 10), dates.first().date)
        assertEquals(LocalDate.of(2026, 12, 20), dates[1].date)
    }

    @Test
    fun findsTheBillAmountOnAnAmericanPage() {
        val page = "Account 0012345678\nPrevious balance $1,200.00\nPayment received -$1,200.00\nAmount due $1,450.05\nDue date Dec 10, 2026\nStatement date 11/20/2026"
        assertEquals(145005L, AmountFinder.amounts(page, "USD").first().cents)
        val dates = AmountFinder.dates(page, "USD", today)
        assertEquals(LocalDate.of(2026, 12, 10), dates.first().date)
        assertTrue(dates.any { it.date == LocalDate.of(2026, 11, 20) })
    }

    @Test
    fun readsDatesWrittenWithWords() {
        fun first(text: String, currency: String = "ARS") = AmountFinder.dates(text, currency, today).first().date
        assertEquals(LocalDate.of(2026, 12, 10), first("Vence el 10 de diciembre de 2026"))
        assertEquals(LocalDate.of(2026, 12, 10), first("Vence el 10 de diciembre"))
        assertEquals(LocalDate.of(2027, 1, 5), first("Vencimiento: 5 ene"))
        assertEquals(LocalDate.of(2026, 12, 10), first("Pay by December 10th", "USD"))
        assertEquals(LocalDate.of(2026, 12, 10), first("Due 2026-12-10", "USD"))
        assertTrue(AmountFinder.dates("Cliente desde 01/01/2015", "ARS", today).isEmpty())
    }

    @Test
    fun pageWithoutAmountsGivesNothing() {
        assertTrue(AmountFinder.amounts("Ingresá tu número de cuenta 1234567890 y presioná Siguiente", "ARS").isEmpty())
    }

    // ------------------------------------------------------------------ statistics and storage

    @Test
    fun statisticsAddPaymentsByMonthAndCategory() {
        val light = bill("light")
        val net = light.copy(id = "net", name = "Telecentro", category = Category.INTERNET)
        val data = AppData(
            bills = listOf(light, net),
            payments = listOf(
                Payment("1", "light", "2026-10", 4000000, "2026-10-12"),
                Payment("2", "net", "2026-10", 3000000, "2026-10-15"),
                Payment("3", "light", "2026-11", 4500000, "2026-11-12"),
            ),
        )
        val months = Stats.monthly(data, "ARS", today, 3)
        assertEquals(listOf(0L, 7000000L, 4500000L), months.map { it.cents })
        assertEquals(YearMonth.of(2026, 11), months.last().month)
        assertEquals(listOf(Category.ELECTRICITY to 4000000L, Category.INTERNET to 3000000L),
            Stats.byCategory(data, "ARS", YearMonth.of(2026, 10)).map { it.category to it.cents })
        assertEquals(7000000L, Stats.average(data, "ARS", today))
    }

    @Test
    fun storeKeepsDataAcrossRestartsAndRestoresBackups() {
        val dir = Files.createTempDirectory("bills").toFile()
        val file = File(dir, "bills.json")
        val store = Store(file)
        store.update { it.copy(bills = listOf(bill()), payments = listOf(Payment("p", "b1", "2026-11", 123, "2026-11-25", cuota = 2, cuotaTotal = 6))) }
        val reopened = Store(file)
        assertEquals(store.data.value, reopened.data.value)
        assertEquals(2, reopened.data.value.payments.single().cuota)

        val backup = store.exportJson()
        val other = Store(File(dir, "other.json"))
        assertTrue(other.importJson(backup))
        assertEquals(store.data.value, other.data.value)
        assertFalse(other.importJson("this is not a backup"))
        assertEquals(store.data.value, other.data.value)
        dir.deleteRecursively()
    }
}
