package com.joano.seoga.ui.screens.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.CollectionWithCount
import com.joano.seoga.data.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository.getInstance(application)
    private val preferencesRepo = com.joano.seoga.data.preferences.UserPreferencesRepository(application)

    val collectionsWithCounts: StateFlow<List<CollectionWithCount>> = repository.collectionsWithCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<com.joano.seoga.data.preferences.UserSettings> = preferencesRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.joano.seoga.data.preferences.UserSettings())

    val allCollectionWorksCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(
        repository.allWorksWithCollections,
        preferencesRepo.userSettingsFlow
    ) { works, _ ->
        works.count { !it.work.hiddenFromAll || it.collections.isEmpty() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun renameAllCollection(newName: String) {
        viewModelScope.launch {
            preferencesRepo.setAllCollectionName(newName.trim())
        }
    }

    fun createCollection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                repository.createCollection(trimmed)
            }
        }
    }

    fun renameCollection(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                repository.renameCollection(id, trimmed)
            }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            repository.deleteCollection(id)
        }
    }

    fun moveCollection(fromIndex: Int, toIndex: Int) {
        val currentList = collectionsWithCounts.value.map {
            CollectionEntity(
                id = it.id,
                name = it.name,
                createdAt = it.createdAt,
                orderPosition = it.orderPosition
            )
        }.toMutableList()

        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            viewModelScope.launch {
                repository.reorderCollections(currentList)
            }
        }
    }
}
