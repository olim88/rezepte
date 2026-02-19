package olim.android.rezepte.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun DistinctDynamicColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current

    // Get dynamic system colors
    val baseColors = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    // Minimum perceptible luminance difference (0..1)
    val minContrast = 0.15f

    fun ensureContrast(base: Color, other: Color): Color {
        val diff = kotlin.math.abs(base.luminance() - other.luminance())
        return if (diff < minContrast) {
            // Push other color away from base
            if (darkTheme) lerp(other, Color.Black, minContrast - diff)
            else lerp(other, Color.White, minContrast - diff)
        } else other
    }

    return baseColors.copy(
        secondary = ensureContrast(baseColors.primary, baseColors.secondary),
        tertiary = ensureContrast(baseColors.primary, baseColors.tertiary)
    )
    }
@Composable
fun RezepteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            DistinctDynamicColorScheme(darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current


    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
        val insetsController = WindowInsetsControllerCompat(window, view)

        // Status bar
        window.statusBarColor = colorScheme.primary.toArgb()
        insetsController.isAppearanceLightStatusBars = !darkTheme

        // Navigation bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            insetsController.isAppearanceLightNavigationBars = !darkTheme

        }



    }

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}