package app.billio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.AppState
import app.billio.R
import app.billio.Screen
import app.billio.Sheet
import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Category
import app.billio.data.Payment
import app.billio.data.Provider
import app.billio.domain.Bills
import app.billio.domain.Money
import app.billio.domain.Reminders
import app.billio.domain.Upcoming
import java.time.LocalDate
import java.time.YearMonth

/**
 * The floating card of the mock: slides up from the bottom, leaves a margin around it, and the page
 * behind is dimmed. It lives in the same window as the screens, so the page can be blurred under it.
 */
@Composable
fun BoxScope.SheetHost(sheet: Sheet?, onDismiss: () -> Unit, content: @Composable (Sheet) -> Unit) {
    // Keeps the last sheet around so it stays drawn while it slides away.
    val last = remember { arrayOfNulls<Sheet>(1) }
    if (sheet != null) last[0] = sheet
    val shown = sheet ?: last[0]
    val quiet = remember { MutableInteractionSource() }

    AnimatedVisibility(visible = sheet != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
        Box(
            Modifier.fillMaxSize().background(Color(0x59101828))
                .clickable(interactionSource = quiet, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible = sheet != null,
        enter = slideInVertically(tween(260)) { it } + fadeIn(tween(120)),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        val shape = RoundedCornerShape(32.dp)
        Surface(
            shape = shape,
            color = Color.White,
            modifier = Modifier.statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 12.dp).fillMaxWidth()
                .softShadow(16.dp, shape)
                // Taps on the card itself must not fall through to the dimmed page and close it.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            if (shown != null) {
                val scroll = remember(shown) { ScrollState(0) }
                Column(Modifier.verticalScroll(scroll).padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)) {
                    content(shown)
                }
            }
        }
    }
}

/** Close button on the right, optional back button on the left, then title and description. */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit, body: String = "", onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            CircleButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
        }
        Spacer(Modifier.weight(1f))
        CircleButton(Icons.Rounded.Close, stringResource(R.string.close), onClose)
    }
    Spacer(Modifier.height(14.dp))
    Text(title, color = Palette.Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium)
    if (body.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(body, color = Palette.Muted, fontSize = 15.5.sp, lineHeight = 22.sp)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
fun OptionRow(label: String, onClick: () -> Unit, sub: String = "", leading: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp).heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (sub.isNotEmpty()) Text(sub, color = Palette.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(28.dp).clip(CircleShape).background(Palette.Field), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------- connect a provider

@Composable
fun AddSheet(sheet: Sheet.Add, state: AppState, data: AppData) {
    val country = state.country(data.settings)
    val close = { state.sheet = null }
    when {
        sheet.provider != null || sheet.custom -> {
            val provider = sheet.provider
            val template = Bill(
                id = "",
                providerId = provider?.id,
                name = provider?.name.orEmpty(),
                category = provider?.category ?: sheet.category ?: Category.OTHER,
                country = country,
                currency = Money.currencyOf(country),
                accountLabel = provider?.idLabel.orEmpty(),
                reminderDay = 5,
                checkUrl = provider?.url.orEmpty(),
            )
            SheetHeader(
                title = provider?.name ?: stringResource(R.string.other_provider),
                onClose = close,
                body = when (provider?.access) {
                    "login" -> stringResource(R.string.login_note_login)
                    "open" -> stringResource(R.string.login_note_public)
                    else -> ""
                },
                onBack = { state.sheet = Sheet.Add(category = sheet.category) },
            )
            BillForm(template, provider, isNew = true, pickCategory = provider == null && sheet.category == null) { bill ->
                state.addBill(bill)
                state.sheet = null
            }
        }
        sheet.category != null -> {
            SheetHeader(stringResource(sheet.category.label()), close, onBack = { state.sheet = Sheet.Add() })
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.catalog.of(country, sheet.category).forEach { p ->
                    OptionRow(p.name, { state.sheet = sheet.copy(provider = p) }) { Avatar(p.name, size = 36.dp) }
                }
                OptionRow(
                    stringResource(R.string.other_provider), { state.sheet = sheet.copy(custom = true) },
                    sub = stringResource(R.string.other_provider_sub),
                ) { IconBubble(Icons.Rounded.Add, size = 36.dp) }
            }
        }
        else -> {
            var query by remember { mutableStateOf("") }
            // The country is picked in Settings; here it only decides which companies are listed.
            SheetHeader(stringResource(R.string.add_title), close, body = stringResource(R.string.add_body))
            SearchBox(query, { query = it }, stringResource(R.string.search_provider))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (query.isBlank()) {
                    state.catalog.categories(country).forEach { c ->
                        OptionRow(stringResource(c.label()), { state.sheet = Sheet.Add(category = c) }) {
                            Icon(c.icon(), null, tint = Palette.Blue, modifier = Modifier.size(26.dp))
                        }
                    }
                } else {
                    val found = state.catalog.search(country, query)
                    found.forEach { p ->
                        OptionRow(p.name, { state.sheet = Sheet.Add(category = p.category, provider = p) }, sub = stringResource(p.category.label())) {
                            Avatar(p.name, size = 36.dp)
                        }
                    }
                    if (found.isEmpty()) Hint(stringResource(R.string.no_results), Modifier.padding(vertical = 4.dp))
                }
                OptionRow(
                    stringResource(R.string.other_provider), { state.sheet = Sheet.Add(custom = true) },
                    sub = stringResource(R.string.other_provider_sub),
                ) { Icon(Icons.Rounded.Add, null, tint = Palette.Blue, modifier = Modifier.size(26.dp)) }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, placeholder: String) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Palette.Ink, fontSize = 16.5.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Palette.Blue),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Palette.Field).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = Palette.Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Palette.Faint, fontSize = 16.5.sp)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close, stringResource(R.string.clear), tint = Palette.Muted,
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onChange("") },
                    )
                }
            }
        },
    )
}

