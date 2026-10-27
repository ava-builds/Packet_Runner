package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A rounded colored box used for cards everywhere in the game. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = PR.White,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    var m = modifier.fillMaxWidth().background(color, shape)
    if (borderColor != null) m = m.border(1.5.dp, borderColor, shape)
    Column(m.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PR.Teal, contentColor = PR.White),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PR.Teal800)
    }
}

@Composable
fun Pill(text: String, background: Color = PR.Purple50, color: Color = PR.Purple800) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Nana with a speech bubble. Red-tinted bubble when [isError]. */
@Composable
fun NanaSays(text: String, isError: Boolean = false, monospace: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Nana(size = 56.dp)
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = if (isError) PR.Red800 else PR.Ink,
            modifier = Modifier
                .padding(start = 10.dp)
                .fillMaxWidth()
                .background(if (isError) PR.Red50 else PR.White, RoundedCornerShape(14.dp))
                .padding(12.dp),
        )
    }
}

/** "★★☆" in amber and gray. */
@Composable
fun StarRow(stars: Int, total: Int = 3, size: Int = 32) {
    Row {
        repeat(total) { i ->
            Text("★", fontSize = size.sp, color = if (i < stars) PR.Amber else PR.Line)
        }
    }
}
