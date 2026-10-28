package com.avabuilds.packetrunner.game

enum class BuildingType { OFFICE, HOUSE, CAFE, CITY_HALL, APARTMENTS }

/** Things that stand in the town besides buildings and trees. */
enum class PropKind { STREETLIGHT, BENCH, FENCE, FLOWERS, MAILBOX, CAR, BUS_STOP }

/**
 * A small decoration on one tile. [variant] picks a color or direction (a fence runs along x or y).
 * A prop with [blocks] set cannot be walked through; a thin streetlight can.
 */
data class Prop(val kind: PropKind, val tile: TilePos, val variant: Int = 0, val blocks: Boolean = true)

/**
 * Which wall of a building the door is on. In the isometric view only two walls face the camera:
 * RIGHT faces the lower right (the +x side) and LEFT faces the lower left (the +y side).
 */
enum class DoorSide { RIGHT, LEFT }

/**
 * A square building of [size] x [size] tiles whose top-left corner tile is [origin].
 * [door] is the walkable tile in front of the door; it must touch the RIGHT or LEFT wall.
 */
data class Building(
    val id: String,
    val type: BuildingType,
    val name: String,
    val origin: TilePos,
    val size: Int,
    val door: TilePos,
    /** Picks a color variation when drawn, for example the roof color of a house. */
    val style: Int = 0,
) {
    /** Every tile the building stands on. Nobody can walk here. */
    val footprint: Set<TilePos> =
        (0 until size).flatMap { dx -> (0 until size).map { dy -> TilePos(origin.x + dx, origin.y + dy) } }.toSet()

    /** Center of the footprint, in tile coordinates. */
    val center: Vec2 get() = Vec2(origin.x + (size - 1) / 2f, origin.y + (size - 1) / 2f)

    /**
     * Drawing order: buildings with a bigger value are in front and are drawn later.
     * It is the x + y of the front corner tile of the footprint.
     */
    val depth: Float get() = (origin.x + origin.y + 2 * (size - 1)).toFloat()

    val doorSide: DoorSide
    /** Position of the door along its wall: 0 until [size]. */
    val doorIndex: Int

    init {
        require(size >= 1) { "A building needs at least one tile" }
        val onRightWall = door.x == origin.x + size && door.y in origin.y until origin.y + size
        val onLeftWall = door.y == origin.y + size && door.x in origin.x until origin.x + size
        require(onRightWall || onLeftWall) { "The door of $id must touch the right or left wall" }
        doorSide = if (onRightWall) DoorSide.RIGHT else DoorSide.LEFT
        doorIndex = if (onRightWall) door.y - origin.y else door.x - origin.x
    }
}

/**
 * The town: a grid with roads, buildings and trees, plus where the player starts.
 * Buildings and trees block walking; roads and grass can be walked on.
 * The constructor rejects impossible towns (overlaps, things outside the grid, blocked doors).
 */
class TownMap(
    val width: Int,
    val height: Int,
    val roads: Set<TilePos>,
    val buildings: List<Building>,
    val trees: Set<TilePos>,
    val start: TilePos,
    val props: List<Prop> = emptyList(),
) {
    private val footprints: Set<TilePos>

    init {
        val all = buildings.flatMap { it.footprint }
        require(all.size == all.toSet().size) { "Two buildings overlap" }
        require(buildings.map { it.id }.toSet().size == buildings.size) { "Building ids must be unique" }
        footprints = all.toSet()

        val inside = { t: TilePos -> t.x in 0 until width && t.y in 0 until height }
        require((footprints + roads + trees + start + buildings.map { it.door }).all(inside)) { "Something is outside the grid" }
        require(footprints.none { it in roads }) { "A building stands on a road" }
        require(footprints.none { it in trees }) { "A tree stands inside a building" }
        require(trees.none { it in roads }) { "A tree stands on a road" }
        require(buildings.all { it.door !in footprints && it.door !in trees }) { "A door is blocked" }
        require(start !in footprints && start !in trees) { "The player starts on a blocked tile" }

        val propTiles = props.map { it.tile }
        require(propTiles.size == propTiles.toSet().size) { "Two props share a tile" }
        require(propTiles.all(inside)) { "A prop is outside the grid" }
        require(propTiles.none { it in footprints || it in roads || it in trees }) { "A prop stands on a building, a road or a tree" }
        require(propTiles.none { it == start || it in buildings.map { b -> b.door } }) { "A prop blocks the start or a door" }
    }

    val grid = Grid(width, height, blocked = footprints + trees + props.filter { it.blocks }.map { it.tile })

    fun building(id: String): Building = buildings.first { it.id == id }

    /** The building standing on [tile], or null. */
    fun buildingAt(tile: TilePos): Building? = buildings.firstOrNull { tile in it.footprint }
}

