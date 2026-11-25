package app.billio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.R
import app.billio.data.Payment
import app.billio.domain.BillState
import app.billio.domain.Bills
import app.billio.domain.Money
import app.billio.domain.Status
import java.util.Locale

@Composable
fun DetailScreen(
    state: BillState,
    payments: List<Payment>,
    home: String,
    canOpenMercadoPago: Boolean,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onCheckSite: () -> Unit,
    onEnterAmount: () -> Unit,
    onMercadoPago: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyAccount: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onEditPayment: (Payment) -> Unit,
) {
    StatusBarIcons(white = false)
    val locale = appLocale()
    val bill = state.bill
    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        TopBar(bill.name, onBack) {
            CircleButton(Icons.Rounded.Edit, stringResource(R.string.menu_edit), onEdit, background = Color.White)
            Spacer(Modifier.width(8.dp))
            CircleButton(Icons.Rounded.DeleteOutline, stringResource(R.string.menu_delete), onDelete, background = Color.White, tint = Palette.Red)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---- this month
            Card {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(bill.category.icon(), size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Fmt.month(state.period, locale), color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(bill.category.label()), color = Palette.Muted, fontSize = 13.5.sp)
                        }
                        StatusTag(state)
                    }
                    Spacer(Modifier.height(18.dp))
                    Label(
                        stringResource(
                            when (state.status) {
                                Status.PAID, Status.DONE -> R.string.paid_label
                                else -> R.string.amount_to_pay
                            },
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    val shown = when {
                        state.status == Status.PAID -> state.paidThisMonthCents
                        else -> state.toPayCents
                    }
                    if (shown != null) {
                        BigAmount(Money.parts(shown, bill.currency, home), color = Palette.Ink)
                    } else {
                        Text(stringResource(R.string.no_amount_yet), color = Palette.Faint, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                        if (state.status != Status.DONE && bill.estimatedCents != null) {
                            Text(
                                stringResource(R.string.usual_amount_value, Money.short(bill.estimatedCents, bill.currency, home)),
                                color = Palette.Muted, fontSize = 14.sp,
                            )
                        }
                    }
                    val line = detailLine(state, locale)
                    if (line.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(line, color = if (state.status == Status.OVERDUE) Palette.Red else Palette.Muted, fontSize = 14.5.sp)
                    }
                    if (state.nextCuota != null && bill.cuotasTotal != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.cuota_progress, state.nextCuota, bill.cuotasTotal), color = Palette.Muted, fontSize = 14.5.sp)
                    }

                    if (state.status != Status.DONE) {
                        Spacer(Modifier.height(18.dp))
                        PrimaryButton(stringResource(R.string.action_log_payment), onPay, icon = Icons.Rounded.Check)
                        Spacer(Modifier.height(10.dp))
                        if (bill.checkUrl.isNotBlank()) {
                            SoftButton(stringResource(R.string.action_check_site), onCheckSite, leading = Icons.Rounded.Search)
                            Spacer(Modifier.height(10.dp))
                        }
                        SoftButton(
                            stringResource(if (state.due == null) R.string.action_enter_amount else R.string.action_change_amount),
                            onEnterAmount, leading = Icons.Rounded.Edit,
                        )
                        if (canOpenMercadoPago) {
                            Spacer(Modifier.height(10.dp))
                            SoftButton(stringResource(R.string.action_open_mp), onMercadoPago, leading = Icons.AutoMirrored.Rounded.OpenInNew)
                        }
                    }
                }
            }

            // ---- details
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (bill.accountNumber.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Label(bill.accountLabel.ifBlank { stringResource(R.string.field_account) })
                                Text(bill.accountNumber, color = Palette.Ink, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            CircleButton(Icons.Rounded.ContentCopy, stringResource(R.string.copy), onCopyAccount)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Label(stringResource(R.string.reminders_label))
                            Text(stringResource(R.string.reminder_line, bill.reminderDay), color = Palette.Ink, fontSize = 17.sp)
                        }
                        Toggle(bill.remindersOn, onToggleReminders)
                    }
                    if (bill.checkUrl.isNotBlank()) {
                        Column {
                            Label(stringResource(R.string.field_site))
                            Text(
                                bill.checkUrl.removePrefix("https://").removePrefix("www.").trimEnd('/'),
                                color = Palette.Blue, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onCheckSite),
                            )
                        }
                    }
                    if (bill.notes.isNotBlank()) {
                        Column {
                            Label(stringResource(R.string.field_notes))
                            Text(bill.notes, color = Palette.Ink, fontSize = 16.sp)
                        }
                    }
                }
            }

            // ---- payments
            Text(
                stringResource(R.string.section_payments), color = Palette.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            )
            if (payments.isEmpty()) {
                Text(stringResource(R.string.no_payments), color = Palette.Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp))
            } else {
                Card(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
                    Column {
                        payments.sortedByDescending { it.paidOn }.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onEditPayment(p) }.padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(paymentTitle(p, locale), color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        stringResource(R.string.paid_on, Bills.date(p.paidOn)?.let { Fmt.full(it, locale) } ?: p.paidOn),
                                        color = Palette.Muted, fontSize = 13.5.sp,
                                    )
                                }
                                Text(Money.format(p.amountCents, bill.currency, home), color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "November 2026 · instalment 2/6", or with the month shortened to "Nov 2026" where space is tight. */
@Composable
fun paymentTitle(p: Payment, locale: Locale, short: Boolean = false): String {
    val period = Bills.period(p.period)
    val month = if (short) "${Fmt.monthShort(period, locale)} ${period.year}" else Fmt.month(period, locale)
    return when {
        p.cuota != null && p.cuotaTotal != null -> month + " · " + stringResource(R.string.cuota_short, p.cuota, p.cuotaTotal)
        p.cuota != null -> month + " · " + stringResource(R.string.cuota_short_one, p.cuota)
        else -> month
    }
}

@Composable
private fun detailLine(state: BillState, locale: Locale): String = when (state.status) {
    Status.OVERDUE -> state.dueDate?.let { stringResource(R.string.was_due_on, Fmt.full(it, locale)) }
    Status.DUE -> state.dueDate?.let { stringResource(R.string.due_on, Fmt.full(it, locale)) }
    Status.PENDING -> stringResource(R.string.pending_line)
    Status.UPCOMING, Status.PAID -> state.nextReminder?.let { stringResource(R.string.next_reminder_on, Fmt.full(it, locale)) }
    Status.DONE -> stringResource(R.string.done_line)
}.orEmpty()

@Composable
fun MissingBill(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Bg)) { TopBar("", onBack) }
}
