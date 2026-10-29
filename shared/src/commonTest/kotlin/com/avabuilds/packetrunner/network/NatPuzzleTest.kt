package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertNotNull
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class NatPuzzleTest {
    private val puzzle = NatPuzzles.apartment

    @Test fun outgoingConnectionsGetOwnPublicPortsInOrder() {
        val t = NatTable()
        assertEquals(50000, t.outbound(NatConnection(0, 5000, "a:80")).publicPort)
        assertEquals(50001, t.outbound(NatConnection(1, 5000, "b:80")).publicPort)
    }

    @Test fun theSamePrivatePortOnDifferentFlatsStillGetsDifferentPublicPorts() {
        // three flats all use source port 5000: only the public port tells them apart
        val ports = puzzle.fullTable().entries.filter { it.privatePort == 5000 }.map { it.publicPort }
        assertEquals(3, ports.size)
        assertEquals(3, ports.toSet().size)
    }

    @Test fun aRepeatedConnectionKeepsItsRow() {
        val t = NatTable()
        val first = t.outbound(NatConnection(0, 5000, "a:80"))
        val again = t.outbound(NatConnection(0, 5000, "a:80"))
        assertEquals(first, again)
        assertEquals(1, t.entries.size)
    }

    @Test fun repliesAreMatchedToTheFlatThatAsked() {
        val t = puzzle.fullTable()
        puzzle.connections.forEach { c ->
            val publicPort = t.entries.first { it.flat == c.flat && it.privatePort == c.privatePort }.publicPort
            assertEquals(c.flat, t.inbound(publicPort)!!.flat)
        }
    }

    @Test fun aPacketNobodyAskedForHasNoRow() {
        assertNull(puzzle.fullTable().inbound(50099))
    }

    @Test fun incomingMixesRepliesWithOneStrangerAndEveryRealReplyAppearsOnce() {
        val incoming = puzzle.incoming()
        assertEquals(puzzle.connections.size + 1, incoming.size)
        assertEquals(incoming.size, incoming.map { it.publicPort }.toSet().size)
        assertEquals(1, incoming.count { puzzle.rightAnswer(it) == null })
    }

    @Test fun theRightChoiceIsAcceptedAndAWrongOneIsNot() {
        val reply = puzzle.incoming().first { puzzle.rightAnswer(it) != null }
        val flat = puzzle.rightAnswer(reply)!!
        assertTrue(puzzle.isRight(reply, flat))
        assertFalse(puzzle.isRight(reply, (flat + 1) % puzzle.flats.size))
        assertFalse(puzzle.isRight(reply, null))
    }

    @Test fun theStrangerMustBeDroppedNotDelivered() {
        val stranger = puzzle.incoming().first { puzzle.rightAnswer(it) == null }
        assertTrue(puzzle.isRight(stranger, null))
        assertFalse(puzzle.isRight(stranger, 0))
    }

    @Test fun withoutAForwardingRuleNoVisitorGetsIn() {
        assertFalse(puzzle.visitorsServed(null))
    }

    @Test fun aRuleToTheRightFlatServesVisitorsAndStillDropsOtherPorts() {
        assertTrue(puzzle.visitorsServed(puzzle.serverFlat))
        val remoteLogin = puzzle.visitors.first { it.port != puzzle.serverPort }
        assertNull(puzzle.visitorGoesTo(remoteLogin, puzzle.serverFlat))
    }

    @Test fun aRuleToTheWrongFlatDoesNotServeVisitors() {
        assertFalse(puzzle.visitorsServed((puzzle.serverFlat + 1) % puzzle.flats.size))
    }

    @Test fun flatsHaveDifferentPrivateAddressesInTheSameNetwork() {
        assertEquals(puzzle.flats.size, puzzle.flats.map { it.ip }.toSet().size)
        assertTrue(puzzle.flats.all { it.ip.octets.take(3) == listOf(192, 168, 1) })
    }

    @Test fun starsAndTipsFollowTheMistakes() {
        assertEquals(3, puzzle.stars(0))
        assertEquals(3, puzzle.stars(1))
        assertEquals(2, puzzle.stars(3))
        assertEquals(1, puzzle.stars(4))
        assertEquals(20, puzzle.tip(3))
    }

    @Test fun theJobTeachesNatConcepts() {
        assertTrue(Concepts.NAT_TABLE in puzzle.concepts && Concepts.PORT_FORWARDING in puzzle.concepts && Concepts.PRIVATE_ADDRESSES in puzzle.concepts)
        assertNotNull(puzzle.hints.firstOrNull())
    }
}