/** The starter town. */
object Town {
    /**
     * A 10x10 town with a road going down (x = 5) and another going across (y = 5).
     * The player starts at the door of the Office, which is home base.
     */
    fun default(): TownMap {
        val roads = buildSet {
            for (i in 0 until 10) {
                add(TilePos(5, i))
                add(TilePos(i, 5))
            }
        }
        val buildings = listOf(
            Building("office", BuildingType.OFFICE, "Office", origin = TilePos(2, 2), size = 2, door = TilePos(4, 2)),
            Building("cafe", BuildingType.CAFE, "Cafe", origin = TilePos(6, 2), size = 2, door = TilePos(6, 4)),
            Building("house1", BuildingType.HOUSE, "House 1", origin = TilePos(0, 6), size = 2, door = TilePos(2, 6), style = 0),
            Building("house2", BuildingType.HOUSE, "House 2", origin = TilePos(3, 8), size = 2, door = TilePos(5, 8), style = 1),
            Building("house3", BuildingType.HOUSE, "House 3", origin = TilePos(6, 6), size = 2, door = TilePos(8, 6), style = 2),
            Building("cityhall", BuildingType.CITY_HALL, "City Hall", origin = TilePos(0, 8), size = 2, door = TilePos(2, 8)),
            Building("apartments", BuildingType.APARTMENTS, "Apartments", origin = TilePos(8, 0), size = 2, door = TilePos(8, 2)),
        )
        val trees = setOf(
            TilePos(0, 0), TilePos(1, 0), TilePos(7, 0), TilePos(0, 3), TilePos(0, 4),
            TilePos(8, 4), TilePos(4, 7), TilePos(8, 7), TilePos(9, 9), TilePos(7, 9), TilePos(6, 9),
        )
        // Props sit beside the roads. Every door stays reachable (TownMapTest checks it).
        val props = listOf(
            Prop(PropKind.BUS_STOP, TilePos(4, 6)),
            Prop(PropKind.BENCH, TilePos(3, 4)), Prop(PropKind.BENCH, TilePos(8, 9), variant = 1),
            Prop(PropKind.FENCE, TilePos(3, 6), variant = 1), Prop(PropKind.FENCE, TilePos(3, 7), variant = 1),
            Prop(PropKind.FENCE, TilePos(9, 6), variant = 1), Prop(PropKind.FENCE, TilePos(9, 7), variant = 1),
            Prop(PropKind.FLOWERS, TilePos(4, 3)), Prop(PropKind.FLOWERS, TilePos(1, 2)),
            Prop(PropKind.FLOWERS, TilePos(7, 1)), Prop(PropKind.FLOWERS, TilePos(2, 9)),
            Prop(PropKind.MAILBOX, TilePos(2, 4)),
            Prop(PropKind.CAR, TilePos(4, 4), variant = 0), Prop(PropKind.CAR, TilePos(7, 4), variant = 1),
            Prop(PropKind.CAR, TilePos(9, 8), variant = 2),
            Prop(PropKind.STREETLIGHT, TilePos(4, 1), blocks = false), Prop(PropKind.STREETLIGHT, TilePos(6, 1), blocks = false),
            Prop(PropKind.STREETLIGHT, TilePos(1, 3), blocks = false), Prop(PropKind.STREETLIGHT, TilePos(8, 3), blocks = false),
            Prop(PropKind.STREETLIGHT, TilePos(2, 7), blocks = false), Prop(PropKind.STREETLIGHT, TilePos(8, 8), blocks = false),
        )
        return TownMap(10, 10, roads, buildings, trees, start = TilePos(4, 2), props = props)
    }
}
