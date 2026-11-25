package app.billio

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.billio.data.AppData
import app.billio.data.Bill
import app.billio.data.Category
import app.billio.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The app language picked in Settings, on the real screen of the app (the other screen tests draw the
 * screens directly, without the part that applies the language).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w412dp-h915dp-xhdpi")
class LanguageTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: BillioApp get() = ApplicationProvider.getApplicationContext()

    private fun save(language: String) = app.store.update {
        AppData(
            settings = Settings(userName = "Nahuel", country = "AR", language = language, askedNotifications = true),
            bills = listOf(Bill(id = "edenor", name = "Edenor", category = Category.ELECTRICITY, country = "AR", currency = "ARS", reminderDay = 20)),
        )
    }

    private fun screen(): Activity =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()

    private fun shot(name: String) {
        compose.waitForIdle()
        val root = screen().findViewById<View>(android.R.id.content)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val out = File("build/screenshots/$name.png")
        out.parentFile!!.mkdirs()
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("Saved ${out.absolutePath}")
    }

    @Test
    fun withNoLanguagePickedTheAppFollowsThePhone() {
        save(language = "")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Hello, Nahuel").assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "es-rAR-w412dp-h915dp-xhdpi")
    fun withNoLanguagePickedASpanishPhoneGetsSpanish() {
        save(language = "")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Hola, Nahuel").assertIsDisplayed()
        }
    }

    @Test
    fun spanishSavedInSettingsIsUsedOnAnEnglishPhone() {
        save(language = "es")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Hola, Nahuel").assertIsDisplayed()
            compose.onNodeWithText("A pagar este mes").assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "es-rAR-w412dp-h915dp-xhdpi")
    fun englishSavedInSettingsIsUsedOnASpanishPhone() {
        save(language = "en")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Hello, Nahuel").assertIsDisplayed()
            compose.onNodeWithText("To pay this month").assertIsDisplayed()
        }
    }

    @Test
    fun pickingSpanishInSettingsTurnsTheWholeAppSpanishAndBack() {
        save(language = "")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("App language").assertIsDisplayed()

            compose.onNodeWithText("Español").performClick()
            compose.waitForIdle()
            // Still on Settings, now in Spanish.
            compose.onNodeWithText("Ajustes").assertIsDisplayed()
            compose.onNodeWithText("Idioma de la app").assertIsDisplayed()
            compose.onNodeWithText("Estados Unidos").assertIsDisplayed()
            assertEquals("es", app.store.data.value.settings.language)
            assertEquals(0, compose.onAllNodesWithText("Settings").fetchSemanticsNodes().size)
            shot("17-settings-es")

            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithText("Hola, Nahuel").assertIsDisplayed()

            compose.onNodeWithContentDescription("Ajustes").performClick()
            compose.onNodeWithText("English").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Settings").assertIsDisplayed()
            compose.onNodeWithText("App language").assertIsDisplayed()
            assertEquals("en", app.store.data.value.settings.language)
        }
    }
}
