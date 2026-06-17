package com.example.applistas.data.repository

import com.example.applistas.data.local.dao.ChecklistDao
import com.example.applistas.data.local.entity.Checklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChecklistRepository(private val checklistDao: ChecklistDao) {

    val allChecklists: Flow<List<Checklist>> = checklistDao.getAllChecklists()

    suspend fun insert(checklist: Checklist): Int {
        return withContext(Dispatchers.IO) {
            checklistDao.insertChecklist(checklist).toInt()
        }
    }

    suspend fun update(checklist: Checklist) {
        withContext(Dispatchers.IO) {
            checklistDao.updateItem(checklist)
        }
    }

    suspend fun delete(checklist: Checklist) {
        withContext(Dispatchers.IO) {
            checklistDao.deleteChecklist(checklist)
        }
    }
}
