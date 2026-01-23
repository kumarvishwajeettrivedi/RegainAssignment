package com.example.regainassignment.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkMintGreen,
    secondary = SoftGreen,
    tertiary = MintGreen,
    background = DeepTeal,
    surface = DarkTeal,
    onPrimary = DarkText,
    onSecondary = CleanWhite,
    onTertiary = DarkText,
    onBackground = CleanWhite,
    onSurface = CleanWhite,
    primaryContainer = DarkTeal,
    secondaryContainer = DeepTeal
)

private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    secondary = SoftGreen,
    tertiary = DarkTeal,
    background = LightBeige,
    surface = CleanWhite,
    onPrimary = DarkText,
    onSecondary = DarkText,
    onTertiary = CleanWhite,
    onBackground = DarkText,
    onSurface = DarkText,
    primaryContainer = SoftGreen,
    secondaryContainer = LightBeige,
    error = AccentOrange
)

@Composable
fun RegainAssignmentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce our focus theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // We override dynamic color to false by default, but if passed as true we could use it. 
        // For this task, we want the specific "Focus" look.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}