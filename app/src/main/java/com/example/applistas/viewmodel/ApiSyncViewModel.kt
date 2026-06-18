package com.example.applistas.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applistas.data.repository.SyncRepository
import kotlinx.coroutines.launch

enum class SyncStatus {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

class ApiSyncViewModel(private val repository: SyncRepository) : ViewModel() {

    private val _syncStatus = MutableLiveData(SyncStatus.IDLE)
    val syncStatus: LiveData<SyncStatus>
        get() = _syncStatus

    fun sync(): Boolean {
        if (_syncStatus.value == SyncStatus.LOADING) return false

        _syncStatus.value = SyncStatus.LOADING
        viewModelScope.launch {
            try {
                repository.syncUnsyncedItems()
                _syncStatus.value = SyncStatus.SUCCESS
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
        return true
    }
}
