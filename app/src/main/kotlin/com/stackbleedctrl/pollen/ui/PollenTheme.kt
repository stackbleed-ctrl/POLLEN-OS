package com.stackbleedctrl.pollen.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object PollenColors {
    val SilverTop = Color(0xFFF4F3EF)
    val SilverMid = Color(0xFFD9D5CB)
    val SilverBottom = Color(0xFFA9A397)

    val DeepPanel = Color(0xFF0E1116)
    val DeepPanel2 = Color(0xFF171B22)

    val Gold = Color(0xFFD6AA4F)
    val GoldBright = Color(0xFFFFD36A)
    val GoldSoft = Color(0xFFE7C67A)

    val TextPrimary = Color(0xFFF4F1E8)
    val TextMuted = Color(0xFFB9B5AA)
}

val PollenBackgroundBrush = Brush.verticalGradient(
    listOf(
        PollenColors.SilverTop,
        PollenColors.SilverMid,
        PollenColors.SilverBottom
    )
)

private val PollenColorScheme = darkColorScheme(
    primary = PollenColors.Gold,
    secondary = PollenColors.GoldSoft,
    background = PollenColors.DeepPanel,
    surface = PollenColors.DeepPanel2,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = PollenColors.TextPrimary,
    onSurface = PollenColors.TextPrimary
)

@Composable
fun PollenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PollenColorScheme,
        content = content
    )
}