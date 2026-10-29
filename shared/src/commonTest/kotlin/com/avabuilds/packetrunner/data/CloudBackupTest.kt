package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class CloudBackupTest {
    private class FakeSaves(var stored: SaveData? = null, var failSave: Boolean = false, var failLoad: Boolean = false) : SaveRepository {
        override fun load(uid: String, onDone: (LoadResult) -> Unit) =
            onDone(if (failLoad) LoadResult.Failed("offline") else LoadResult.Found(stored))
        override fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit) {
            if (!failSave) stored = data
            onDone(!failSave)
        }
        override fun delete(uid: String, onDone: (Boolean) -> Unit) { stored = null; onDone(true) }
    }

    private class NoAuth : AuthRepository {
        override val currentUser: UserProfile? = null
        override fun signUp(email: String, password: String, nickname: String, onDone: (AuthOutcome) -> Unit) = Unit
        override fun signIn(email: String, password: String, onDone: (AuthOutcome) -> Unit) = Unit
        override fun signOut() = Unit
        override fun updateNickname(nickname: String, onDone: (AuthOutcome) -> Unit) = Unit
        override fun deleteAccount(password: String, beforeDelete: (String, (Boolean) -> Unit) -> Unit, onDone: (AuthOutcome) -> Unit) = Unit
    }

    private val save = SaveData(day = 4, money = 500, hearts = emptyMap(), connected = setOf("hana"), solved = emptyList())
    private val note = Note("n1", "TCP", "three-way handshake", "TCP", 1L, 2L)

    private fun backup(saves: SaveRepository, notebook: NotebookRepository) = CloudBackup(NoAuth(), saves, notebook)

    @Test fun backingUpCopiesTheGameAndTheNotes() {
        val saves = FakeSaves()
        val notebook = InMemoryNotebookRepository()
        var ok = false
        backup(saves, notebook).backUp("u", save, listOf(note)) { ok = it }
        assertTrue(ok)
        assertEquals(save, saves.stored)
        notebook.list("u") { assertEquals(listOf(note), it.getOrThrow()) }
    }

    @Test fun aFailedGameBackupSaysSoAndLeavesTheNotesAlone() {
        val notebook = InMemoryNotebookRepository().also { it.replaceAll("u", listOf(note)) {} }
        var ok = true
        backup(FakeSaves(failSave = true), notebook).backUp("u", save, emptyList()) { ok = it }
        assertFalse(ok)
        notebook.list("u") { assertEquals(listOf(note), it.getOrThrow()) }
    }

    @Test fun restoringGivesBackTheGameAndTheNotes() {
        val notebook = InMemoryNotebookRepository().also { it.replaceAll("u", listOf(note)) {} }
        var result: RestoreResult? = null
        backup(FakeSaves(save), notebook).restore("u") { result = it }
        assertEquals(RestoreResult.Found(CloudCopy(save, listOf(note))), result)
    }

    @Test fun restoringWithNothingBackedUpAndFailuresAreTold() {
        var result: RestoreResult? = null
        backup(FakeSaves(), InMemoryNotebookRepository()).restore("u") { result = it }
        assertEquals(RestoreResult.NothingBackedUp, result)
        backup(FakeSaves(failLoad = true), InMemoryNotebookRepository()).restore("u") { result = it }
        assertEquals(RestoreResult.Failed("offline"), result)
    }

    @Test fun deletingTheBackupRemovesTheGameAndTheNotes() {
        val saves = FakeSaves(save)
        val notebook = InMemoryNotebookRepository().also { it.replaceAll("u", listOf(note)) {} }
        var ok = false
        backup(saves, notebook).deleteBackup("u") { ok = it }
        assertTrue(ok)
        assertEquals(null, saves.stored)
        notebook.list("u") { assertTrue(it.getOrThrow().isEmpty()) }
    }
}
