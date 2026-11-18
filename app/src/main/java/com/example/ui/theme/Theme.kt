package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.common.LocalAppStrings
import com.example.core.common.stringsFor
import com.example.core.model.AppAccentColor
import com.example.core.model.AppLanguage
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.ThemeMode

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private fun buildLightColorScheme(accent: AppAccentColor): ColorScheme {
    val primary = Color(accent.primaryHex)
    val tertiary = Color(accent.accentHex)
    val onPrimary = if (primary.luminance() > 0.45f) PrimaryTextLight else Color.White
    val primaryContainer = lerp(Color.White, primary, 0.16f)
    val onPrimaryContainer = lerp(primary, Color.Black, 0.58f)
    val secondary = lerp(primary, tertiary, 0.30f)
    val onSecondary = if (secondary.luminance() > 0.45f) PrimaryTextLight else Color.White
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = tertiary,
        onTertiary = PrimaryTextLight,
        background = LightBackground,
        onBackground = PrimaryTextLight,
        surface = LightSurface,
        onSurface = PrimaryTextLight,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = SecondaryText,
        surfaceTint = primary,
        outline = LightOutline,
        outlineVariant = Color(0xFFECEFF2),
        error = ExpenseRed,
        onError = Color.White
    )
}

private fun buildDarkColorScheme(
    accent: AppAccentColor,
    amoled: Boolean
): ColorScheme {
    val primary = Color(accent.primaryHex)
    val tertiary = Color(accent.accentHex)
    val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF141100) else Color.White
    val bg = if (amoled) AmoledBackground else DarkBackground
    val surf = if (amoled) AmoledSurface else DarkSurface
    val surfVar = if (amoled) AmoledSurfaceVariant else DarkSurfaceVariant
    val outline = if (amoled) AmoledOutline else DarkOutline
    val outlineVariant = if (amoled) Color(0xFF1C1F28) else Color(0xFF232732)
    val primaryContainer = lerp(surf, primary, 0.24f)
    val onPrimaryContainer = lerp(tertiary, Color.White, 0.25f)
    val secondary = lerp(primary, tertiary, 0.30f)
    val onSecondary = if (secondary.luminance() > 0.45f) Color(0xFF141100) else Color.White

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = tertiary,
        onTertiary = bg,
        background = bg,
        onBackground = PrimaryTextDark,
        surface = surf,
        onSurface = PrimaryTextDark,
        surfaceVariant = surfVar,
        onSurfaceVariant = SecondaryText,
        surfaceTint = primary,
        outline = outline,
        outlineVariant = outlineVariant,
        error = ExpenseRed,
        onError = Color.White
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ExpenseManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AppAccentColor = AppAccentColor.EMERALD,
    language: AppLanguage = AppLanguage.ENGLISH,
    languageTag: String? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(accent = accentColor, amoled = themeMode == ThemeMode.AMOLED)
    } else {
        buildLightColorScheme(accent = accentColor)
    }
    val context = LocalContext.current
    val effectiveTag = SupportedLanguageCatalog.normalizeTag(languageTag ?: language.code)
    val strings = remember(context, effectiveTag) {
        stringsFor(context, effectiveTag)
    }
    val layoutDirection = if (SupportedLanguageCatalog.isRtl(effectiveTag)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalIsDarkTheme provides darkTheme,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
