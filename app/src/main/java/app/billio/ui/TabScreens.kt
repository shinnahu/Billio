package app.billio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.R
import app.billio.Tab
import app.billio.data.Bill
import app.billio.data.Payment
import app.billio.domain.BillState
import app.billio.domain.Bills
import app.billio.domain.CategoryTotal
import app.billio.domain.MonthTotal
import app.billio.domain.Money
import java.time.YearMonth

/** The floating bar of the mock: a white pill with four places to go. */
@Composable
fun BottomNav(tab: Tab, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(36.dp)
    Surface(
        shape = shape,
        color = Color.White,
        modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth().height(70.dp).softShadow(14.dp, shape),
    ) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { t ->
                val selected = t == tab
                val color = if (selected) Palette.Blue else Palette.Muted
                Column(
                    Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(29.dp))
                        .background(if (selected) Palette.Bg else Color.Transparent)
                        .clickable { onTab(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        when (t) {
                            Tab.HOME -> Icons.Rounded.Home
                            Tab.BILLS -> Icons.Rounded.Receipt
                            Tab.STATS -> Icons.Rounded.BarChart
                            Tab.HISTORY -> Icons.Rounded.History
                        },
                        null, tint = color, modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(
                            when (t) {
                                Tab.HOME -> R.string.nav_home
                                Tab.BILLS -> R.string.nav_bills
                                Tab.STATS -> R.string.nav_stats
                                Tab.HISTORY -> R.string.nav_history
                            },
                        ),
                        color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPage(title: String, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    StatusBarIcons(white = false)
    Column(
        Modifier.fillMaxSize().background(Palette.Bg).verticalScroll(rememberScrollState()).statusBarsPadding()
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = NavClearance),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Palette.Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = Palette.Muted, fontSize = 15.5.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp))
}

// ---------------------------------------------------------------------------------------- your bills

@Composable
fun BillsScreen(states: List<BillState>, home: String, onOpen: (Bill) -> Unit, onAdd: () -> Unit) {
    TabPage(
        stringResource(R.string.nav_bills),
        action = { CircleButton(Icons.Rounded.Add, stringResource(R.string.add_new_bill), onAdd, background = Palette.Blue, tint = Color.White, size = 42.dp) },
    ) {
        if (states.isEmpty()) {
            EmptyNote(stringResource(R.string.bills_empty))
            return@TabPage
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            states.sortedBy { it.bill.name.lowercase() }.forEach { s ->
                Surface(onClick = { onOpen(s.bill) }, shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(s.bill.category.icon())
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    s.bill.name, color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                                )
                                Spacer(Modifier.width(8.dp))
                                StatusTag(s)
                            }
                            Text(
                                listOfNotNull(
                                    s.bill.accountNumber.takeIf { it.isNotBlank() },
                                    stringResource(R.string.reminder_short, s.bill.reminderDay).takeIf { s.bill.remindersOn },
                                ).joinToString(" · ").ifEmpty { stringResource(s.bill.category.label()) },
                                color = Palette.Muted, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        AmountLabel(s, home)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------- statistics

@Composable
fun StatsScreen(months: List<MonthTotal>, categories: List<CategoryTotal>, averageCents: Long?, home: String) {
    val locale = appLocale()
    TabPage(stringResource(R.string.nav_stats)) {
        if (months.all { it.cents == 0L }) {
            EmptyNote(stringResource(R.string.stats_empty))
            return@TabPage
        }
        val current = months.last()
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card {
                Column {
                    Label(stringResource(R.string.stats_paid_month, Fmt.month(current.month, locale)))
                    Spacer(Modifier.height(2.dp))
                    BigAmount(Money.parts(current.cents, home), color = Palette.Ink)
                    if (averageCents != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.stats_average, Money.short(averageCents, home)),
                            color = Palette.Muted, fontSize = 14.5.sp,
                        )
                    }
                }
            }
            Card {
                Column {
                    Text(stringResource(R.string.stats_last_months), color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(16.dp))
                    MonthBars(months)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        months.forEach { m ->
                            Text(
                                Fmt.monthShort(m.month, locale),
                                color = if (m.month == current.month) Palette.Ink else Palette.Muted,
                                fontSize = 12.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (categories.isNotEmpty()) {
                Card {
                    Column {
                        Text(stringResource(R.string.stats_by_category), color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        val top = categories.maxOf { it.cents }.coerceAtLeast(1)
                        categories.forEach { c ->
                            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconBubble(c.category.icon(), size = 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row {
                                        Text(stringResource(c.category.label()), color = Palette.Ink, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
                                        Text(Money.short(c.cents, home), color = Palette.Ink, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Palette.Field)) {
                                        Box(Modifier.fillMaxWidth(c.cents.toFloat() / top).height(6.dp).clip(CircleShape).background(Palette.Blue))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One bar per month; the current month is solid blue, the earlier ones lighter. */
@Composable
private fun MonthBars(months: List<MonthTotal>) {
    val top = months.maxOf { it.cents }.coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val slot = size.width / months.size
        val barWidth = slot * 0.46f
        months.forEachIndexed { i, m ->
            val height = (size.height * (m.cents.toFloat() / top)).coerceAtLeast(6.dp.toPx())
            drawRoundRect(
                color = if (i == months.lastIndex) Palette.Blue else Color(0xFFBFD5FC),
                topLeft = Offset(slot * i + (slot - barWidth) / 2, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------- history

@Composable
fun HistoryScreen(payments: List<Payment>, bills: List<Bill>, home: String, onEdit: (Payment) -> Unit) {
    val locale = appLocale()
    val byId = bills.associateBy { it.id }
    TabPage(stringResource(R.string.nav_history)) {
        val known = payments.filter { it.billId in byId }.sortedByDescending { it.paidOn }
        if (known.isEmpty()) {
            EmptyNote(stringResource(R.string.history_empty))
            return@TabPage
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            known.groupBy { it.paidOn.take(7) }.forEach { (monthKey, list) ->
                val total = list.groupBy { byId.getValue(it.billId).currency }
                    .map { (currency, items) -> Money.short(items.sumOf { it.amountCents }, currency, home) }
                    .joinToString(" + ")
                Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.month(runCatching { YearMonth.parse(monthKey) }.getOrDefault(YearMonth.now()), locale),
                        color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    )
                    Text(total, color = Palette.Muted, fontSize = 14.5.sp)
                }
                Card(padding = PaddingValues(vertical = 6.dp)) {
                    Column {
                        list.forEach { p ->
                            val bill = byId.getValue(p.billId)
                            Row(
                                Modifier.fillMaxWidth().clickable { onEdit(p) }.padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconBubble(bill.category.icon(), size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(bill.name, color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        (Bills.date(p.paidOn)?.let { Fmt.day(it, locale) } ?: p.paidOn) + " · " + paymentTitle(p, locale, short = true),
                                        color = Palette.Muted, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(Money.short(p.amountCents, bill.currency, home), color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
