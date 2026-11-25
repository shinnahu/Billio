package app.billio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.Language
import app.billio.R
import app.billio.data.Settings

@Composable
fun SettingsScreen(
    settings: Settings,
    country: String,
    /** The language the screens are in right now: "en" or "es". */
    language: String,
    notificationsOn: Boolean,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onCountry: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onHour: (Int) -> Unit,
    onNotifications: () -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    StatusBarIcons(white = false)
    var name by remember { mutableStateOf(settings.userName) }
    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        TopBar(stringResource(R.string.settings_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Field(
                        stringResource(R.string.field_your_name), name,
                        { name = it.take(30); onName(name) },
                        hint = stringResource(R.string.name_hint), capitalize = true,
                    )
                    Column {
                        Label(stringResource(R.string.settings_country))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(stringResource(R.string.country_ar), country == "AR", { onCountry("AR") })
                            Chip(stringResource(R.string.country_us), country == "US", { onCountry("US") })
                        }
                        Spacer(Modifier.height(6.dp))
                        Hint(stringResource(R.string.country_hint))
                    }
                    Column {
                        Label(stringResource(R.string.settings_language))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(stringResource(R.string.language_en), language == Language.ENGLISH, { onLanguage(Language.ENGLISH) })
                            Chip(stringResource(R.string.language_es), language == Language.SPANISH, { onLanguage(Language.SPANISH) })
                        }
                    }
                }
            }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.settings_reminders), color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium)
                    Column {
                        Label(stringResource(R.string.settings_reminder_time))
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Field).padding(horizontal = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircleButton(
                                Icons.Rounded.Remove, stringResource(R.string.earlier),
                                { onHour((settings.reminderHour + 23) % 24) }, background = Color.White,
                            )
                            Text(
                                "%02d:00".format(settings.reminderHour), color = Palette.Ink, fontSize = 17.sp,
                                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                            )
                            CircleButton(
                                Icons.Rounded.Add, stringResource(R.string.later),
                                { onHour((settings.reminderHour + 1) % 24) }, background = Color.White,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Hint(stringResource(R.string.reminder_time_hint))
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onNotifications),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Label(stringResource(R.string.settings_notifications))
                            Text(
                                stringResource(if (notificationsOn) R.string.notif_on else R.string.notif_off),
                                color = if (notificationsOn) Palette.Green else Palette.Red, fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    SoftButton(stringResource(R.string.action_test_notification), onTest)
                }
            }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.settings_backup), color = Palette.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Medium)
                    Hint(stringResource(R.string.settings_privacy))
                    Spacer(Modifier.height(2.dp))
                    SoftButton(stringResource(R.string.action_export), onExport)
                    SoftButton(stringResource(R.string.action_import), onImport)
                }
            }
        }
    }
}
