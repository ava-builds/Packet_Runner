package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.Light
import com.avabuilds.packetrunner.game.PropKind
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.ui.sprites.Sprite
import com.avabuilds.packetrunner.ui.sprites.SpriteKey
import com.avabuilds.packetrunner.ui.sprites.SpriteProvider

private val LampColor = Color(0xFFFFD36B)

/**
 * Colors the whole scene for the time of day by multiplying it: warm in the morning, orange in the evening, dark blue
 * at night. Multiplying keeps the colors rich (a plain see-through color would turn grass and sky grey).
 */
internal fun DrawScope.drawDayTint(light: Light) {
    if (light.tintAlpha <= 0.001f) return
    fun mix(channel: Float) = 1f - light.tintAlpha * (1f - channel) // 1 = unchanged, lower = darker
    drawRect(Color(mix(light.tintRed), mix(light.tintGreen), mix(light.tintBlue)), blendMode = BlendMode.Multiply)
}

/**
 * The lights that shine on top of the dark: lit windows in every building, and the streetlights with a glow around
 * the lamp and a pool of light on the ground. They fade in through the evening and are fully on at night.
 */
internal fun DrawScope.drawNightLights(view: ViewTransform, sprites: SpriteProvider, town: TownMap, light: Light, lockedBuildings: Set<String> = emptySet()) {
    val scope = this
    val tw = view.tw
    if (light.windowGlow > 0.01f) {
        town.buildings.forEach { b ->
            if (b.id in lockedBuildings) return@forEach // a lot or a building site has no lit windows
            val at = view.screenOf(b.center.x, b.center.y)
            if (!view.onScreen(at)) return@forEach
            sprites.draw(
                scope,
                Sprite(
                    SpriteKey.LIGHTS, at, tw, size = b.size, variant = b.type.ordinal, doorSide = b.doorSide, doorIndex = b.doorIndex,
                    alpha = light.windowGlow, phase = (b.style + b.origin.x * 3 + b.origin.y).toFloat(),
                ),
            )
        }
    }
    if (light.lampGlow > 0.01f) {
        town.props.filter { it.kind == PropKind.STREETLIGHT }.forEach { lamp ->
            val ground = view.screenOf(lamp.tile.x.toFloat(), lamp.tile.y.toFloat())
            if (!view.onScreen(ground)) return@forEach
            val head = ground + Offset(0.1f * tw, -0.78f * tw)
            val g = light.lampGlow
            // a pool of light on the ground, then the glow around the lamp itself
            drawOval(LampColor.copy(alpha = 0.22f * g), ground - Offset(0.85f * tw, 0.32f * tw), Size(1.7f * tw, 0.64f * tw))
            drawCircle(
                Brush.radialGradient(listOf(LampColor.copy(alpha = 0.6f * g), Color.Transparent), center = head, radius = 0.9f * tw),
                0.9f * tw, head,
            )
            drawCircle(Color(0xFFFFF3C4).copy(alpha = g), 0.05f * tw, head)
        }
    }
    // the cafe's round sign glows too
    if (light.lampGlow > 0.01f) {
        town.buildings.filter { it.type == BuildingType.CAFE }.forEach { b ->
            val sign = view.screenOf(b.center.x, b.center.y) - Offset(0f, 1.23f * tw)
            drawCircle(Brush.radialGradient(listOf(LampColor.copy(alpha = 0.5f * light.lampGlow), Color.Transparent), center = sign, radius = 0.5f * tw), 0.5f * tw, sign)
        }
    }
}
