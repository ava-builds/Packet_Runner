package com.avabuilds.packetrunner.game

/**
 * The outline of an isometric box on screen: the ground diamond (width [w], height [h]) around
 * [center], lifted by [boxHeight]. Used to decide whether a tap landed on a building.
 */
fun isoBoxSilhouette(center: Vec2, w: Float, h: Float, boxHeight: Float): List<Vec2> = listOf(
    Vec2(center.x - w / 2, center.y), // left corner on the ground
    Vec2(center.x, center.y + h / 2), // front corner on the ground
    Vec2(center.x + w / 2, center.y), // right corner on the ground
    Vec2(center.x + w / 2, center.y - boxHeight), // right corner on the roof
    Vec2(center.x, center.y - h / 2 - boxHeight), // back corner on the roof
    Vec2(center.x - w / 2, center.y - boxHeight), // left corner on the roof
)

/** True when [p] is inside [polygon] (ray casting: count how many edges a ray to the right crosses). */
fun pointInPolygon(polygon: List<Vec2>, p: Vec2): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        val crosses = (a.y > p.y) != (b.y > p.y) &&
            p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
        if (crosses) inside = !inside
        j = i
    }
    return inside
}
