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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.R
import app.billio.data.Bill
import app.billio.domain.BillState
import app.billio.domain.Money
import app.billio.domain.Status
import app.billio.domain.Summary
import java.util.Locale

@Composable
fun HomeScreen(
    name: String,
    summary: Summary,
    states: List<BillState>,
    needsActionOnly: Boolean,
    onFilter: (Boolean) -> Unit,
    onBell: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Bill) -> Unit,
    onFocusAction: (BillState?) -> Unit,
) {
    val scroll = rememberScrollState()
    val headerEnd = with(LocalDensity.current) { 250.dp.toPx() }
    val overHeader by remember { derivedStateOf { scroll.value < headerEnd } }
    StatusBarIcons(white = overHeader)

    val home = summary.currency
    val focus = states.firstOrNull { it.needsAction }
    val shown = if (needsActionOnly) states.filter { it.needsAction } else states

    Box(Modifier.fillMaxSize().background(Palette.Bg).verticalScroll(scroll)) {
        Box(Modifier.fillMaxWidth().height(520.dp).background(HeaderBrush))
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isBlank()) {
                        Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(initials(name), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (name.isBlank()) stringResource(R.string.hello) else stringResource(R.string.hello_name, name.trim()),
                    color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                HeaderIcon(Icons.Rounded.NotificationsNone, stringResource(R.string.upcoming_title), onBell)
                HeaderIcon(Icons.Outlined.Settings, stringResource(R.string.settings_title), onSettings)
            }

            Spacer(Modifier.height(30.dp))
            Text(
                stringResource(R.string.to_pay_month),
                color = Color.White.copy(alpha = 0.88f), fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            BigAmount(Money.parts(summary.toPayCents, home), Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(17.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Palette.Blue, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.paid_this_month, Money.short(summary.paidThisMonthCents, home)),
                    color = Color.White, fontSize = 15.5.sp,
                )
            }
            val notes = listOfNotNull(
                if (summary.estimated > 0) stringResource(R.string.estimates_note, summary.estimated) else null,
                summary.otherCurrency?.takeIf { summary.otherToPayCents > 0 }?.let {
                    stringResource(R.string.other_currency_line, Money.short(summary.otherToPayCents, it, home))
                },
            )
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    notes.joinToString("  ·  "), color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(30.dp))
            FocusCard(focus, states, home, onFocusAction)

            Spacer(Modifier.height(26.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.all_bills), !needsActionOnly, { onFilter(false) })
                Chip(stringResource(R.string.needs_action), needsActionOnly, { onFilter(true) })
            }
            Spacer(Modifier.height(10.dp))
            if (shown.isEmpty() && states.isNotEmpty()) {
                Text(
                    stringResource(R.string.nothing_needs_action), color = Palette.Muted, fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
            }
            shown.forEach { BillRow(it, home, onClick = { onOpen(it.bill) }) }

            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF0F1F4),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(56.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = Palette.Muted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.add_new_bill), color = Palette.Muted, fontSize = 16.5.sp)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(NavClearance))
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** The big number of the header, with the cents dimmed as in the mock. Long amounts get smaller type. */
@Composable
fun BigAmount(parts: Pair<String, String>, modifier: Modifier = Modifier, color: Color = Color.White) {
    val length = parts.first.length + parts.second.length
    val size = when {
        length <= 10 -> 50.sp
        length <= 13 -> 43.sp
        length <= 16 -> 36.sp
        else -> 30.sp
    }
    Text(
        buildAnnotatedString {
            append(parts.first)
            withStyle(SpanStyle(color = color.copy(alpha = 0.55f))) { append(parts.second) }
        },
        color = color, fontSize = size, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp,
        textAlign = TextAlign.Center, maxLines = 1, modifier = modifier,
    )
}

