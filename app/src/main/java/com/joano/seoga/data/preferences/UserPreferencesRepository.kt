package com.joano.seoga.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val useMangaDexSearch: Boolean = true,
    val useAniListSearch: Boolean = true,
    val includeAdultContent: Boolean = false,
    val themeMode: String = "dark", // "dark", "light", "system"
    val viewMode: String = "grid",   // "grid", "list"
    val language: String = "auto",   // "auto", "pt-BR", "es", "en"
    val allCollectionName: String = "" // Custom name for "Todos"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USE_MANGADEX = booleanPreferencesKey("use_mangadex_search")
        val USE_ANILIST = booleanPreferencesKey("use_anilist_search")
        val INCLUDE_ADULT = booleanPreferencesKey("include_adult_content")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ALL_COLLECTION_NAME = stringPreferencesKey("all_collection_name")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val useMangaDex = preferences[PreferencesKeys.USE_MANGADEX] ?: true
        val useAniList = preferences[PreferencesKeys.USE_ANILIST] ?: true
        val includeAdult = preferences[PreferencesKeys.INCLUDE_ADULT] ?: false
        val themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "dark"
        val viewMode = preferences[PreferencesKeys.VIEW_MODE] ?: "grid"
        val language = preferences[PreferencesKeys.LANGUAGE] ?: "auto"
        val allCollectionName = preferences[PreferencesKeys.ALL_COLLECTION_NAME] ?: ""
        UserSettings(
            useMangaDexSearch = useMangaDex,
            useAniListSearch = useAniList,
            includeAdultContent = includeAdult,
            themeMode = themeMode,
            viewMode = viewMode,
            language = language,
            allCollectionName = allCollectionName
        )
    }

    suspend fun setUseMangaDexSearch(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MANGADEX] = enabled
        }
    }

    suspend fun setUseAniListSearch(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_ANILIST] = enabled
        }
    }

    suspend fun setIncludeAdultContent(include: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INCLUDE_ADULT] = include
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIEW_MODE] = mode
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun setAllCollectionName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALL_COLLECTION_NAME] = name.trim()
        }
    }
}
