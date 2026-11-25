package app.billio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.billio.data.Catalog
import app.billio.notify.Notifier
import app.billio.ui.BillioRoot
import app.billio.ui.BillioTheme
import java.util.Locale
import java.util.TimeZone

class AppViewModel(app: BillioApp) : ViewModel() {
    val state = AppState(
        store = app.store,
        catalog = Catalog.load(app),
        deviceCountry = guessCountry(app),
    )

    /** The language choice the screen was last restarted for, so it is never restarted twice for the same one. */
    var restartedFor: String? = null

    init {
        // Remember the guess, so the reminder alarm uses the same country as the screens.
        if (state.store.data.value.settings.country.isEmpty()) {
            state.setSettings { it.copy(country = guessCountry(app)) }
        }
    }

    private companion object {
        /** Argentina or United States, from the mobile network, then the time zone, then the phone's region. */
        fun guessCountry(context: Context): String {
            val network = runCatching {
                context.getSystemService(TelephonyManager::class.java)?.networkCountryIso.orEmpty().uppercase()
            }.getOrDefault("")
            if (network == "AR" || network == "US") return network
            if (TimeZone.getDefault().id.startsWith("America/Argentina")) return "AR"
            return if (Locale.getDefault().country == "US") "US" else "AR"
        }
    }
}

class MainActivity : ComponentActivity() {
    // Built by hand, so it always works on the data of this screen's own application.
    private val model: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(application as BillioApp) } }
    }

    /** Every text of the screen comes from here, so this is where the language picked in Settings is applied. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Language.wrap(newBase, Language.chosen(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Its name is shown in the phone's notification settings; this keeps it in the app's language.
        Notifier.ensureChannel(this)
        if (savedInstanceState == null) openFromNotification(intent)
        setContent {
            val data by model.state.data.collectAsStateWithLifecycle()
            val language = data.settings.language
            LaunchedEffect(language) { showIn(language) }
            BillioTheme { BillioRoot(model.state) }
        }
    }

    /**
     * Texts are loaded when the screen starts, so a language other than the one on screen starts it again.
     * What is open stays open: that lives in the view model, not in the screen.
     */
    private fun showIn(language: String) {
        val wanted = Language.shown(Language.wrap(applicationContext, language))
        if (wanted == Language.shown(this) || model.restartedFor == language) return
        model.restartedFor = language
        window.decorView.post { recreate() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }

    override fun onResume() {
        super.onResume()
        model.state.tick()
    }

    private fun openFromNotification(intent: Intent?) {
        val billId = intent?.getStringExtra(Notifier.EXTRA_BILL) ?: return
        intent.removeExtra(Notifier.EXTRA_BILL)
        model.state.openFromNotification(billId, pay = intent.getBooleanExtra(Notifier.EXTRA_PAY, false))
    }
}
