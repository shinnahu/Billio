package app.billio.data

import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    INTERNET, MOBILE, ELECTRICITY, GAS, WATER, TAXES, INSURANCE, HEALTH,
    SUBSCRIPTION, CARD, SECURITY, EDUCATION, HOUSING, OTHER,
}

/** A bill the user tracks every month: one provider, one account. */
@Serializable
data class Bill(
    val id: String,
    val providerId: String? = null,
    val name: String,
    val category: Category,
    val country: String,
    val currency: String,
    /** What the provider calls the identifier, for example "N° de cuenta". */
    val accountLabel: String = "",
    val accountNumber: String = "",
    /** Day of the month on which the reminder is shown. */
    val reminderDay: Int = 1,
    val remindersOn: Boolean = true,
    /** Usual amount, used while this month's amount is unknown. Follows the last payment. */
    val estimatedCents: Long? = null,
    /** Set when the bill is a plan with a fixed number of instalments. */
    val cuotasTotal: Int? = null,
    /** Provider page where the amount can be checked. */
    val checkUrl: String = "",
    val notes: String = "",
    val createdOn: String = "",
)

/** The bill of one period: how much has to be paid, and until when. */
@Serializable
data class Due(
    val billId: String,
    /** Year and month, "2026-11". */
    val period: String,
    val amountCents: Long,
    val dueDate: String? = null,
    val cuota: Int? = null,
    /** True when the amount was read from the provider page instead of typed. */
    val fromWeb: Boolean = false,
)

@Serializable
data class Payment(
    val id: String,
    val billId: String,
    val period: String,
    val amountCents: Long,
    val paidOn: String,
    val cuota: Int? = null,
    val cuotaTotal: Int? = null,
    val note: String = "",
)

@Serializable
data class Settings(
    val userName: String = "",
    /** "AR" or "US"; empty until the user picks one (then the phone's region decides). */
    val country: String = "",
    /** "en" or "es"; empty until the user picks one (then the phone's language decides). */
    val language: String = "",
    val reminderHour: Int = 9,
    val askedNotifications: Boolean = false,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val settings: Settings = Settings(),
    val bills: List<Bill> = emptyList(),
    val dues: List<Due> = emptyList(),
    val payments: List<Payment> = emptyList(),
)
