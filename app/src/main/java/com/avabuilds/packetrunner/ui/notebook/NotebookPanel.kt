package com.avabuilds.packetrunner.ui.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.Note
import com.avabuilds.packetrunner.data.NoteDraft
import com.avabuilds.packetrunner.data.NoteList
import com.avabuilds.packetrunner.data.NoteValidation
import com.avabuilds.packetrunner.data.NotebookRepository
import com.avabuilds.packetrunner.network.Concepts
import com.avabuilds.packetrunner.network.all
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the editor is showing: a new note ([original] is null) or an existing one. */
private class Editor(val original: Note?, title: String, body: String, topic: String) {
    var title by mutableStateOf(title)
    var body by mutableStateOf(body)
    var topic by mutableStateOf(topic)
}

/**
 * The Engineer's notebook: a list of the player's notes with search, and an editor to create, change
 * and delete them. Everything is stored through [repo] for the player [uid].
 */
@Composable
fun NotebookPanel(uid: String, repo: NotebookRepository, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var notes by remember { mutableStateOf<List<Note>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<Editor?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        repo.list(uid) { result ->
            result.onSuccess { notes = it; loadError = false }.onFailure { loadError = true }
        }
    }

    fun save(e: Editor) {
        val problem = NoteValidation.title(e.title) ?: NoteValidation.body(e.body)
        if (problem != null) {
            message = problem
            return
        }
        val now = System.currentTimeMillis()
        val original = e.original
        if (original == null) {
            repo.add(uid, NoteDraft(e.title, e.body, e.topic), now) { added ->
                if (added != null) { notes = notes.orEmpty() + added; editor = null; message = "Note saved." } else message = "Could not save the note."
            }
        } else {
            val changed = original.copy(title = e.title.trim(), body = e.body, topic = e.topic, updatedAt = now)
            repo.update(uid, changed) { ok ->
                if (ok) { notes = notes.orEmpty().map { if (it.id == changed.id) changed else it }; editor = null; message = "Note updated." } else message = "Could not update the note."
            }
        }
    }

    fun delete(note: Note) {
        repo.delete(uid, note.id) { ok ->
            if (ok) { notes = notes.orEmpty().filter { it.id != note.id }; editor = null; message = "Note deleted." } else message = "Could not delete the note."
        }
    }

    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📓 Engineer's notebook", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clickable(onClick = onClose)) { Pill("Close", PR.Blue50, PR.Blue800) }
        }

        val e = editor
        if (e != null) {
            EditorForm(e, confirmDelete, onSave = { save(e) }, onCancel = { editor = null; confirmDelete = false; message = null }, onDelete = {
                val original = e.original ?: return@EditorForm
                if (confirmDelete) { confirmDelete = false; delete(original) } else { confirmDelete = true; message = "Tap Delete again to remove this note." }
            })
        } else {
            PrimaryButton("＋ New note") { editor = Editor(null, "", "", ""); message = null; confirmDelete = false }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search notes") }, singleLine = true)
            Column(Modifier.fillMaxWidth().height(260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val shown = NoteList.sorted(NoteList.filtered(notes.orEmpty(), query))
                when {
                    loadError -> Text("Could not load your notes. Check your internet connection.", fontSize = 13.sp, color = PR.Red800)
                    notes == null -> Text("Loading…", fontSize = 13.sp, color = PR.Muted)
                    notes.orEmpty().isEmpty() -> Text("No notes yet. Write down what you learn, or save it from the card after a job.", fontSize = 13.sp, color = PR.Muted)
                    shown.isEmpty() -> Text("No note matches “$query”.", fontSize = 13.sp, color = PR.Muted)
                    else -> shown.forEach { note -> NoteRow(note) { editor = Editor(note, note.title, note.body, note.topic); message = null; confirmDelete = false } }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(20.dp)) {
            message?.let { Text(it, fontSize = 12.sp, color = PR.Ink) }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(PR.Teal50, RoundedCornerShape(12.dp)).clickable(onClick = onOpen).padding(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(note.title, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PR.Ink, maxLines = 1)
            Text(SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(note.updatedAt)), fontSize = 11.sp, color = PR.Muted)
        }
        if (note.topic.isNotBlank()) Text(note.topic, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
        if (note.body.isNotBlank()) Text(note.body, fontSize = 12.sp, color = PR.Muted, maxLines = 2)
    }
}

@Composable
private fun EditorForm(e: Editor, confirmingDelete: Boolean, onSave: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).height(330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(e.title, { e.title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true,
            supportingText = { Text("${e.title.trim().length}/${NoteValidation.TITLE_MAX}") })
        Text("Topic (optional)", fontSize = 12.sp, color = PR.Muted)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Concepts.all.forEach { c ->
                val picked = e.topic == c.en
                Box(Modifier.clickable { e.topic = if (picked) "" else c.en }) {
                    Pill(c.en, if (picked) Palette5.PersianBlue else PR.Blue50, if (picked) PR.White else PR.Blue800)
                }
            }
        }
        OutlinedTextField(e.body, { e.body = it }, Modifier.fillMaxWidth(), label = { Text("Note") }, minLines = 3, maxLines = 5,
            supportingText = { Text("${e.body.length}/${NoteValidation.BODY_MAX}") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Cancel", Modifier.weight(1f), onCancel)
            if (e.original != null) SecondaryButton(if (confirmingDelete) "Sure?" else "Delete", Modifier.weight(1f), onDelete)
            PrimaryButton("Save", Modifier.weight(1f), onSave)
        }
    }
}