/** The details of a bill, for a new one and for editing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillForm(initial: Bill, provider: Provider?, isNew: Boolean, pickCategory: Boolean = false, onSave: (Bill) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var account by remember { mutableStateOf(initial.accountNumber) }
    var day by remember { mutableStateOf(initial.reminderDay) }
    var usual by remember { mutableStateOf(initial.estimatedCents?.let { Money.editable(it, initial.currency) }.orEmpty()) }
    var cuotas by remember { mutableStateOf(initial.cuotasTotal?.toString().orEmpty()) }
    var url by remember { mutableStateOf(initial.checkUrl) }
    var notes by remember { mutableStateOf(initial.notes) }
    var more by remember { mutableStateOf(!isNew) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (provider == null || !isNew) {
            Field(stringResource(R.string.field_name), name, { name = it }, capitalize = true, placeholder = stringResource(R.string.name_placeholder))
        }
        if (pickCategory || (!isNew && provider == null)) {
            Column {
                Label(stringResource(R.string.field_category))
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c -> Chip(stringResource(c.label()), c == category, { category = c }) }
                }
            }
        }
        Field(
            label = initial.accountLabel.ifBlank { stringResource(R.string.field_account) },
            value = account,
            onValueChange = { account = it },
            placeholder = provider?.idHint.orEmpty(),
            hint = stringResource(R.string.account_hint),
            keyboard = KeyboardType.Ascii,
        )
        DayPicker(stringResource(R.string.field_reminder_day), day, { day = it }, hint = stringResource(R.string.reminder_day_hint, day))
        Field(
            stringResource(R.string.field_usual_amount), usual, { usual = it },
            hint = stringResource(R.string.usual_hint), keyboard = KeyboardType.Decimal, prefix = Money.symbol(initial.currency),
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { more = !more }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.more_options), color = Palette.Blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Icon(if (more) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Palette.Blue, modifier = Modifier.size(22.dp))
        }
        if (more) {
            Field(
                stringResource(R.string.field_cuotas), cuotas, { cuotas = it.filter(Char::isDigit).take(3) },
                hint = stringResource(R.string.cuotas_hint), keyboard = KeyboardType.Number,
            )
            Field(
                stringResource(R.string.field_site), url, { url = it.trim() },
                hint = stringResource(R.string.site_hint), keyboard = KeyboardType.Uri, placeholder = "https://",
            )
            Field(stringResource(R.string.field_notes), notes, { notes = it }, capitalize = true, singleLine = false)
        }
        PrimaryButton(
            stringResource(if (isNew) R.string.action_add_bill else R.string.action_save),
            enabled = name.isNotBlank(),
            onClick = {
                onSave(
                    initial.copy(
                        name = name.trim(),
                        category = category,
                        accountNumber = account.trim(),
                        reminderDay = day,
                        estimatedCents = Money.parse(usual, initial.currency)?.takeIf { it > 0 },
                        cuotasTotal = cuotas.toIntOrNull()?.takeIf { it > 0 },
                        checkUrl = normaliseUrl(url),
                        notes = notes.trim(),
                    ),
                )
            },
        )
    }
}

/** "edenor.com" becomes "https://edenor.com"; anything that is not a web address is dropped. */
fun normaliseUrl(text: String): String {
    val t = text.trim()
    return when {
        t.isEmpty() -> ""
        t.startsWith("https://", true) || t.startsWith("http://", true) -> t
        t.contains('.') && !t.contains(' ') -> "https://$t"
        else -> ""
    }
}

