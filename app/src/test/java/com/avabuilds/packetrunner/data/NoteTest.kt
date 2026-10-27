package com.avabuilds.packetrunner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTest {
    private val uid = "u1"

    private fun note(id: String, title: String, body: String = "", topic: String = "", updated: Long = 0) =
        Note(id, title, body, topic, createdAt = 0, updatedAt = updated)

    // ---- the note itself ----

    @Test fun aNoteSurvivesTheTripToAMapAndBack() {
        val n = Note("a", "DHCP", "leases expire", "DHCP lease time", 100L, 250L)
        assertEquals(n, Note.fromMap("a", n.toMap()))
    }

    @Test fun firestoreIntegersAndMissingFieldsAreHandled() {
        val n = Note.fromMap("a", mapOf("title" to "T", "createdAt" to 5, "updatedAt" to 6L))
        assertEquals("T", n.title)
        assertEquals("", n.body)
        assertEquals(5L, n.createdAt)
        assertEquals("", Note.fromMap("b", mapOf("title" to 42)).title)
    }

    @Test fun titlesNeedTextAndAreNotTooLong() {
        assertNotNull(NoteValidation.title(""))
        assertNotNull(NoteValidation.title("   "))
        assertNull(NoteValidation.title("Hidden node"))
        assertNull(NoteValidation.title("x".repeat(NoteValidation.TITLE_MAX)))
        assertNotNull(NoteValidation.title("x".repeat(NoteValidation.TITLE_MAX + 1)))
    }

    @Test fun bodiesHaveALengthLimit() {
        assertNull(NoteValidation.body(""))
        assertNull(NoteValidation.body("x".repeat(NoteValidation.BODY_MAX)))
        assertNotNull(NoteValidation.body("x".repeat(NoteValidation.BODY_MAX + 1)))
    }

    // ---- sorting and searching ----

    @Test fun theNewestlyChangedNoteComesFirst() {
        val sorted = NoteList.sorted(listOf(note("a", "old", updated = 1), note("b", "new", updated = 9), note("c", "mid", updated = 5)))
        assertEquals(listOf("b", "c", "a"), sorted.map { it.id })
    }

    @Test fun searchLooksAtTitleTextAndTopicIgnoringCase() {
        val notes = listOf(note("a", "RTS/CTS"), note("b", "x", body = "the router hands out IP addresses"), note("c", "y", topic = "DHCP"))
        assertEquals(listOf("a"), NoteList.filtered(notes, "rts").map { it.id })
        assertEquals(listOf("b"), NoteList.filtered(notes, "ROUTER").map { it.id })
        assertEquals(listOf("c"), NoteList.filtered(notes, "dhcp").map { it.id })
        assertTrue(NoteList.filtered(notes, "zebra").isEmpty())
    }

    @Test fun aBlankSearchKeepsEveryNote() {
        val notes = listOf(note("a", "A"), note("b", "B"))
        assertEquals(notes, NoteList.filtered(notes, "  "))
    }

    // ---- create, read, update, delete ----

    @Test fun createThenReadGivesTheNoteBack() {
        val repo = InMemoryNotebookRepository()
        var added: Note? = null
        repo.add(uid, NoteDraft("  Switch  ", "learns addresses", "Learning bridge"), now = 1000L) { added = it }
        assertEquals("Switch", added!!.title)
        assertEquals(1000L, added!!.createdAt)
        var listed = emptyList<Note>()
        repo.list(uid) { listed = it.getOrThrow() }
        assertEquals(listOf(added), listed)
    }

    @Test fun updateChangesTheNoteAndKeepsItsId() {
        val repo = InMemoryNotebookRepository()
        var n: Note? = null
        repo.add(uid, NoteDraft("A", "b"), 1L) { n = it }
        var ok = false
        repo.update(uid, n!!.copy(title = "A2", updatedAt = 2L)) { ok = it }
        assertTrue(ok)
        var listed = emptyList<Note>()
        repo.list(uid) { listed = it.getOrThrow() }
        assertEquals(1, listed.size)
        assertEquals("A2", listed[0].title)
        assertEquals(n!!.id, listed[0].id)
    }

    @Test fun updatingANoteThatIsGoneFails() {
        var ok = true
        InMemoryNotebookRepository().update(uid, note("ghost", "x")) { ok = it }
        assertFalse(ok)
    }

    @Test fun deleteRemovesOnlyThatNote() {
        val repo = InMemoryNotebookRepository()
        val ids = mutableListOf<String>()
        repeat(2) { repo.add(uid, NoteDraft("n$it", ""), it.toLong()) { n -> ids += n!!.id } }
        repo.delete(uid, ids[0]) {}
        var listed = emptyList<Note>()
        repo.list(uid) { listed = it.getOrThrow() }
        assertEquals(listOf(ids[1]), listed.map { it.id })
    }

    @Test fun playersNeverSeeEachOthersNotes() {
        val repo = InMemoryNotebookRepository()
        repo.add("ava", NoteDraft("mine", ""), 1L) {}
        var other = listOf<Note>(note("x", "x"))
        repo.list("bo") { other = it.getOrThrow() }
        assertTrue(other.isEmpty())
    }

    @Test fun deleteAllEmptiesTheNotebook() {
        val repo = InMemoryNotebookRepository()
        repeat(3) { repo.add(uid, NoteDraft("n$it", ""), 1L) {} }
        repo.deleteAll(uid) {}
        var listed = listOf<Note>(note("x", "x"))
        repo.list(uid) { listed = it.getOrThrow() }
        assertTrue(listed.isEmpty())
    }
}
