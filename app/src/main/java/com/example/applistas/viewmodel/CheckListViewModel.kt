package com.example.applistas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.applistas.data.local.entity.Checklist
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.ChecklistRepository
import kotlinx.coroutines.launch

class CheckListViewModel(private val repository: ChecklistRepository) : ViewModel() {

    // Converte o Flow do repositório em LiveData para observação na UI
    val allChecklists: LiveData<List<Checklist>> = repository.allChecklists.asLiveData()

    fun insert(checklist: Checklist) = viewModelScope.launch {
        repository.insert(checklist)
    }

    fun delete(checklist: Checklist) = viewModelScope.launch {
        repository.delete(checklist)
    }

    fun update(checklist: Checklist) = viewModelScope.launch {
        repository.update(checklist)
    }

    fun toggleCompleted(checklist: Checklist) = viewModelScope.launch {
        repository.update(checklist.copy(isCompleted = !checklist.isCompleted))
    }

    fun saveChecklist(title: String, priority: NotePriority, currentChecklist: Checklist?) =
        viewModelScope.launch {
            if (currentChecklist == null) {
                repository.insert(
                    Checklist(
                        title = title,
                        priority = priority
                    )
                )
            } else {
                repository.update(
                    currentChecklist.copy(
                        title = title,
                        priority = priority
                    )
                )
            }
        }
}
