package com.joano.seoga.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.R
import com.joano.seoga.data.preferences.UserPreferencesRepository
import com.joano.seoga.data.preferences.UserSettings
import com.joano.seoga.data.remote.AniListRateLimitException
import com.joano.seoga.data.remote.AniListSource
import com.joano.seoga.data.remote.MangaDexSource
import com.joano.seoga.data.remote.MangaSearchResult
import com.joano.seoga.ui.util.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val results: List<MangaSearchResult>,
        val partialWarning: UiText? = null
    ) : SearchUiState()
    data class Error(val message: UiText) : SearchUiState()
}

class SearchMangaDexViewModel(application: Application) : AndroidViewModel(application) {

    private val mangaDexSource = MangaDexSource()
    private val aniListSource = AniListSource()
    private val preferencesRepository = UserPreferencesRepository(application)

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // Holds the selected result for passing to form review
    var selectedResultForReview: MangaSearchResult? = null

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.trim().length < 2) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            executeSearch(newQuery.trim())
        }
    }

    fun executeSearch(query: String = _searchQuery.value.trim()) {
        if (query.isEmpty()) return
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            val settings = userSettings.value
            val useMangaDex = settings.useMangaDexSearch
            val useAniList = settings.useAniListSearch
            val includeAdult = settings.includeAdultContent
            val effectiveLanguage = com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag(settings.language)

            if (!useMangaDex && !useAniList) {
                _uiState.value = SearchUiState.Idle
                return@launch
            }

            // Execute active sources in parallel
            val deferredMangaDex = if (useMangaDex) {
                async { mangaDexSource.search(query, includeAdult, effectiveLanguage) }
            } else null

            val deferredAniList = if (useAniList) {
                async { aniListSource.search(query, includeAdult, effectiveLanguage) }
            } else null

            val mdResult = deferredMangaDex?.await()
            val alResult = deferredAniList?.await()

            val combined = mutableListOf<MangaSearchResult>()
            val failedSources = mutableListOf<String>()

            mdResult?.fold(
                onSuccess = { list -> combined.addAll(list) },
                onFailure = { failedSources.add("MangaDex") }
            )

            alResult?.fold(
                onSuccess = { list -> combined.addAll(list) },
                onFailure = { error ->
                    if (error is AniListRateLimitException) {
                        failedSources.add("AniList (429: limite de requisições)")
                    } else {
                        failedSources.add("AniList")
                    }
                }
            )

            val activeSourceCount = (if (useMangaDex) 1 else 0) + (if (useAniList) 1 else 0)

            if (combined.isNotEmpty()) {
                val warning = if (failedSources.isNotEmpty()) {
                    UiText.StringResource(R.string.search_partial_failure, failedSources.joinToString(", "))
                } else null
                _uiState.value = SearchUiState.Success(combined, warning)
            } else if (failedSources.size == activeSourceCount && failedSources.isNotEmpty()) {
                _uiState.value = SearchUiState.Error(
                    UiText.StringResource(R.string.search_all_failed, failedSources.joinToString(", "))
                )
            } else {
                _uiState.value = SearchUiState.Success(emptyList())
            }
        }
    }

    fun enableSources() {
        viewModelScope.launch {
            preferencesRepository.setUseMangaDexSearch(true)
            preferencesRepository.setUseAniListSearch(true)
        }
    }
}