/** The card under the total: the one bill that most needs attention, with its next step. */
@Composable
private fun FocusCard(focus: BillState?, states: List<BillState>, home: String, onAction: (BillState?) -> Unit) {
    val locale = appLocale()
    val title: String
    val icon: ImageVector
    val body: String
    val strong: String
    val action: String
    when {
        states.isEmpty() -> {
            title = stringResource(R.string.focus_empty_title)
            icon = Icons.Rounded.Savings
            body = stringResource(R.string.focus_empty_body)
            strong = ""
            action = stringResource(R.string.focus_empty_action)
        }
        focus == null -> {
            val next = states.filter { it.nextReminder != null }.minByOrNull { it.nextReminder!! }
            title = stringResource(R.string.focus_ok_title)
            icon = Icons.Rounded.Savings
            body = if (next == null) stringResource(R.string.focus_ok_body) else
                stringResource(R.string.focus_ok_body_next, next.bill.name, Fmt.day(next.nextReminder!!, locale))
            strong = next?.bill?.name.orEmpty()
            action = stringResource(R.string.focus_ok_action)
        }
        else -> {
            title = focus.bill.name
            icon = focus.bill.category.icon()
            val amount = focus.toPayCents?.let { Money.format(it, focus.bill.currency, home) }.orEmpty()
            strong = amount
            val date = focus.dueDate?.let { Fmt.day(it, locale) }
            body = when (focus.status) {
                Status.OVERDUE -> stringResource(R.string.focus_overdue_body, amount, date.orEmpty())
                Status.DUE -> if (date == null) stringResource(R.string.focus_due_body, amount) else
                    stringResource(R.string.focus_due_body_date, amount, date)
                else -> if (focus.bill.checkUrl.isBlank()) stringResource(R.string.focus_pending_body_nourl) else
                    stringResource(R.string.focus_pending_body)
            }
            action = when {
                focus.status != Status.PENDING -> stringResource(R.string.action_log_payment)
                focus.bill.checkUrl.isBlank() -> stringResource(R.string.action_enter_amount)
                else -> stringResource(R.string.action_check_amount)
            }
        }
    }

    Surface(shape = RoundedCornerShape(26.dp), color = Color.White, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(Modifier.padding(6.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Palette.Blue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    title, color = Palette.Ink, fontSize = 18.5.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (focus != null) {
                    Spacer(Modifier.width(10.dp))
                    StatusTag(focus)
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Bg)) {
                Text(
                    emphasised(body, strong),
                    color = Palette.Muted, fontSize = 16.sp, lineHeight = 23.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                )
                SoftButton(
                    action, { onAction(focus) }, trailing = Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

/** [text] with the first appearance of [strong] in dark ink, like the "$45" of the mock. */
private fun emphasised(text: String, strong: String) = buildAnnotatedString {
    val at = if (strong.isEmpty()) -1 else text.indexOf(strong)
    if (at < 0) {
        append(text)
    } else {
        append(text.substring(0, at))
        withStyle(SpanStyle(color = Palette.Ink, fontWeight = FontWeight.Medium)) { append(strong) }
        append(text.substring(at + strong.length))
    }
}

@Composable
fun StatusTag(state: BillState, modifier: Modifier = Modifier) {
    when (state.status) {
        Status.OVERDUE -> Tag(stringResource(R.string.tag_overdue), Palette.Red, modifier)
        Status.DUE -> Tag(stringResource(R.string.tag_to_pay), Palette.Orange, modifier)
        Status.PENDING -> Tag(stringResource(R.string.tag_check), Palette.Orange, modifier)
        Status.PAID -> Tag(stringResource(R.string.tag_paid), Palette.Green, modifier)
        Status.DONE -> Tag(stringResource(R.string.tag_done), Palette.Green, modifier)
        Status.UPCOMING -> Unit
    }
}

/** The second line of a bill row: its kind of service and the date that matters now. */
@Composable
fun statusLine(state: BillState, locale: Locale): String {
    val category = stringResource(state.bill.category.label())
    val detail = when (state.status) {
        Status.OVERDUE -> state.dueDate?.let { stringResource(R.string.sub_was_due, Fmt.day(it, locale)) }
        Status.DUE -> state.dueDate?.let { stringResource(R.string.sub_due, Fmt.day(it, locale)) }
        Status.UPCOMING, Status.PAID -> state.nextReminder?.let { stringResource(R.string.sub_reminder, Fmt.day(it, locale)) }
        Status.PENDING, Status.DONE -> null
    }
    return if (detail == null) category else "$category · $detail"
}

@Composable
fun BillRow(state: BillState, home: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = appLocale()
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(state.bill.category.icon())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.bill.name, color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                StatusTag(state)
            }
            Spacer(Modifier.height(2.dp))
            Text(statusLine(state, locale), color = Palette.Muted, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        AmountLabel(state, home)
    }
}

/** Right side of a bill row: what is owed, what was paid, or the usual amount with a "~" in front. */
@Composable
fun AmountLabel(state: BillState, home: String) {
    val currency = state.bill.currency
    val text: String
    val color: Color
    when {
        state.status == Status.PAID -> {
            text = Money.short(state.paidThisMonthCents, currency, home)
            color = Palette.Green
        }
        state.toPayCents != null -> {
            text = Money.short(state.toPayCents, currency, home)
            color = Palette.Ink
        }
        state.status != Status.DONE && state.bill.estimatedCents != null -> {
            text = "~" + Money.short(state.bill.estimatedCents, currency, home)
            color = Palette.Muted
        }
        else -> {
            text = "—"
            color = Palette.Faint
        }
    }
    Text(text, color = color, fontSize = 16.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
}
