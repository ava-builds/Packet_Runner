package com.avabuilds.packetrunner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.avabuilds.packetrunner.Palette5

/** One scheme for light and dark: the game's own backgrounds are always the palette's blues. */
private val AppColorScheme = lightColorScheme(
    primary = Palette5.PersianBlue,
    onPrimary = Color.White,
    secondary = Palette5.Cornflower,
    onSecondary = Palette5.DeepNavy,
    tertiary = Palette5.DuskBlue,
    onTertiary = Color.White,
    background = Palette5.IcyBlue,
    onBackground = Palette5.DeepNavy,
    surface = Color.White,
    onSurface = Palette5.DeepNavy,
    surfaceVariant = Color(0xFFD6E7FD),
    onSurfaceVariant = Palette5.DuskBlue,
    outline = Palette5.Cornflower,
)

@Composable
fun PacketRunnerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}