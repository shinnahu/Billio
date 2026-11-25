package app.billio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Catalog
import app.billio.data.Category
import app.billio.data.Due
import app.billio.data.Payment
import app.billio.data.Settings
import app.billio.data.Store
import app.billio.domain.AmountFinder
import app.billio.ui.BillioRoot
import app.billio.ui.BillioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Runs the real screens on a simulated phone: walks through the main flows and saves a picture of each
 * screen under build/screenshots, to compare with the design.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w412dp-h915dp-xhdpi")
class ScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 11, 25)

    private fun bill(id: String, name: String, category: Category, day: Int, provider: String? = null, account: String = "", url: String = "", estimate: Long? = null) =
        Bill(
            id = id, providerId = provider, name = name, category = category, country = "AR", currency = "ARS",
            accountNumber = account, reminderDay = day, checkUrl = url, estimatedCents = estimate, createdOn = "2026-08-01",
        )

    private fun sample() = AppData(
        settings = Settings(userName = "Nahuel", country = "AR", askedNotifications = true),
        bills = listOf(
            bill("edenor", "Edenor", Category.ELECTRICITY, 20, "ar-edenor", "1234567890", "https://www.edenordigital.com/").copy(accountLabel = "N° de cuenta"),
            bill("telecentro", "Telecentro", Category.INTERNET, 5, "ar-telecentro", "", "https://telecentro.com.ar/sucursal-virtual/facturacion", 3899900),
            bill("naturgy", "Naturgy", Category.GAS, 22, "ar-naturgy-ban", "654321", "https://ov.naturgy.com.ar/ingreso", 2150000),
            bill("aysa", "AySA", Category.WATER, 28, "ar-aysa", "", "https://www.aysa.com.ar/", 1420000),
        ),
        dues = listOf(Due("edenor", "2026-11", 4523010, "2026-11-28")),
        payments = listOf(
            Payment("p1", "telecentro", "2026-11", 3899900, "2026-11-05"),
            Payment("p2", "telecentro", "2026-10", 3650000, "2026-10-06"),
            Payment("p3", "edenor", "2026-10", 4120000, "2026-10-21"),
            Payment("p4", "naturgy", "2026-10", 2150000, "2026-10-23", cuota = 2, cuotaTotal = 2),
            Payment("p5", "aysa", "2026-10", 1420000, "2026-10-28"),
            Payment("p6", "edenor", "2026-09", 3980000, "2026-09-22"),
            Payment("p7", "telecentro", "2026-09", 3650000, "2026-09-05"),
            Payment("p8", "naturgy", "2026-09", 2150000, "2026-09-23", cuota = 1, cuotaTotal = 2),
            Payment("p9", "edenor", "2026-08", 3550000, "2026-08-21"),
            Payment("p10", "telecentro", "2026-08", 3400000, "2026-08-05"),
        ),
    )

    private fun start(data: AppData = sample()): AppState {
        val file = File.createTempFile("bills", ".json").also { it.delete() }
        val store = Store(file)
        store.update { data }
        val state = AppState(store, Catalog.load(context), clock = { today }, deviceCountry = "AR")
        compose.setContent { BillioTheme { BillioRoot(state) } }
        compose.waitForIdle()
        return state
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val root = compose.activity.findViewById<View>(android.R.id.content)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        save(bitmap, name)
    }

    private fun save(bitmap: Bitmap, name: String) {
        val out = File("build/screenshots/$name.png")
        out.parentFile!!.mkdirs()
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("Saved ${out.absolutePath}")
    }

    // ------------------------------------------------------------------ pictures

    @Test
    fun home() {
        start()
        compose.onNodeWithText("Hello, Nahuel").assertIsDisplayed()
        compose.onNodeWithText("To pay this month").assertIsDisplayed()
        shot("01-home")
    }

    @Test
    @Config(qualifiers = "es-rAR-w412dp-h915dp-xhdpi")
    fun homeInSpanish() {
        start()
        compose.onNodeWithText("Hola, Nahuel").assertIsDisplayed()
        shot("02-home-es")
    }

    @Test
    fun emptyHome() {
        start(AppData(settings = Settings(country = "AR", askedNotifications = true)))
        compose.onNodeWithText("Connect your first provider").assertIsDisplayed()
        shot("03-home-empty")
    }

    @Test
    fun connectAProvider() {
        val state = start()
        // With four bills the button sits behind the floating bar until the page is scrolled, as in the design.
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("Add a new bill").performClick()
        compose.onNodeWithText("Connect a Provider").assertIsDisplayed()
        // The country is neither named nor offered here; it lives in Settings.
        assertEquals(0, compose.onAllNodesWithText("Argentina", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("United States", substring = true).fetchSemanticsNodes().size)
        shot("04-connect-categories")
        compose.onNodeWithText("Electricity").performClick()
        compose.onNodeWithText("Edesur").assertIsDisplayed()
        shot("05-connect-companies")
        compose.onNodeWithText("Edesur").performClick()
        shot("06-connect-details")
        assertTrue(state.sheet is Sheet.Add)
    }

    @Test
    fun otherScreens() {
        val state = start()
        state.open(Screen.Detail("edenor"))
        shot("07-bill")
        state.sheet = Sheet.Pay("edenor")
        shot("08-log-payment")
        state.sheet = Sheet.Amount("naturgy")
        shot("09-enter-amount")
        state.sheet = Sheet.Upcoming
        shot("10-coming-up")
        state.sheet = null
        state.stack.clear()
        state.tab = Tab.BILLS
        shot("11-your-bills")
        state.tab = Tab.STATS
        shot("12-statistics")
        state.tab = Tab.HISTORY
        shot("13-history")
        state.open(Screen.Settings)
        shot("14-settings")
        state.stack.clear()
        state.open(Screen.Browser("edenor"))
        shot("15-company-site")
        val page = "Total a pagar $ 45.230,10\nVencimiento 10/12/2026\nCargo fijo $ 3.210,55"
        state.sheet = Sheet.Capture("edenor", AmountFinder.amounts(page, "ARS"), AmountFinder.dates(page, "ARS", today))
        shot("16-amounts-found")
    }

    /**
     * The two screens of the presentation picture in the mockup folder, drawn the way a phone shows them:
     * with room for the status bar on top and the gesture bar at the bottom. The bills are the three
     * companies the app was first made for.
     */
    @Test
    @Config(qualifiers = "en-rUS-w440dp-h945dp-xxhdpi")
    fun screensForTheMockup() {
        val three = sample().let { all ->
            all.copy(bills = all.bills.filterNot { it.id == "aysa" }, payments = all.payments.filterNot { it.billId == "aysa" })
        }
        val state = start(three)
        systemBars(topDp = 54, bottomDp = 16)
        compose.onNodeWithText("Hello, Nahuel").assertIsDisplayed()
        photo("mockup-home")
        state.sheet = Sheet.Add()
        compose.waitForIdle()
        compose.onNodeWithText("Connect a Provider").assertIsDisplayed()
        photo("mockup-connect")
    }

    /** Like [shot], but taken off the screen the way a phone draws it, so shadows and blur are in the picture. */
    private fun photo(name: String) {
        compose.waitForIdle()
        val window = compose.activity.window
        val bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        var result = -1
        PixelCopy.request(window, bitmap, { result = it }, Handler(Looper.getMainLooper()))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(PixelCopy.SUCCESS, result)
        save(bitmap, name)
    }

    /** Tells the screens how much room the phone's own bars take, as a real phone does. */
    private fun systemBars(topDp: Int, bottomDp: Int) {
        val density = compose.activity.resources.displayMetrics.density
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, (topDp * density).toInt(), 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, (bottomDp * density).toInt()))
            .build()
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        compose.runOnUiThread { ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets) }
        compose.waitForIdle()
    }

    // ------------------------------------------------------------------ flows

    @Test
    fun addingACompanyFromTheCatalogCreatesTheBill() {
        val state = start(AppData(settings = Settings(country = "AR", askedNotifications = true)))
        compose.onNodeWithText("Connect a provider").performClick()
        compose.onNodeWithText("Electricity").performClick()
        compose.onNodeWithText("Edenor").performClick()
        compose.onNodeWithText("Its site lets you pay without registering: it asks for this number.").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("1234567890")
        compose.onNodeWithText("Add bill").performScrollTo().performClick()
        compose.waitForIdle()

        val bill = state.data.value.bills.single()
        assertEquals("Edenor", bill.name)
        assertEquals("ar-edenor", bill.providerId)
        assertEquals(Category.ELECTRICITY, bill.category)
        assertEquals("1234567890", bill.accountNumber)
        assertEquals("N° de cuenta", bill.accountLabel)
        assertEquals("ARS", bill.currency)
        assertEquals("https://www.edenordigital.com/", bill.checkUrl)
        assertNull(state.sheet)
        compose.onAllNodesWithText("Edenor")[0].assertIsDisplayed()
    }

    @Test
    fun addingACompanyThatIsNotInTheCatalog() {
        val state = start(AppData(settings = Settings(country = "AR", askedNotifications = true)))
        state.sheet = Sheet.Add(custom = true)
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Expensas")
        compose.onNodeWithText("Rent and building fees").performClick()
        compose.onNodeWithText("Add bill").performScrollTo().performClick()
        compose.waitForIdle()
        val bill = state.data.value.bills.single()
        assertEquals("Expensas", bill.name)
        assertEquals(Category.HOUSING, bill.category)
        assertNull(bill.providerId)
    }

    @Test
    fun searchFindsCompaniesWithoutAccents() {
        start(AppData(settings = Settings(country = "AR", askedNotifications = true)))
        compose.onNodeWithText("Connect a provider").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("cordoba")
        compose.onNodeWithText("EPEC (Córdoba)").assertIsDisplayed()
    }

    @Test
    fun theCountryPickedInSettingsDecidesWhichCompaniesAreOffered() {
        val state = start(AppData(settings = Settings(country = "AR", askedNotifications = true)))
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("United States").performClick()
        compose.waitForIdle()
        assertEquals("US", state.data.value.settings.country)
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Connect a provider").performClick()
        assertEquals(0, compose.onAllNodesWithText("United States", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Home security").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Internet & TV").performScrollTo().performClick()
        compose.onNodeWithText("Xfinity (Comcast)").performClick()
        compose.onNodeWithText("Add bill").performScrollTo().performClick()
        compose.waitForIdle()
        val bill = state.data.value.bills.single()
        assertEquals("US", bill.country)
        assertEquals("USD", bill.currency)
    }

    @Test
    fun settingsOfferEnglishAndSpanishAndKeepTheChoice() {
        val state = start()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("App language").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("English").assertIsDisplayed()
        assertEquals("", state.data.value.settings.language) // nothing picked yet: the phone's language is used
        compose.onNodeWithText("Español").performClick()
        compose.waitForIdle()
        assertEquals("es", state.data.value.settings.language)
        compose.onNodeWithText("English").performClick()
        compose.waitForIdle()
        assertEquals("en", state.data.value.settings.language)
    }

    @Test
    fun loggingAPaymentMarksTheMonthAsPaid() {
        val state = start()
        compose.onNodeWithText("Log payment").performClick() // the card on top is Edenor, the bill to pay
        compose.onNodeWithText("Log a payment").assertIsDisplayed()
        // The amount comes filled in with this month's bill; change it to what was really paid.
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("45.230,10")
        compose.onNodeWithText("Save payment").performScrollTo().performClick()
        compose.waitForIdle()

        val payment = state.data.value.payments.first { it.billId == "edenor" && it.period == "2026-11" }
        assertEquals(4523010L, payment.amountCents)
        assertEquals("2026-11-25", payment.paidOn)
        assertNull(payment.cuota)
        assertEquals(4523010L, state.bill("edenor")!!.estimatedCents)
        assertNull(state.sheet)
    }

    @Test
    fun aPaymentCanBeAnInstalment() {
        val state = start()
        state.sheet = Sheet.Pay("naturgy")
        compose.waitForIdle()
        compose.onNodeWithText("It is an instalment").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("1")
        compose.onAllNodes(hasSetTextAction())[2].performTextInput("2")
        compose.onNodeWithText("Save payment").performScrollTo().performClick()
        compose.waitForIdle()
        val payment = state.data.value.payments.first { it.billId == "naturgy" && it.period == "2026-11" }
        assertEquals(1, payment.cuota)
        assertEquals(2, payment.cuotaTotal)
        assertEquals(2150000L, payment.amountCents) // the usual amount was offered and accepted
    }

    @Test
    fun enteringTheAmountByHandMakesTheBillDue() {
        val state = start()
        state.sheet = Sheet.Amount("naturgy")
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("21.800")
        compose.onNodeWithText("Save amount").performScrollTo().performClick()
        compose.waitForIdle()
        val due = state.data.value.dues.single { it.billId == "naturgy" }
        assertEquals(2180000L, due.amountCents)
        assertEquals("2026-11", due.period)
        assertNull(due.dueDate)
    }

    @Test
    fun amountReadFromTheCompanyPageIsSavedAndTheSiteCloses() {
        val state = start()
        state.open(Screen.Detail("naturgy"))
        state.open(Screen.Browser("naturgy"))
        val page = "Saldo a pagar $ 21.800,50\nVencimiento 09/12/2026"
        state.sheet = Sheet.Capture("naturgy", AmountFinder.amounts(page, "ARS"), AmountFinder.dates(page, "ARS", today))
        compose.waitForIdle()
        compose.onNodeWithText("$21.800,50").assertIsDisplayed()
        compose.onNodeWithText("Save as amount to pay").performScrollTo().performClick()
        compose.waitForIdle()
        val due = state.data.value.dues.single { it.billId == "naturgy" }
        assertEquals(2180050L, due.amountCents)
        assertEquals("2026-12-09", due.dueDate)
        assertTrue(due.fromWeb)
        assertEquals(Screen.Detail("naturgy"), state.stack.last())
    }

    @Test
    fun deletingABillRemovesItsPaymentsToo() {
        val state = start()
        state.open(Screen.Detail("telecentro"))
        state.sheet = Sheet.Delete("telecentro")
        compose.waitForIdle()
        compose.onAllNodesWithText("Delete")[0].performClick()
        compose.waitForIdle()
        assertNull(state.bill("telecentro"))
        assertTrue(state.data.value.payments.none { it.billId == "telecentro" })
        assertTrue(state.stack.isEmpty())
        assertNotNull(state.bill("edenor"))
    }

    @Test
    fun needsActionFilterHidesWhatIsAlreadyPaid() {
        start()
        compose.onNodeWithText("Needs action").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithText("Telecentro").fetchSemanticsNodes().size) // paid on the 5th
        assertTrue(compose.onAllNodesWithText("Edenor").fetchSemanticsNodes().isNotEmpty())
    }
}
