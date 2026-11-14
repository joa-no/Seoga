package com.joano.seoga.ui.screens.form

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.R
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.WorkEntity
import com.joano.seoga.data.remote.MangaSearchResult
import com.joano.seoga.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkFormState(
    val workId: Long? = null,
    val title: String = "",
    val coverUri: String? = null,
    val authors: String = "",
    val artists: String = "",
    val type: String = "Mangá",
    val genres: List<String> = emptyList(),
    val newGenreInput: String = "",
    val chaptersRead: String = "0",
    val chaptersReadError: String? = null,
    val totalChapters: String = "",
    val initialTotalChapters: String = "",
    val totalSource: String = "MANUAL", // "MANUAL", "MANGADEX", "ANILIST", etc.
    val limitReleased: Boolean = false,
    val synopsis: String = "",
    val personalNotes: String = "",
    val rating: Float = 0f,
    val selectedCollectionIds: Set<Long> = emptySet(),
    val inAllCollection: Boolean = true,
    val isAdult: Boolean = false,
    val validationToast: String? = null,
    val externalId: String? = null,
    val externalSource: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showChangeTotalToManualDialog: Boolean = false,
    val pendingNewTotalValue: String? = null,
    val showReleaseLimitDialogOnSave: Boolean = false
) {
    val isLimitActive: Boolean
        get() = totalChapters.toIntOrNull() != null && (totalSource == "MANUAL" || !limitReleased)

    val displaySource: String
        get() = when (totalSource.uppercase()) {
            "MANGADEX" -> "MangaDex"
            "ANILIST" -> "AniList"
            else -> externalSource ?: totalSource
        }
}

class WorkFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LibraryRepository.getInstance(application)
    private val preferencesRepo = com.joano.seoga.data.preferences.UserPreferencesRepository(application)

    private val _formState = MutableStateFlow(WorkFormState())
    val formState: StateFlow<WorkFormState> = _formState.asStateFlow()

    val availableCollections: StateFlow<List<CollectionEntity>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<com.joano.seoga.data.preferences.UserSettings> = preferencesRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.joano.seoga.data.preferences.UserSettings())

    fun initializeForNew() {
        _formState.value = WorkFormState(inAllCollection = true, isAdult = false)
    }

    fun initializeFromExternal(result: MangaSearchResult) {
        val hasTotal = result.totalChapters != null
        val src = result.source.ifBlank { "MangaDex" }
        _formState.value = WorkFormState(
            title = result.title,
            coverUri = result.coverUrl,
            authors = result.authors.joinToString(", "),
            artists = result.artists.joinToString(", "),
            type = result.type,
            genres = result.genres,
            totalChapters = result.totalChapters?.toString() ?: "",
            initialTotalChapters = result.totalChapters?.toString() ?: "",
            totalSource = if (hasTotal) src.uppercase() else "MANUAL",
            limitReleased = false,
            synopsis = result.description,
            externalId = result.id,
            externalSource = src,
            inAllCollection = true,
            isAdult = result.isAdult
        )
    }

    fun initializeFromMangaDex(result: MangaSearchResult) {
        initializeFromExternal(result)
    }

    fun initializeForEdit(workId: Long) {
        viewModelScope.launch {
            val workWithCols = repository.getWorkWithCollections(workId).firstOrNull() ?: return@launch
            val work = workWithCols.work
            val genreList = if (work.genres.isNotBlank()) {
                work.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            _formState.value = WorkFormState(
                workId = work.id,
                title = work.title,
                coverUri = work.coverUri,
                authors = work.authors,
                artists = work.artists,
                type = work.type,
                genres = genreList,
                chaptersRead = work.chaptersRead.toString(),
                totalChapters = work.totalChapters?.toString() ?: "",
                initialTotalChapters = work.totalChapters?.toString() ?: "",
                totalSource = work.totalSource,
                limitReleased = work.limitReleased,
                synopsis = work.synopsis,
                personalNotes = work.personalNotes,
                rating = work.rating,
                selectedCollectionIds = workWithCols.collections.map { it.id }.toSet(),
                inAllCollection = !work.hiddenFromAll || workWithCols.collections.isEmpty(),
                isAdult = work.isAdult,
                externalId = work.externalId,
                externalSource = work.externalSource
            )
        }
    }

    fun onIsAdultChange(value: Boolean) {
        _formState.value = _formState.value.copy(isAdult = value)
    }

    fun onTitleChange(value: String) {
        _formState.value = _formState.value.copy(title = value, errorMessage = null)
    }

    fun onAuthorsChange(value: String) {
        _formState.value = _formState.value.copy(authors = value)
    }

    fun onArtistsChange(value: String) {
        _formState.value = _formState.value.copy(artists = value)
    }

    fun onTypeChange(value: String) {
        _formState.value = _formState.value.copy(type = value)
    }

    fun onNewGenreInputChange(value: String) {
        _formState.value = _formState.value.copy(newGenreInput = value)
    }

    fun addGenre() {
        val input = _formState.value.newGenreInput.trim()
        if (input.isNotEmpty() && !_formState.value.genres.contains(input)) {
            _formState.value = _formState.value.copy(
                genres = _formState.value.genres + input,
                newGenreInput = ""
            )
        }
    }

    fun removeGenre(genre: String) {
        _formState.value = _formState.value.copy(
            genres = _formState.value.genres - genre
        )
    }

    fun onChaptersReadChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _formState.value = _formState.value.copy(chaptersRead = value, chaptersReadError = null)
        }
    }

    fun onTotalChaptersChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            val state = _formState.value
            // If total was from external source and user modifies it, confirm changing to MANUAL
            if (state.totalSource != "MANUAL" && value != state.initialTotalChapters) {
                _formState.value = state.copy(
                    showChangeTotalToManualDialog = true,
                    pendingNewTotalValue = value,
                    chaptersReadError = null
                )
            } else {
                _formState.value = state.copy(totalChapters = value, chaptersReadError = null)
            }
        }
    }

    fun confirmChangeTotalToManual() {
        val pending = _formState.value.pendingNewTotalValue ?: _formState.value.totalChapters
        _formState.value = _formState.value.copy(
            totalChapters = pending,
            totalSource = "MANUAL",
            limitReleased = false,
            showChangeTotalToManualDialog = false,
            pendingNewTotalValue = null
        )
    }

    fun dismissChangeTotalToManual() {
        _formState.value = _formState.value.copy(
            showChangeTotalToManualDialog = false,
            pendingNewTotalValue = null
        )
    }

    fun toggleLimitReleased(released: Boolean) {
        _formState.value = _formState.value.copy(limitReleased = released)
    }

    fun onSynopsisChange(value: String) {
        _formState.value = _formState.value.copy(synopsis = value)
    }

    fun onPersonalNotesChange(value: String) {
        _formState.value = _formState.value.copy(personalNotes = value)
    }

    fun onRatingChange(value: Float) {
        _formState.value = _formState.value.copy(rating = value)
    }

    fun toggleCollection(collectionId: Long) {
        val current = _formState.value.selectedCollectionIds.toMutableSet()
        if (current.contains(collectionId)) {
            current.remove(collectionId)
        } else {
            current.add(collectionId)
        }
        val newInAll = if (current.isEmpty()) true else _formState.value.inAllCollection
        _formState.value = _formState.value.copy(
            selectedCollectionIds = current,
            inAllCollection = newInAll
        )
    }

    fun toggleAllCollection(warningMessage: String) {
        val state = _formState.value
        if (state.inAllCollection) {
            // Cannot uncheck "Todos" if no other collection is selected
            if (state.selectedCollectionIds.isEmpty()) {
                _formState.value = state.copy(validationToast = warningMessage)
                return
            }
            _formState.value = state.copy(inAllCollection = false, validationToast = null)
        } else {
            _formState.value = state.copy(inAllCollection = true, validationToast = null)
        }
    }

    fun clearValidationToast() {
        _formState.value = _formState.value.copy(validationToast = null)
    }

    fun onCoverPicked(uri: Uri) {
        viewModelScope.launch {
            val localPath = repository.persistCoverFromUri(uri)
            if (localPath != null) {
                _formState.value = _formState.value.copy(coverUri = localPath)
            }
        }
    }

    fun removeCover() {
        _formState.value = _formState.value.copy(coverUri = null)
    }

    fun saveWork(onSuccess: (Long) -> Unit) {
        val state = _formState.value
        if (state.title.trim().isEmpty()) {
            _formState.value = state.copy(errorMessage = "REQUIRED")
            return
        }

        val readCount = state.chaptersRead.toIntOrNull() ?: 0
        val totalCount = state.totalChapters.toIntOrNull()

        // Check if chaptersRead exceeds total
        if (totalCount != null && readCount > totalCount) {
            if (state.totalSource == "MANUAL") {
                _formState.value = state.copy(
                    chaptersReadError = getApplication<Application>().getString(
                        R.string.read_exceeds_total_error,
                        totalCount
                    )
                )
                return
            } else if (!state.limitReleased) {
                _formState.value = state.copy(showReleaseLimitDialogOnSave = true)
                return
            }
        }

        executeSave(onSuccess)
    }

    fun confirmReleaseLimitOnSave(onSuccess: (Long) -> Unit) {
        _formState.value = _formState.value.copy(
            limitReleased = true,
            showReleaseLimitDialogOnSave = false
        )
        executeSave(onSuccess)
    }

    fun dismissReleaseLimitOnSave() {
        _formState.value = _formState.value.copy(showReleaseLimitDialogOnSave = false)
    }

    private fun executeSave(onSuccess: (Long) -> Unit) {
        val state = _formState.value
        _formState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val finalHidden = if (state.selectedCollectionIds.isEmpty()) false else !state.inAllCollection
                val entity = WorkEntity(
                    id = state.workId ?: 0,
                    title = state.title.trim(),
                    coverUri = state.coverUri,
                    authors = state.authors.trim(),
                    artists = state.artists.trim(),
                    type = state.type,
                    genres = state.genres.joinToString(", "),
                    chaptersRead = state.chaptersRead.toIntOrNull() ?: 0,
                    totalChapters = state.totalChapters.toIntOrNull(),
                    synopsis = state.synopsis.trim(),
                    personalNotes = state.personalNotes.trim(),
                    rating = state.rating,
                    externalId = state.externalId,
                    externalSource = state.externalSource,
                    totalSource = state.totalSource,
                    limitReleased = state.limitReleased,
                    hiddenFromAll = finalHidden,
                    isAdult = state.isAdult,
                    updatedAt = System.currentTimeMillis()
                )

                val savedId = repository.saveWork(entity, state.selectedCollectionIds.toList())
                _formState.value = _formState.value.copy(isSaving = false)
                onSuccess(savedId)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage ?: "Error saving work"
                )
            }
        }
    }
}
