package com.avabuilds.packetrunner.network

/** What a client (laptop, phone) experiences. */
enum class ClientStatus { CONNECTED, NO_SIGNAL, NO_UPLINK, NO_IP, COLLISIONS }

/**
 * What the Scan tool finds out about one client. [path] is the route Pip walks: device ids from
 * the wall socket to the last device he can reach, with the client at the end when it is online.
 */
data class ClientReport(
    val deviceId: String,
    val status: ClientStatus,
    val sourceId: String?,
    val signal: Float,
    val speedMbps: Int,
    val latencyMs: Int,
    val path: List<String>,
    val reason: String,
)

data class NetworkReport(val clients: List<ClientReport>, val onlineSources: Set<String>) {
    val allConnected: Boolean get() = clients.isNotEmpty() && clients.all { it.status == ClientStatus.CONNECTED }
    fun forClient(id: String): ClientReport? = clients.firstOrNull { it.deviceId == id }
}

/**
 * Works out how a room's network behaves. It follows the rules we teach:
 *  - the internet comes from the wall socket, along Ethernet cables;
 *  - a router, access point or repeater is only useful when it is online (cabled to the socket,
 *    or for a repeater, within range of an online source);
 *  - a client joins the closest online source that reaches it; farther away means a weaker signal
 *    and a lower speed; a repeater adds delay and halves the speed;
 *  - the router's DHCP server hands out IP addresses until its pool runs out (devices that left
 *    keep theirs until the lease ends);
 *  - two clients that reach the same source but cannot hear each other collide (the hidden
 *    node problem), unless the source asks permission first (RTS/CTS).
 */
object NetworkSimulator {
    /** Clients can hear each other up to this distance, in cells. */
    const val CLIENT_RANGE = 4f

    private const val WIRED_SPEED = 100
    private const val REPEATER_SPEED = 50

    fun evaluate(scene: NetworkScene): NetworkReport {
        val socket = scene.devices.firstOrNull { it.type == DeviceType.WALL_SOCKET }
        val wiredParent = if (socket != null) wiredTree(scene, socket.id) else emptyMap()

        // Wi-Fi sources that are online
        val sources = scene.devices.filter { it.type.isWifiSource }
        val online = linkedMapOf<String, Device>()
        sources.filter { it.type != DeviceType.REPEATER && it.id in wiredParent }.forEach { online[it.id] = it }
        val upstreamOf = mutableMapOf<String, String>() // repeater id -> the source it listens to
        sources.filter { it.type == DeviceType.REPEATER }.forEach { rep ->
            val upstream = online.values
                .filter { it.type != DeviceType.REPEATER && scene.distance(it.id, rep.id) <= it.type.wifiRange }
                .minByOrNull { scene.distance(it.id, rep.id) }
            if (upstream != null) {
                online[rep.id] = rep
                upstreamOf[rep.id] = upstream.id
            }
        }

        // Who answers DHCP requests: a router that is online
        val dhcpRouter = scene.devices.firstOrNull { it.type == DeviceType.ROUTER && it.id in wiredParent }
        var addressesLeft = dhcpRouter?.settings?.let { DhcpPool(it.dhcpPoolSize, it.leaseMinutes, scene.staleLeaseAges).free } ?: 0

        // Each client picks the closest online source that reaches it
        data class Pick(val client: Device, val source: Device?, val distance: Float)
        val picks = scene.devices.filter { it.type.isClient }.sortedBy { it.id }.map { client ->
            val reachable = online.values.filter { scene.distance(it.id, client.id) <= it.type.wifiRange }
            val best = reachable.minByOrNull { scene.distance(it.id, client.id) }
            Pick(client, best, best?.let { scene.distance(it.id, client.id) } ?: 0f)
        }

        // Hand out IP addresses in order; the rest are left without
        val hasIp = mutableSetOf<String>()
        picks.filter { it.source != null }.forEach { pick ->
            if (addressesLeft > 0) {
                addressesLeft--
                hasIp += pick.client.id
            }
        }

        // Hidden node: two clients on the same source that are out of each other's hearing range
        val colliding = mutableSetOf<String>()
        val onSource = picks.filter { it.source != null && it.client.id in hasIp }.groupBy { it.source!!.id }
        onSource.forEach { (sourceId, group) ->
            if (online.getValue(sourceId).settings.rtsCts) return@forEach
            for (i in group.indices) for (j in i + 1 until group.size) {
                if (scene.distance(group[i].client.id, group[j].client.id) > CLIENT_RANGE) {
                    colliding += group[i].client.id
                    colliding += group[j].client.id
                }
            }
        }

        val reports = picks.map { pick ->
            val client = pick.client
            val source = pick.source
            when {
                source == null -> offline(scene, client, sources, socket)
                client.id !in hasIp -> ClientReport(
                    client.id, ClientStatus.NO_IP, source.id, 0f, 0, 0, pathTo(scene, source, wiredParent, upstreamOf) + client.id,
                    if (dhcpRouter == null) "Connected to Wi-Fi, but no router hands out IP addresses." else "The router ran out of IP addresses.",
                )
                else -> {
                    val signal = (1f - pick.distance / source.type.wifiRange).coerceIn(0f, 1f)
                    val isRepeater = source.type == DeviceType.REPEATER
                    val path = pathTo(scene, source, wiredParent, upstreamOf) + client.id
                    var speed = (if (isRepeater) REPEATER_SPEED else WIRED_SPEED) * (0.4f + 0.6f * signal)
                    var latency = 12f + (path.size - 1) * 4f + (if (isRepeater) 12f else 0f) + (1f - signal) * 20f
                    val collides = client.id in colliding
                    if (collides) {
                        speed *= 0.2f
                        latency += 120f
                    } else if (source.settings.rtsCts) {
                        speed *= 0.9f
                    }
                    ClientReport(
                        client.id,
                        if (collides) ClientStatus.COLLISIONS else ClientStatus.CONNECTED,
                        source.id, signal, speed.toInt(), latency.toInt(), path,
                        if (collides) "Collisions! Two devices can't hear each other and talk at the same time." else "Online.",
                    )
                }
            }
        }
        return NetworkReport(reports, online.keys.toSet())
    }

