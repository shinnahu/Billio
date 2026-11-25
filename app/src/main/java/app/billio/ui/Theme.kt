package app.billio.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat

object Palette {
    val Blue = Color(0xFF2F6BF6)
    val BlueDeep = Color(0xFF2452DC)
    val BlueSoft = Color(0xFFE9F1FF)
    val Ink = Color(0xFF1B1C1F)
    val Muted = Color(0xFF858B94)
    val Faint = Color(0xFFB4B9C2)
    val Line = Color(0xFFE8EAEE)
    val Bg = Color(0xFFF6F7F9)
    val Field = Color(0xFFF2F3F5)
    val Orange = Color(0xFFE5811C)
    val Green = Color(0xFF18A06A)
    val GreenSoft = Color(0xFFE4F6EE)
    val Red = Color(0xFFE5484D)
    val RedSoft = Color(0xFFFDEBEC)
}

/** The blue sky of the home screen: deep blue at the top, fading into the page. */
val HeaderBrush = Brush.verticalGradient(
    0f to Color(0xFF2A52DE),
    0.40f to Color(0xFF4A88F7),
    0.72f to Color(0xFFA4C8FB),
    1f to Palette.Bg,
)

@Composable
fun BillioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Blue,
            onPrimary = Color.White,
            secondary = Palette.Blue,
            background = Palette.Bg,
            onBackground = Palette.Ink,
            surface = Color.White,
            onSurface = Palette.Ink,
            surfaceVariant = Palette.Field,
            onSurfaceVariant = Palette.Muted,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Palette.Field,
            outline = Palette.Line,
            error = Palette.Red,
        ),
        // Material spaces letters half a point apart by default; the design sets them tight.
        typography = Typography(bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp)),
        content = content,
    )
}

/**
 * The design's shadow under floating pieces: wide and faint. Android's own shadow for the same height is
 * several times darker, and reads as a hard edge under anything near the bottom of the screen.
 */
fun Modifier.softShadow(elevation: Dp, shape: Shape): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = Color(0x66101828), spotColor = Color(0x4D101828))

/** White status bar icons over the blue header, dark ones everywhere else. */
@Composable
fun StatusBarIcons(white: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !white
    }
}
