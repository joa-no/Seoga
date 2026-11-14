package com.joano.seoga.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.CollectionWithCount
import com.joano.seoga.data.local.WorkEntity
import com.joano.seoga.data.local.WorkWithCollections
import com.joano.seoga.data.preferences.UserPreferencesRepository
import com.joano.seoga.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.annotation.StringRes
import com.joano.seoga.R

enum class SortOption(@StringRes val resId: Int, val label: String) {
    RECENT_UPDATED(R.string.sort_recent, "Última Atualização"),
    RECENT_ADDED(R.string.sort_recent, "Data de Adição"),
    TITLE_ASC(R.string.sort_title_asc, "Título (A-Z)"),
    TITLE_DESC(R.string.sort_title_asc, "Título (Z-A)"),
    PROGRESS(R.string.sort_progress, "Progresso de Leitura")
}

data class FilterState(
    val searchQuery: String = "",
    val selectedCollectionId: Long? = null, // null means "Todos"
    val selectedType: String? = null,        // null means All types
    val progressFilter: String = "ALL",      // "ALL", "WITH_PROGRESS", "NO_PROGRESS"
    val sortOption: SortOption = SortOption.RECENT_UPDATED,
    val viewMode: String = "grid"            // "grid", "list"
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository.getInstance(application)
    private val preferencesRepo = UserPreferencesRepository(application)

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _selectedWorkIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedWorkIds: StateFlow<Set<Long>> = _selectedWorkIds.asStateFlow()

    val collectionsWithCounts: StateFlow<List<CollectionWithCount>> = repository.collectionsWithCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCollections: StateFlow<List<CollectionEntity>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWorksCount: StateFlow<Int> = repository.totalWorksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userSettings: StateFlow<com.joano.seoga.data.preferences.UserSettings> = preferencesRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.joano.seoga.data.preferences.UserSettings())

    val allWorksWithCollections: StateFlow<List<WorkWithCollections>> = repository.allWorksWithCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCollectionWorksCount: StateFlow<Int> = repository.allWorksWithCollections.combine(preferencesRepo.userSettingsFlow) { works, _ ->
        works.count { !it.work.hiddenFromAll || it.collections.isEmpty() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val displayedWorks: StateFlow<List<WorkWithCollections>> = combine(
        repository.allWorksWithCollections,
        _filterState,
        preferencesRepo.userSettingsFlow
    ) { allWorks, filter, settings ->
        filterAndSort(allWorks, filter.selectedCollectionId, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getWorksForCollectionPage(collectionId: Long?): List<WorkWithCollections> {
        val all = allWorksWithCollections.value
        val filter = _filterState.value
        return filterAndSort(all, collectionId, filter)
    }

    private fun filterAndSort(
        allWorks: List<WorkWithCollections>,
        collectionId: Long?,
        filter: FilterState
    ): List<WorkWithCollections> {
        var list = allWorks

        // Filter by collection
        if (collectionId != null) {
            list = list.filter { workWithCols ->
                workWithCols.collections.any { it.id == collectionId }
            }
        } else {
            // "Todos" rule: visible if !hiddenFromAll OR collections.isEmpty()
            list = list.filter { workWithCols ->
                !workWithCols.work.hiddenFromAll || workWithCols.collections.isEmpty()
            }
        }

        // Filter by search query (title, author, artist, genres)
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            list = list.filter {
                it.work.title.lowercase().contains(q) ||
                it.work.authors.lowercase().contains(q) ||
                it.work.artists.lowercase().contains(q) ||
                it.work.genres.lowercase().contains(q)
            }
        }

        // Filter by type
        if (filter.selectedType != null) {
            list = list.filter { it.work.type.equals(filter.selectedType, ignoreCase = true) }
        }

        // Filter by progress status
        when (filter.progressFilter) {
            "WITH_PROGRESS" -> list = list.filter { it.work.chaptersRead > 0 }
            "NO_PROGRESS" -> list = list.filter { it.work.chaptersRead == 0 }
        }

        // Sort
        return when (filter.sortOption) {
            SortOption.RECENT_UPDATED -> list.sortedByDescending { it.work.updatedAt }
            SortOption.RECENT_ADDED -> list.sortedByDescending { it.work.createdAt }
            SortOption.TITLE_ASC -> list.sortedBy { it.work.title.lowercase() }
            SortOption.TITLE_DESC -> list.sortedByDescending { it.work.title.lowercase() }
            SortOption.PROGRESS -> list.sortedByDescending {
                if (it.work.totalChapters != null && it.work.totalChapters > 0) {
                    it.work.chaptersRead.toFloat() / it.work.totalChapters.toFloat()
                } else {
                    it.work.chaptersRead.toFloat()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectCollection(collectionId: Long?) {
        _filterState.value = _filterState.value.copy(selectedCollectionId = collectionId)
    }

    fun setSortOption(sort: SortOption) {
        _filterState.value = _filterState.value.copy(sortOption = sort)
    }

    fun setTypeFilter(type: String?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun setProgressFilter(progressFilter: String) {
        _filterState.value = _filterState.value.copy(progressFilter = progressFilter)
    }

    fun toggleViewMode() {
        val current = _filterState.value.viewMode
        val newMode = if (current == "grid") "list" else "grid"
        _filterState.value = _filterState.value.copy(viewMode = newMode)
        viewModelScope.launch {
            preferencesRepo.setViewMode(newMode)
        }
    }

    fun incrementChapter(work: WorkEntity) {
        if (work.isLimitActive && work.chaptersRead >= (work.totalChapters ?: 0)) {
            // Limited at total
            return
        }
        viewModelScope.launch {
            val max = work.totalChapters
            val next = work.chaptersRead + 1
            if (max == null || !work.isLimitActive || next <= max) {
                repository.updateProgress(work.id, next)
            }
        }
    }

    fun decrementChapter(work: WorkEntity) {
        viewModelScope.launch {
            if (work.chaptersRead > 0) {
                repository.updateProgress(work.id, work.chaptersRead - 1)
            }
        }
    }

    fun deleteWork(workId: Long) {
        viewModelScope.launch {
            repository.deleteWork(workId)
        }
    }

    fun updateWorkCollections(workId: Long, collectionIds: List<Long>) {
        viewModelScope.launch {
            repository.setWorkCollections(workId, collectionIds)
        }
    }

    fun toggleWorkSelection(workId: Long) {
        val current = _selectedWorkIds.value
        _selectedWorkIds.value = if (current.contains(workId)) {
            current - workId
        } else {
            current + workId
        }
    }

    fun selectAll(workIds: List<Long>) {
        _selectedWorkIds.value = workIds.toSet()
    }

    fun clearSelection() {
        _selectedWorkIds.value = emptySet()
    }

    fun applyManageCollectionsChanges(
        userChanges: Map<Long, Boolean>,
        allCollections: List<com.joano.seoga.data.local.CollectionEntity>,
        onComplete: () -> Unit
    ) {
        val ids = _selectedWorkIds.value.toList()
        if (ids.isEmpty()) {
            onComplete()
            return
        }

        val collectionsToAdd = allCollections.map { it.id }.filter { userChanges[it] == true }
        val collectionsToRemove = allCollections.map { it.id }.filter { userChanges[it] == false }
        val hiddenFromAllChange = when (userChanges[com.joano.seoga.ui.components.ALL_COLLECTION_PSEUDO_ID]) {
            true -> false
            false -> true
            null -> null
        }

        viewModelScope.launch {
            repository.applyBatchCollectionChanges(
                workIds = ids,
                collectionsToAdd = collectionsToAdd,
                collectionsToRemove = collectionsToRemove,
                hiddenFromAllChange = hiddenFromAllChange
            )
            _selectedWorkIds.value = emptySet()
            onComplete()
        }
    }

    fun deleteSelectedWorks(onComplete: () -> Unit = {}) {
        val ids = _selectedWorkIds.value.toList()
        if (ids.isEmpty()) {
            onComplete()
            return
        }
        viewModelScope.launch {
            repository.deleteWorks(ids)
            _selectedWorkIds.value = emptySet()
            onComplete()
        }
    }
}