    /** For each device reachable from [socketId] by cable: the device before it on the way. */
    private fun wiredTree(scene: NetworkScene, socketId: String): Map<String, String?> {
        val parent = linkedMapOf<String, String?>(socketId to null)
        val queue = ArrayDeque(listOf(socketId))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (next in scene.cabledNeighbors(current).sorted()) {
                if (next !in parent) {
                    parent[next] = current
                    queue.addLast(next)
                }
            }
        }
        return parent
    }

    /** The ids from the wall socket to [source] (a repeater is reached through the source it listens to). */
    private fun pathTo(scene: NetworkScene, source: Device, wiredParent: Map<String, String?>, upstreamOf: Map<String, String>): List<String> {
        val wiredId = if (source.type == DeviceType.REPEATER) upstreamOf.getValue(source.id) else source.id
        val path = mutableListOf<String>()
        var step: String? = wiredId
        while (step != null) {
            path.add(step)
            step = wiredParent[step]
        }
        path.reverse()
        return if (source.type == DeviceType.REPEATER) path + source.id else path
    }

    /** No online source reaches this client: find out why, so the Scan can say it. */
    private fun offline(scene: NetworkScene, client: Device, sources: List<Device>, socket: Device?): ClientReport {
        val nearbyOffline = sources.filter { scene.distance(it.id, client.id) <= it.type.wifiRange }
        val closest = nearbyOffline.minByOrNull { scene.distance(it.id, client.id) }
        // Pip can only get as far as the wall socket: nothing carries the signal on to this client
        val path = if (socket != null) listOf(socket.id) else emptyList()
        return if (closest != null) {
            ClientReport(
                client.id, ClientStatus.NO_UPLINK, closest.id, 0f, 0, 0, path,
                "The ${closest.type.label.lowercase()} reaches here, but it has no connection to the internet.",
            )
        } else {
            val nearest = sources.minByOrNull { scene.distance(it.id, client.id) }
            ClientReport(
                client.id, ClientStatus.NO_SIGNAL, null, 0f, 0, 0, path,
                if (nearest == null) "There is no Wi-Fi in this room yet." else "Out of Wi-Fi range. The signal doesn't reach here.",
            )
        }
    }
}
