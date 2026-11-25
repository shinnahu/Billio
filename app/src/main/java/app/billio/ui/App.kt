package app.billio.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.billio.AppState
import app.billio.R
import app.billio.Screen
import app.billio.Sheet
import app.billio.Tab
import app.billio.data.Bill
import app.billio.domain.AmountFinder
import app.billio.domain.BillState
import app.billio.domain.Bills
import app.billio.domain.Money
import app.billio.domain.Stats
import app.billio.domain.Status
import app.billio.domain.Summary
import app.billio.notify.Notifier
import app.billio.notify.ReminderScheduler
import java.time.YearMonth

private const val MERCADO_PAGO = "com.mercadopago.wallet"

/** The floating bar, its margin below, and the stretch above it where the page fades out. */
private val NavFadeHeight = 128.dp

/** Empty room at the end of a page that scrolls under the floating bar, so its last row can clear the fade. */
val NavClearance = NavFadeHeight + 8.dp

@Composable
fun BillioRoot(state: AppState) {
    val data by state.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val country = state.country(data.settings)
    val home = Money.currencyOf(country)
    val today = state.today
    val states = remember(data, today) { Bills.states(data, today) }

    BackHandler(enabled = state.sheet != null || state.stack.isNotEmpty() || state.tab != Tab.HOME) { state.back() }

    // ---- reminders and the permission to show them
    var notificationsOn by remember { mutableStateOf(Notifier.allowed(context)) }
    var settingsIfRefused by remember { mutableStateOf(false) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = Notifier.allowed(context)
        // Android stops showing its question after two refusals; then only its settings page can allow them.
        if (!notificationsOn && settingsIfRefused) openNotificationSettings(context)
        settingsIfRefused = false
    }

    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            settingsIfRefused = true
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }
    LifecycleResumeEffect(Unit) {
        notificationsOn = Notifier.allowed(context)
        state.tick()
        onPauseOrDispose { }
    }
    LaunchedEffect(data.settings.reminderHour) { ReminderScheduler.schedule(context, data.settings.reminderHour) }
    val hasBills = data.bills.isNotEmpty()
    LaunchedEffect(hasBills) {
        // Asked once, when the first bill exists and a reminder has a reason to be.
        if (hasBills && !data.settings.askedNotifications) {
            state.setSettings { it.copy(askedNotifications = true) }
            if (Build.VERSION.SDK_INT >= 33 && !Notifier.allowed(context)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---- backup files
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { it.write(state.store.exportJson().toByteArray()) }
            }.isSuccess
            toast(context, if (ok) R.string.backup_saved else R.string.backup_failed)
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrDefault("")
            toast(context, if (state.store.importJson(text)) R.string.backup_restored else R.string.backup_bad)
        }
    }

    val mercadoPago = remember { runCatching { context.packageManager.getLaunchIntentForPackage(MERCADO_PAGO) }.getOrNull() }

    fun copyAccount(bill: Bill) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(bill.name, bill.accountNumber))
        // Android 13 and later show their own "copied" message.
        if (Build.VERSION.SDK_INT < 33) toast(context, R.string.copied)
    }

    /** The one obvious next step for a bill: pay it, or first find out how much it is. */
    fun nextStep(s: BillState) {
        when {
            s.status != Status.PENDING -> state.sheet = Sheet.Pay(s.bill.id)
            s.bill.checkUrl.isBlank() -> state.sheet = Sheet.Amount(s.bill.id)
            else -> state.open(Screen.Browser(s.bill.id))
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        val blur by animateDpAsState(if (state.sheet != null) 10.dp else 0.dp, label = "blur")
        Box(Modifier.fillMaxSize().blur(blur)) {
            when (val top = state.stack.lastOrNull()) {
                null -> Box(Modifier.fillMaxSize()) {
                    when (state.tab) {
                        Tab.HOME -> HomeScreen(
                            name = data.settings.userName,
                            summary = remember(states, data.payments, home, today) { Summary.of(states, data.payments, home, today) },
                            states = states,
                            needsActionOnly = state.needsActionOnly,
                            onFilter = { state.needsActionOnly = it },
                            onBell = { state.sheet = Sheet.Upcoming },
                            onSettings = { state.open(Screen.Settings) },
                            onAdd = { state.sheet = Sheet.Add() },
                            onOpen = { state.open(Screen.Detail(it.id)) },
                            onFocusAction = { s ->
                                when {
                                    s != null -> nextStep(s)
                                    data.bills.isEmpty() -> state.sheet = Sheet.Add()
                                    else -> state.sheet = Sheet.Upcoming
                                }
                            },
                        )
                        Tab.BILLS -> BillsScreen(states, home, { state.open(Screen.Detail(it.id)) }, { state.sheet = Sheet.Add() })
                        Tab.STATS -> StatsScreen(
                            months = remember(data, home, today) { Stats.monthly(data, home, today) },
                            categories = remember(data, home, today) { Stats.byCategory(data, home, YearMonth.from(today)) },
                            averageCents = remember(data, home, today) { Stats.average(data, home, today) },
                            home = home,
                        )
                        Tab.HISTORY -> HistoryScreen(data.payments, data.bills, home) { state.sheet = Sheet.Pay(it.billId, it.id) }
                    }
                    // The page fades into its background behind the floating bar, so rows do not peek out around it.
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .background(Brush.verticalGradient(0f to Palette.Bg.copy(alpha = 0f), 0.36f to Palette.Bg.copy(alpha = 0.92f), 0.5f to Palette.Bg))
                            .navigationBarsPadding().height(NavFadeHeight),
                    )
                    BottomNav(
                        state.tab, { state.tab = it },
                        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
                    )
                }

                is Screen.Detail -> {
                    val s = states.firstOrNull { it.bill.id == top.billId }
                    if (s == null) MissingBill { state.back() } else DetailScreen(
                        state = s,
                        payments = data.payments.filter { it.billId == s.bill.id },
                        home = home,
                        canOpenMercadoPago = mercadoPago != null && s.bill.country == "AR",
                        onBack = { state.back() },
                        onPay = { state.sheet = Sheet.Pay(s.bill.id) },
                        onCheckSite = { state.open(Screen.Browser(s.bill.id)) },
                        onEnterAmount = { state.sheet = Sheet.Amount(s.bill.id) },
                        onMercadoPago = { mercadoPago?.let { runCatching { context.startActivity(it) } } },
                        onEdit = { state.sheet = Sheet.Edit(s.bill.id) },
                        onDelete = { state.sheet = Sheet.Delete(s.bill.id) },
                        onCopyAccount = { copyAccount(s.bill) },
                        onToggleReminders = { on -> state.updateBill(s.bill.copy(remindersOn = on)) },
                        onEditPayment = { state.sheet = Sheet.Pay(it.billId, it.id) },
                    )
                }

                is Screen.Browser -> {
                    val bill = data.bills.firstOrNull { it.id == top.billId }
                    if (bill == null || bill.checkUrl.isBlank()) MissingBill { state.back() } else BrowserScreen(
                        bill = bill,
                        handleBack = state.sheet == null,
                        onClose = { state.back() },
                        onPageText = { text ->
                            state.sheet = Sheet.Capture(
                                bill.id,
                                AmountFinder.amounts(text, bill.currency),
                                AmountFinder.dates(text, bill.currency, today),
                            )
                        },
                        onCopyAccount = { copyAccount(bill) },
                    )
                }

                Screen.Settings -> SettingsScreen(
                    settings = data.settings,
                    country = country,
                    language = stringResource(R.string.language_code),
                    notificationsOn = notificationsOn,
                    onBack = { state.back() },
                    onName = { name -> state.setSettings { it.copy(userName = name) } },
                    onCountry = { c -> state.setSettings { it.copy(country = c) } },
                    onLanguage = { l -> state.setSettings { it.copy(language = l) } },
                    onHour = { h -> state.setSettings { it.copy(reminderHour = h) } },
                    onNotifications = { if (!notificationsOn) allowNotifications() },
                    onTest = { if (Notifier.allowed(context)) Notifier.test(context) else allowNotifications() },
                    onExport = { export.launch("billio-backup-$today.json") },
                    onImport = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
            }
        }

        SheetHost(state.sheet, { state.sheet = null }) { sheet ->
            fun bill(id: String) = data.bills.firstOrNull { it.id == id }
            when (sheet) {
                is Sheet.Add -> AddSheet(sheet, state, data)
                is Sheet.Edit -> bill(sheet.billId)?.let { EditSheet(it, state) }
                is Sheet.Pay -> bill(sheet.billId)?.let { b -> PaySheet(b, data.payments.firstOrNull { it.id == sheet.paymentId }, state, data) }
                is Sheet.Amount -> bill(sheet.billId)?.let { AmountSheet(it, state, data) }
                is Sheet.Capture -> bill(sheet.billId)?.let { CaptureSheet(sheet, it, state) }
                is Sheet.Delete -> bill(sheet.billId)?.let { DeleteSheet(it, state) }
                Sheet.Upcoming -> UpcomingSheet(state, data, home)
            }
        }
    }
}

private fun toast(context: Context, message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
