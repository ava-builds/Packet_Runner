package com.avabuilds.packetrunner

import androidx.compose.ui.graphics.Color

/** The app palette: five blues from the style guide, plus a few functional accents. */
object Palette5 {
    val IcyBlue = Color(0xFFABD2FA)
    val Cornflower = Color(0xFF7692FF)
    val PersianBlue = Color(0xFF1B2CC1)
    val DuskBlue = Color(0xFF3D518C)
    val DeepNavy = Color(0xFF091540)
}

/** Semantic colors used across the game screens. */
object PR {
    val Bg = Palette5.IcyBlue
    val Ink = Palette5.DeepNavy
    val Muted = Palette5.DuskBlue
    val White = Color(0xFFFFFFFF)
    val Line = Palette5.Cornflower
    val Gray = Color(0xFF8CA0D8)
    val GrayFill = Color(0xFFD5E4FB)

    // Primary action
    val Teal = Palette5.PersianBlue
    val Teal50 = Color(0xFFD6E7FD)
    val Teal800 = Palette5.PersianBlue

    // Panels and accents
    val Purple = Palette5.Cornflower
    val Purple50 = Color(0xFFE6ECFF)
    val Purple800 = Palette5.PersianBlue

    // Functional: errors, success, stars
    val Red = Color(0xFFE24B4A)
    val Red50 = Color(0xFFFCEBEB)
    val Red800 = Color(0xFF791F1F)

    val Green50 = Color(0xFFE2F5E9)
    val Green600 = Color(0xFF2F8F57)
    val Green800 = Color(0xFF14502F)

    val Amber = Color(0xFFEF9F27)
    val Amber50 = Color(0xFFFAEEDA)
    val Amber800 = Color(0xFF633806)

    val Blue = Palette5.DuskBlue
    val Blue50 = Color(0xFFD6E4FF)
    val Blue800 = Palette5.DeepNavy

    // Dark boards (tables, scenario panels)
    val Board = Palette5.DeepNavy
    val BoardText = Palette5.IcyBlue
    val BoardRowSelected = Palette5.DuskBlue
}