@Composable
fun EditSheet(bill: Bill, state: AppState) {
    SheetHeader(stringResource(R.string.edit_title), { state.sheet = null })
    BillForm(bill, state.catalog.byId(bill.providerId), isNew = false) {
        state.updateBill(it)
        state.sheet = null
    }
}

// ---------------------------------------------------------------------------------------- payments and amounts

@Composable
fun PaySheet(bill: Bill, existing: Payment?, state: AppState, data: AppData) {
    val current = remember(bill.id) { Bills.state(bill, data, state.today) }
    var amount by remember {
        mutableStateOf((existing?.amountCents ?: current.due?.amountCents ?: bill.estimatedCents)?.let { Money.editable(it, bill.currency) }.orEmpty())
    }
    var paidOn by remember { mutableStateOf(existing?.let { Bills.date(it.paidOn) } ?: state.today) }
    var period by remember { mutableStateOf(existing?.let { Bills.period(it.period) } ?: current.period) }
    var isCuota by remember { mutableStateOf(existing?.cuota != null || (existing == null && bill.cuotasTotal != null)) }
    var cuota by remember { mutableStateOf((existing?.cuota ?: current.due?.cuota ?: current.nextCuota)?.toString().orEmpty()) }
    var total by remember { mutableStateOf((existing?.cuotaTotal ?: bill.cuotasTotal)?.toString().orEmpty()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    val cents = Money.parse(amount, bill.currency)?.takeIf { it > 0 }

    SheetHeader(
        stringResource(if (existing == null) R.string.pay_title else R.string.pay_edit_title),
        { state.sheet = null }, body = bill.name,
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Field(
            stringResource(R.string.field_amount_paid), amount, { amount = it },
            keyboard = KeyboardType.Decimal, prefix = Money.symbol(bill.currency),
            placeholder = Money.editable(4523010, bill.currency),
        )
        DateField(stringResource(R.string.field_paid_on), paidOn, { if (it != null) paidOn = it })
        MonthStepper(stringResource(R.string.field_period), period, { period = it })
        // The whole line switches it, not only the small switch.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .toggleable(value = isCuota, role = Role.Switch, onValueChange = { isCuota = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cuota_toggle), color = Palette.Ink, fontSize = 16.sp)
                Hint(stringResource(R.string.cuota_toggle_hint))
            }
            Spacer(Modifier.width(12.dp))
            Toggle(isCuota, null)
        }
        if (isCuota) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(
                    stringResource(R.string.field_cuota), cuota, { cuota = it.filter(Char::isDigit).take(3) },
                    keyboard = KeyboardType.Number, modifier = Modifier.weight(1f),
                )
                Field(
                    stringResource(R.string.field_cuota_of), total, { total = it.filter(Char::isDigit).take(3) },
                    keyboard = KeyboardType.Number, modifier = Modifier.weight(1f),
                )
            }
        }
        Field(stringResource(R.string.field_note), note, { note = it }, capitalize = true, placeholder = stringResource(R.string.optional))
        PrimaryButton(
            stringResource(R.string.action_save_payment),
            enabled = cents != null,
            icon = Icons.Rounded.Check,
            onClick = {
                state.savePayment(
                    Payment(
                        id = existing?.id.orEmpty(),
                        billId = bill.id,
                        period = Bills.key(period),
                        amountCents = cents ?: 0,
                        paidOn = paidOn.toString(),
                        cuota = cuota.toIntOrNull().takeIf { isCuota },
                        cuotaTotal = total.toIntOrNull().takeIf { isCuota },
                        note = note.trim(),
                    ),
                )
                state.sheet = null
            },
        )
        if (existing != null) {
            SoftButton(stringResource(R.string.action_delete_payment), {
                state.deletePayment(existing.id)
                state.sheet = null
            }, color = Palette.Red)
        }
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Palette.Blue,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Palette.Faint, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Typing this month's amount and due date by hand. */
@Composable
fun AmountSheet(bill: Bill, state: AppState, data: AppData) {
    val current = remember(bill.id) { Bills.state(bill, data, state.today) }
    var period by remember { mutableStateOf(current.period) }
    val saved = data.dues.firstOrNull { it.billId == bill.id && it.period == Bills.key(period) }
    var amount by remember(period) { mutableStateOf(saved?.let { Money.editable(it.amountCents, bill.currency) }.orEmpty()) }
    var dueDate by remember(period) { mutableStateOf(Bills.date(saved?.dueDate)) }
    val cents = Money.parse(amount, bill.currency)?.takeIf { it > 0 }

    SheetHeader(stringResource(R.string.amount_title), { state.sheet = null }, body = bill.name)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Field(
            stringResource(R.string.field_amount_due), amount, { amount = it },
            keyboard = KeyboardType.Decimal, prefix = Money.symbol(bill.currency),
            placeholder = Money.editable(4523010, bill.currency),
        )
        DateField(
            stringResource(R.string.field_due_date), dueDate, { dueDate = it },
            placeholder = stringResource(R.string.optional), clearable = true,
        )
        MonthStepper(stringResource(R.string.field_period), period, { period = it })
        PrimaryButton(stringResource(R.string.action_save_amount), enabled = cents != null, onClick = {
            state.setDue(bill.id, period, cents ?: 0, dueDate)
            state.sheet = null
        })
        if (saved != null) {
            SoftButton(stringResource(R.string.action_remove_amount), {
                state.clearDue(bill.id, period)
                state.sheet = null
            }, color = Palette.Red)
        }
    }
}

