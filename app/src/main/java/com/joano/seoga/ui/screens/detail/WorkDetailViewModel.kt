package com.joano.seoga.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.WorkWithCollections
import com.joano.seoga.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkDetailViewModel(
    application: Application,
    private val workId: Long
) : AndroidViewModel(application) {

    private val repository = LibraryRepository.getInstance(application)
    private val preferencesRepo = com.joano.seoga.data.preferences.UserPreferencesRepository(application)

    val workWithCollections: StateFlow<WorkWithCollections?> = repository.getWorkWithCollections(workId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCollections: StateFlow<List<CollectionEntity>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<com.joano.seoga.data.preferences.UserSettings> = preferencesRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.joano.seoga.data.preferences.UserSettings())

    private val _pendingChapterOverLimit = MutableStateFlow<Int?>(null)
    val pendingChapterOverLimit: StateFlow<Int?> = _pendingChapterOverLimit.asStateFlow()

    fun incrementChapter() {
        val current = workWithCollections.value?.work ?: return
        val max = current.totalChapters
        val next = current.chaptersRead + 1
        if (current.isLimitActive && max != null && next > max) {
            // Reached MangaDex limit
            return
        }
        viewModelScope.launch {
            repository.updateProgress(current.id, next)
        }
    }

    fun decrementChapter() {
        val current = workWithCollections.value?.work ?: return
        if (current.chaptersRead > 0) {
            viewModelScope.launch {
                repository.updateProgress(current.id, current.chaptersRead - 1)
            }
        }
    }

    fun setChapter(chapter: Int) {
        val current = workWithCollections.value?.work ?: return
        if (chapter < 0) return
        val max = current.totalChapters
        if (current.isLimitActive && max != null && chapter > max) {
            if (current.totalSource != "MANUAL") {
                _pendingChapterOverLimit.value = chapter
            }
            return
        }
        viewModelScope.launch {
            repository.updateProgress(current.id, chapter)
        }
    }

    fun confirmReleaseLimitAndApply() {
        val chapter = _pendingChapterOverLimit.value
        _pendingChapterOverLimit.value = null
        val current = workWithCollections.value?.work ?: return
        viewModelScope.launch {
            repository.releaseLimit(current.id)
            if (chapter != null) {
                repository.updateProgress(current.id, chapter)
            }
        }
    }

    fun dismissReleaseLimitDialog() {
        _pendingChapterOverLimit.value = null
    }

    fun setRating(rating: Float) {
        viewModelScope.launch {
            repository.updateRating(workId, rating)
        }
    }

    fun updateCollections(selectedCollectionIds: List<Long>, inAll: Boolean = true) {
        val finalHidden = if (selectedCollectionIds.isEmpty()) false else !inAll
        viewModelScope.launch {
            repository.setWorkCollections(workId, selectedCollectionIds, hiddenFromAll = finalHidden)
        }
    }

    fun deleteWork(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWork(workId)
            onDeleted()
        }
    }
}
