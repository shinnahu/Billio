package app.billio.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.billio.R
import app.billio.data.Category
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------------------- categories

fun Category.icon(): ImageVector = when (this) {
    Category.INTERNET -> Icons.Rounded.Wifi
    Category.MOBILE -> Icons.Rounded.Smartphone
    Category.ELECTRICITY -> Icons.Rounded.Bolt
    Category.GAS -> Icons.Rounded.LocalFireDepartment
    Category.WATER -> Icons.Rounded.WaterDrop
    Category.TAXES -> Icons.Rounded.AccountBalance
    Category.INSURANCE -> Icons.Rounded.Shield
    Category.HEALTH -> Icons.Rounded.MedicalServices
    Category.SUBSCRIPTION -> Icons.Rounded.Subscriptions
    Category.CARD -> Icons.Rounded.CreditCard
    Category.SECURITY -> Icons.Rounded.Lock
    Category.EDUCATION -> Icons.Rounded.School
    Category.HOUSING -> Icons.Rounded.Home
    Category.OTHER -> Icons.Rounded.Receipt
}

@StringRes
fun Category.label(): Int = when (this) {
    Category.INTERNET -> R.string.cat_internet
    Category.MOBILE -> R.string.cat_mobile
    Category.ELECTRICITY -> R.string.cat_electricity
    Category.GAS -> R.string.cat_gas
    Category.WATER -> R.string.cat_water
    Category.TAXES -> R.string.cat_taxes
    Category.INSURANCE -> R.string.cat_insurance
    Category.HEALTH -> R.string.cat_health
    Category.SUBSCRIPTION -> R.string.cat_subscription
    Category.CARD -> R.string.cat_card
    Category.SECURITY -> R.string.cat_security
    Category.EDUCATION -> R.string.cat_education
    Category.HOUSING -> R.string.cat_housing
    Category.OTHER -> R.string.cat_other
}

// ---------------------------------------------------------------------------------------- dates as text

/**
 * The locale for dates and month names: the one that goes with the language the texts are in, so a date
 * never comes out in another language than the sentence around it.
 */
@Composable
fun appLocale(): Locale {
    val language = stringResource(R.string.language_code)
    val locales = LocalConfiguration.current.locales
    for (i in 0 until locales.size()) if (locales[i].language == language) return locales[i]
    // The phone is in a language the app does not have, so the texts are in English.
    return Locale.forLanguageTag(language)
}

object Fmt {
    /** "10 Dec" / "10 dic" */
    fun day(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM", locale).format(date).replace(".", "")

    /** "10 Dec 2026" */
    fun full(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date).replace(".", "")

    /** "November 2026" / "Noviembre 2026" */
    fun month(month: YearMonth, locale: Locale): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }

    /** "Nov" */
    fun monthShort(month: YearMonth, locale: Locale): String =
        DateTimeFormatter.ofPattern("LLL", locale).format(month).replace(".", "").replaceFirstChar { it.titlecase(locale) }
}

// ---------------------------------------------------------------------------------------- small pieces

@Composable
fun IconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Palette.BlueSoft,
    tint: Color = Palette.Blue,
) {
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

private val avatarColors = listOf(0xFF2F6BF6, 0xFF18A06A, 0xFFE5811C, 0xFF8E5BEF, 0xFFE5484D, 0xFF0E9AA7, 0xFFCB4B9B)

fun initials(name: String): String =
    name.split(' ', '(', '&', '-').filter { it.isNotBlank() && it.first().isLetterOrDigit() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** A company without its logo: a coloured circle with its initials. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = Color(avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size])
    Box(modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Text(initials(name), color = color, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    background: Color = Palette.Field,
    tint: Color = Palette.Ink,
    enabled: Boolean = true,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(background).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (enabled) tint else Palette.Faint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) Palette.Blue else Palette.Field,
        contentColor = if (enabled) Color.White else Palette.Faint,
        modifier = modifier.fillMaxWidth().height(54.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (icon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(icon, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** The white button of the mock: dark label, blue arrow. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: ImageVector? = null,
    leading: ImageVector? = null,
    color: Color = Palette.Ink,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Palette.Line),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, null, tint = Palette.Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Icon(trailing, null, tint = Palette.Blue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Palette.Ink else Color.White,
        border = if (selected) null else BorderStroke(1.dp, Palette.Line),
        modifier = modifier,
    ) {
        Text(
            text,
            color = if (selected) Color.White else Palette.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Palette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Palette.Muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = modifier)
}

/** A white rounded block that groups related rows. */
@Composable
fun Card(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(18.dp), content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = modifier.fillMaxWidth()) {
        Box(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack, background = Color.White)
        Spacer(Modifier.width(12.dp))
        Text(
            title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        actions()
    }
}

// ---------------------------------------------------------------------------------------- inputs

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    hint: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    prefix: String = "",
    capitalize: Boolean = false,
    singleLine: Boolean = true,
) {
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = Palette.Ink, fontSize = 17.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            ),
            cursorBrush = SolidColor(Palette.Blue),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Palette.Field).padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix.isNotEmpty()) {
                        Text(prefix, color = Palette.Muted, fontSize = 17.sp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = Palette.Faint, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                }
            },
        )
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Hint(hint)
        }
    }
}

/** Days 1 to 31 in a row to swipe through. */
@Composable
fun DayPicker(label: String, day: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier, hint: String = "") {
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (day - 4).coerceIn(0, 30))
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(6.dp))
        LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(31) { index ->
                val value = index + 1
                val selected = value == day
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(if (selected) Palette.Blue else Palette.Field)
                        .clickable { onPick(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$value",
                        color = if (selected) Color.White else Palette.Ink,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Hint(hint)
        }
    }
}

@Composable
fun MonthStepper(label: String, month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val locale = appLocale()
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Field).padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month),
                { onChange(month.minusMonths(1)) }, background = Color.White,
            )
            Text(
                Fmt.month(month, locale), fontSize = 17.sp, color = Palette.Ink,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            CircleButton(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month),
                { onChange(month.plusMonths(1)) }, background = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    clearable: Boolean = false,
) {
    val locale = appLocale()
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Field)
                .clickable { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                date?.let { Fmt.full(it, locale) } ?: placeholder,
                color = if (date != null) Palette.Ink else Palette.Faint,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
            )
            if (clearable && date != null) {
                Icon(
                    Icons.Rounded.Close, stringResource(R.string.clear), tint = Palette.Muted,
                    modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onChange(null) },
                )
            }
        }
    }
    if (open) {
        val start = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(initialSelectedDateMillis = start)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(android.R.string.cancel)) } },
        ) {
            DatePicker(picker)
        }
    }
}
