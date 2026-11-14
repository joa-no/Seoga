package com.joano.seoga.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val coverUri: String? = null,
    val authors: String = "",       // Comma-separated or single
    val artists: String = "",       // Comma-separated or single
    val type: String = "Mangá",     // "Mangá", "Manhwa", "Manhua", "HQ", or custom
    val genres: String = "",        // Comma-separated tags
    val chaptersRead: Int = 0,
    val totalChapters: Int? = null,
    val synopsis: String = "",
    val personalNotes: String = "",
    val rating: Float = 0f,         // 0f to 10f or 0f to 5f
    val externalId: String? = null, // e.g., MangaDex UUID
    val externalSource: String? = null, // "MangaDex"
    val totalSource: String = "MANUAL", // "MANUAL" or "MANGADEX"
    val limitReleased: Boolean = false,
    val hiddenFromAll: Boolean = false,
    val isAdult: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLimitActive: Boolean
        get() = totalChapters != null && (totalSource == "MANUAL" || !limitReleased)

    val displaySource: String
        get() = when (totalSource.uppercase()) {
            "MANGADEX" -> "MangaDex"
            "ANILIST" -> "AniList"
            else -> totalSource
        }
}
