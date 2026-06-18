package com.example.applistas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.applistas.data.local.entity.Address
import com.example.applistas.data.local.entity.Note
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.NoteRepository
import kotlinx.coroutines.launch

enum class PriorityFilter {
    ALL,
    HIGH,
    MEDIUM,
    LOW
}

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    // Converte o Flow do repositório em LiveData para observação na UI
    val allNotes: LiveData<List<Note>> = repository.allNotes.asLiveData()
    private val _filter = MutableLiveData(PriorityFilter.ALL)

    val filter: LiveData<PriorityFilter>
        get() = _filter

    fun setFilter(filter: PriorityFilter) {
        _filter.value = filter
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun saveNote(
        title: String,
        content: String,
        priority: NotePriority,
        address: Address?,
        currentNote: Note?
    ) = viewModelScope.launch {
        if (currentNote == null) {
            repository.insert(
                Note(
                    title = title,
                    content = content,
                    priority = priority,
                    address = address
                )
            )
        } else {
            repository.update(
                currentNote.copy(
                    title = title,
                    content = content,
                    priority = priority,
                    address = address,
                    isSynced = false
                )
            )
        }
    }
}
