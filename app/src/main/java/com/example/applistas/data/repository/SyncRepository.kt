package com.example.applistas.data.repository

import com.example.applistas.data.local.dao.ChecklistDao
import com.example.applistas.data.local.dao.NoteDao
import com.example.applistas.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(
    private val noteDao: NoteDao,
    private val checklistDao: ChecklistDao,
    private val apiService: ApiService
) {

    suspend fun syncUnsyncedItems() {
        withContext(Dispatchers.IO) {
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            val unsyncedChecklists = checklistDao.getUnsyncedChecklists()

            if (unsyncedNotes.isNotEmpty()) {
                val response = apiService.syncNotes(unsyncedNotes)
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao sincronizar notas")
                }

                unsyncedNotes.forEach { note ->
                    noteDao.updateNote(note.copy(isSynced = true))
                }
            }

            if (unsyncedChecklists.isNotEmpty()) {
                val response = apiService.syncChecklist(unsyncedChecklists)
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao sincronizar checklists")
                }

                unsyncedChecklists.forEach { checklist ->
                    checklistDao.updateItem(checklist.copy(isSynced = true))
                }
            }
        }
    }
}