/** What the app found on the provider page: the person picks which amount is the bill. */
@Composable
fun CaptureSheet(sheet: Sheet.Capture, bill: Bill, state: AppState) {
    val locale = appLocale()
    val close = { state.sheet = null }
    if (sheet.amounts.isEmpty()) {
        SheetHeader(stringResource(R.string.capture_none_title), close, body = stringResource(R.string.capture_none_body))
        PrimaryButton(stringResource(R.string.action_enter_amount), { state.sheet = Sheet.Amount(bill.id) })
        return
    }
    var amount by remember { mutableStateOf(sheet.amounts.first().cents) }
    var date by remember { mutableStateOf(sheet.dates.firstOrNull()?.takeIf { it.score >= 4 }?.date) }
    var period by remember { mutableStateOf(YearMonth.from(state.today)) }

    SheetHeader(stringResource(R.string.capture_title), close, body = stringResource(R.string.capture_body))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sheet.amounts.forEach { hit ->
            ChoiceRow(Money.format(hit.cents, bill.currency), hit.context, hit.cents == amount) { amount = hit.cents }
        }
    }
    if (sheet.dates.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Label(stringResource(R.string.field_due_date))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sheet.dates.forEach { hit ->
                ChoiceRow(Fmt.full(hit.date, locale), hit.context, hit.date == date) { date = hit.date }
            }
            ChoiceRow(stringResource(R.string.capture_no_date), "", date == null) { date = null }
        }
    }
    Spacer(Modifier.height(18.dp))
    MonthStepper(stringResource(R.string.field_period), period, { period = it })
    Spacer(Modifier.height(18.dp))
    PrimaryButton(stringResource(R.string.action_use_amount), {
        state.setDue(bill.id, period, amount, date, fromWeb = true)
        state.sheet = null
        if (state.stack.lastOrNull() == Screen.Browser(bill.id)) state.back()
    })
}

@Composable
private fun ChoiceRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Palette.BlueSoft else Color.White,
        border = BorderStroke(1.dp, if (selected) Palette.Blue else Palette.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Palette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                if (sub.isNotEmpty()) Text(sub, color = Palette.Muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Icon(Icons.Rounded.Check, null, tint = Palette.Blue, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun DeleteSheet(bill: Bill, state: AppState) {
    SheetHeader(stringResource(R.string.delete_title, bill.name), { state.sheet = null }, body = stringResource(R.string.delete_body))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            onClick = { state.deleteBill(bill.id); state.sheet = null },
            shape = RoundedCornerShape(16.dp), color = Palette.Red, contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.action_delete), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        SoftButton(stringResource(R.string.action_cancel), { state.sheet = null })
    }
}

// ---------------------------------------------------------------------------------------- the bell

@Composable
fun UpcomingSheet(state: AppState, data: AppData, home: String) {
    val locale = appLocale()
    val list: List<Upcoming> = remember(data, state.today) { Reminders.upcoming(state.today, data) }
    SheetHeader(stringResource(R.string.upcoming_title), { state.sheet = null }, body = stringResource(R.string.upcoming_body))
    if (list.isEmpty()) {
        Hint(stringResource(R.string.upcoming_empty))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        list.forEach { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { state.open(Screen.Detail(item.bill.id)) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(item.bill.category.icon(), size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.bill.name, color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        relativeDay(item.date, state.today, locale) + " · " +
                            stringResource(if (item.isDueDate) R.string.kind_due else R.string.kind_reminder),
                        color = if (item.isDueDate) Palette.Orange else Palette.Muted, fontSize = 13.5.sp,
                    )
                }
                item.amountCents?.let {
                    Text(Money.short(it, item.bill.currency, home), color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun relativeDay(date: LocalDate, today: LocalDate, locale: java.util.Locale): String = when (date) {
    today -> stringResource(R.string.today)
    today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> Fmt.day(date, locale)
}
