package app.billio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Catalog
import app.billio.data.Category
import app.billio.data.Due
import app.billio.data.Payment
import app.billio.data.Provider
import app.billio.data.Settings
import app.billio.data.Store
import app.billio.domain.AmountHit
import app.billio.domain.Bills
import app.billio.domain.DateHit
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class Tab { HOME, BILLS, STATS, HISTORY }

/** Full screens opened on top of the tabs. */
sealed interface Screen {
    data class Detail(val billId: String) : Screen
    data class Browser(val billId: String) : Screen
    data object Settings : Screen
}

/** Cards that slide up from the bottom. */
sealed interface Sheet {
    /** Connect a provider: pick a category, then a company, then fill in the details. */
    data class Add(val category: Category? = null, val provider: Provider? = null, val custom: Boolean = false) : Sheet
    data class Edit(val billId: String) : Sheet
    data class Pay(val billId: String, val paymentId: String? = null) : Sheet
    data class Amount(val billId: String) : Sheet
    data class Capture(val billId: String, val amounts: List<AmountHit>, val dates: List<DateHit>) : Sheet
    data class Delete(val billId: String) : Sheet
    data object Upcoming : Sheet
}

/** What is on screen plus every change the user can make. Plain class, so tests can drive it directly. */
class AppState(
    val store: Store,
    val catalog: Catalog,
    private val clock: () -> LocalDate = LocalDate::now,
    private val deviceCountry: String = "AR",
) {
    val data: StateFlow<AppData> get() = store.data

    var today by mutableStateOf(clock())
        private set
    var tab by mutableStateOf(Tab.HOME)
    val stack = mutableStateListOf<Screen>()
    var sheet by mutableStateOf<Sheet?>(null)
    var needsActionOnly by mutableStateOf(false)

    fun tick() {
        today = clock()
    }

    fun country(settings: Settings): String = settings.country.ifEmpty { deviceCountry }

    fun open(screen: Screen) {
        sheet = null
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    /** Closes whatever is on top. False when there is nothing left to close. */
    fun back(): Boolean {
        when {
            sheet != null -> sheet = null
            stack.isNotEmpty() -> stack.removeAt(stack.lastIndex)
            tab != Tab.HOME -> tab = Tab.HOME
            else -> return false
        }
        return true
    }

    fun openFromNotification(billId: String, pay: Boolean) {
        if (store.data.value.bills.none { it.id == billId }) return
        stack.clear()
        stack.add(Screen.Detail(billId))
        sheet = if (pay) Sheet.Pay(billId) else null
    }

    fun bill(id: String): Bill? = store.data.value.bills.firstOrNull { it.id == id }

    // ------------------------------------------------------------------ changes

    fun addBill(bill: Bill): String {
        val id = newId()
        store.update { it.copy(bills = it.bills + bill.copy(id = id, createdOn = today.toString())) }
        return id
    }

    fun updateBill(bill: Bill) = store.update { data ->
        data.copy(bills = data.bills.map { if (it.id == bill.id) bill else it })
    }

    fun deleteBill(id: String) {
        stack.removeAll { it == Screen.Detail(id) || it == Screen.Browser(id) }
        store.update { data ->
            data.copy(
                bills = data.bills.filterNot { it.id == id },
                dues = data.dues.filterNot { it.billId == id },
                payments = data.payments.filterNot { it.billId == id },
            )
        }
    }

    /** Sets how much has to be paid for a month, replacing what was there. */
    fun setDue(billId: String, period: YearMonth, cents: Long, dueDate: LocalDate?, fromWeb: Boolean = false) = store.update { data ->
        val key = Bills.key(period)
        val due = Due(billId, key, cents, dueDate?.toString(), fromWeb = fromWeb)
        data.copy(dues = data.dues.filterNot { it.billId == billId && it.period == key } + due)
    }

    fun clearDue(billId: String, period: YearMonth) = store.update { data ->
        data.copy(dues = data.dues.filterNot { it.billId == billId && it.period == Bills.key(period) })
    }

    /** Adds a payment, or replaces the one with the same id. The bill's usual amount follows the latest payment. */
    fun savePayment(payment: Payment) = store.update { data ->
        val saved = if (payment.id.isEmpty()) payment.copy(id = newId()) else payment
        val payments = data.payments.filterNot { it.id == saved.id } + saved
        val latest = payments.filter { it.billId == saved.billId }.maxByOrNull { it.paidOn }
        data.copy(
            payments = payments,
            bills = data.bills.map { if (it.id == saved.billId && latest != null) it.copy(estimatedCents = latest.amountCents) else it },
        )
    }

    fun deletePayment(id: String) = store.update { data -> data.copy(payments = data.payments.filterNot { it.id == id }) }

    fun setSettings(change: (Settings) -> Settings) = store.update { it.copy(settings = change(it.settings)) }

    private fun newId(): String = UUID.randomUUID().toString().take(12)
}
